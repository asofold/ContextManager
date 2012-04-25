package me.asofold.bukkit.contextmanager.hooks.chestshop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bukkit.contextmanager.hooks.ServiceHook;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import asofold.pluginlib.shared.Blocks;
import asofold.pluginlib.shared.Utils;
import asofold.pluginlib.shared.blocks.FBlockPos;
import asofold.pluginlib.shared.items.ItemSpec;

import com.Acrobot.ChestShop.Utils.uBlock;
import com.Acrobot.ChestShop.Utils.uSign;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;


public class ChestShopHook implements Listener, ServiceHook{
	
	private final static String[] labels = new String[]{"shops", "shop"};
	
	// TODO: filter by another region, if desired.
	
	Map<FBlockPos, ShopSpec> blockMap = new HashMap<FBlockPos, ShopSpec>();
	
	/**
	 * For checking ...
	 */
	Map<String, Map<String, RegionSpec>> regionMap = new HashMap<String, Map<String,RegionSpec>>();
	
	/**
	 * Mapping raw id to items.
	 */
	Map<Integer, Set<RegionSpec>> idMap = new HashMap<Integer, Set<RegionSpec>>();
	
	/**
	 * Only if one of these regions matches, regions will be added.
	 */
	Map<String, Set<String>> filter = new HashMap<String, Set<String>>();
	
	// TODO: sort in to filter ! -> available item types.
	
	/**
	 * if false, only regions owned by the shop owner will get considered, but one region must match filter.
	 */
	boolean addUnowned = false;
	
	boolean useFilter = true;
	
	public ChestShopHook(){
		addFilter("mainworld", "maintown"); // TODO: HARD CODED EXAMPLE _ REMOVE / MAKE CONFIGURABLE
	}
	
	public void addFilter(String world, String region){
		String lcWorld = world.toLowerCase();
		String lcRid = region.toLowerCase();
		Set<String> rs = filter.get(lcWorld);
		if (rs == null){
			rs = new HashSet<String>();
			filter.put(lcWorld, rs);
		}
		rs.add(lcRid);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	final void onPlayerInteract(final PlayerInteractEvent event){
//		if (event.isCancelled()) return;
		Action action = event.getAction();
		if ( action!= Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return; // [mind leftclick sell]
		final Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.WALL_SIGN) return;
		// Check wall signs for ChestShop syntax:
		final Sign sign = (Sign) block.getState();
		final String[] lines = sign.getLines();
		
		// TODO: split here if desied to add signChange event ? [would change concept though]
		
		if (!uSign.isValid(lines)) return;
		System.out.println("CS clicked");
		final Player player = event.getPlayer();
		final String playerName = player.getName();
		// TODO: maybe better heck or leave it out:
		final String seller = lines[0].trim();
		if (playerName.equalsIgnoreCase(seller)) return; // ignore the shop owner.
		final String priceSpec = lines[2];
		Chest chest = uBlock.findChest(sign);
		if (chest == null){
			update(block, null, null, 0, -1.0, -1.0);
			return;
		}
		double priceBuy = uSign.buyPrice(priceSpec);
		double priceSell= uSign.sellPrice(priceSpec);
		int amount = uSign.itemAmount(lines[1]);
		ItemStack stack = com.Acrobot.ChestShop.Items.Items.getItemStack(lines[3]);
		if(stack == null){
			update(block, null, null, 0, -1.0, -1.0);
			return;
		}
		if (priceBuy >= 0){
			// TODO: check if out of stock.
		}
		
		if (priceSell >= 0){
			// TODO: Check if chest has space.
		}
		
		update(block, seller, stack, amount, priceBuy, priceSell);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	final void onBlockBreak(BlockBreakEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		if (block.getType() != Material.SIGN) return;
		// TODO: maybe this is fastest, maybe not.
		update(block, null, null, 0, -1.0, -1.0);
	}
	
	public final void update(final Block block, String seller, final ItemStack stack, final int amount, final double priceBuy, final double priceSell) {
		final FBlockPos pos = Blocks.FBlockPos(block);
		final ShopSpec spec = blockMap.get(pos);
		if (spec == null){
			if (priceBuy > 0 || priceSell > 0){
				checkAddShopSpec(pos, block, seller, stack, amount, priceBuy, priceSell);
			}
		}
		else{
			// TODO: update might be more specialized later on.
			spec.update(stack, amount, priceBuy, priceSell);
			if (spec.priceBuy < 0 && spec.priceSell < 0){
				// Do remove this shop.
				removeShopSpec(pos, spec);
			}
		}	
	}

	private void removeShopSpec(FBlockPos pos, ShopSpec spec) {
		blockMap.remove(pos);
		for (RegionSpec rSpec : spec.regions){
			rSpec.shops.remove(spec);
			if (rSpec.shops.isEmpty()){
				removeRegionSpec(rSpec);
				final int id = spec.stack.getTypeId();
				Set<RegionSpec> rs = idMap.get(id);
				if (rs!=null){
					rs.remove(rSpec);
					if (rs.isEmpty()) idMap.remove(id);
				}
			}
			else{
				// TODO: might still have to remove the id mapping !
			}
		}
		// "cleanup":
		spec.regions.clear();
		spec.stack = null;
	}
	
	/**
	 * This does NOT remove all the shops or id mappings!
	 * @param rSpec
	 */
	private void removeRegionSpec(RegionSpec rSpec) {
		Map<String, RegionSpec> rMap = regionMap.get(rSpec.worldName);
		if (rMap != null){
			// should always be the case...
			rMap.remove(rSpec.regionName);
			if (rMap.isEmpty()) regionMap.remove(rSpec.worldName);
		}
		
	}

	/**
	 * This is to create a new ShjopSpec for the position, or deny creation.
	 * @param pos
	 * @param block
	 * @param seller
	 * @param stack
	 * @param amount
	 * @param priceBuy
	 * @param priceSell
	 * @return
	 */
	public final boolean checkAddShopSpec(FBlockPos pos, final Block block, final String seller, final ItemStack stack, final int amount,
			final double priceBuy, final double priceSell) {
		final World world = block.getWorld();
		final String lcWorld = world.getName().toLowerCase();
		final Set<String> rids = filter.get(lcWorld);
		if (rids == null) return false;
		final ApplicableRegionSet set = Utils.getWorldGuard().getRegionManager(world).getApplicableRegions(block.getLocation());
		if (useFilter){
			boolean matchedFilter = false;
			for (final ProtectedRegion r : set){
				final String lcId = r.getId().toLowerCase();
				if (rids.contains(lcId)){
					matchedFilter = true;
					break;
				}
			}
			if (!matchedFilter) return false;
		}
		final List<String> valid = new LinkedList<String>();
		for (final ProtectedRegion r : set){
			final String lcId = r.getId().toLowerCase();
			if (addUnowned || r.isOwner(seller) || r.isMember(seller)) valid.add(lcId);
		}
		if (valid.isEmpty()) return false;
		// Do add !
		final ShopSpec spec = new ShopSpec(stack, amount, priceBuy, priceSell);
		// block map
		blockMap.put(pos, spec);
		// id mapping set:
		final int id = stack.getTypeId();
		Set<RegionSpec> rs = idMap.get(id);
		if (rs == null){
			rs = new HashSet<RegionSpec>();
			idMap.put(id, rs);
		}
		// Add region spec entries:
		for (final String lcId : valid){
			final RegionSpec rSpec = getRegionSpec(lcWorld, lcId, true);
			rs.add(rSpec);
			rSpec.shops.add(pos);
			spec.regions.add(rSpec);
		}
		return true;
	}

	/**
	 * 
	 * @param lcWorld
	 * @param b
	 * @return
	 */
	private final RegionSpec getRegionSpec(final String lcWorld, final String lcId, final boolean create) {
		Map<String, RegionSpec> rMap = regionMap.get(lcWorld);
		RegionSpec rSpec;
		if (rMap == null){
			if (!create) return null;
			rMap = new HashMap<String, RegionSpec>();
			regionMap.put(lcWorld, rMap);
			rSpec = new RegionSpec(lcWorld, lcId);
			rMap.put(lcId, rSpec);
			return rSpec;
		}
		rSpec = rMap.get(lcId);
		if (rSpec == null){
			if (!create) return null;
			rSpec = new RegionSpec(lcWorld, lcId);
			rMap.put(lcId, rSpec);
			return rSpec;
		}
		return rSpec;
	}

	@Override
	public String[] getCommandLabels() {
		return labels;
	}

	@Override
	public Listener getListener() {
		return this;
	}

	@Override
	public void onCommand(CommandSender sender, String label, String[] args) {
		int len = args.length;
		// ignore label, currently
		
		// shop / shops:
		if (len == 0) sender.sendMessage("[ShopService] Options  (/cx shop ...): info | find <item> | find <region> | list <region> | list <world> <region> |");
		else if (len == 1 && args[0].equalsIgnoreCase("info")) sendInfo(sender);
		else if (len == 2 && args[0].equalsIgnoreCase("find")) onFind(sender, args[1]);
		else if (len == 2 && args[0].equalsIgnoreCase("list")) onList(sender, null, args[1]);
		else if (len == 3 && args[0].equalsIgnoreCase("list")) onList(sender, args[1], args[2]);
		// TODO: list
	}

	private void onList(CommandSender sender, String world, String rid) {
		if ((sender instanceof Player) && world == null) world = ((Player)sender).getWorld().getName();
		sender.sendMessage("[ShopService] Items for "+rid+" (world: "+((world==null)?"<all>":world)+"):");
		if (world == null){
			for (String worldName : regionMap.keySet()){
				sender.sendMessage("("+worldName+"): "+getItemsStr(worldName, rid));
			}
		}
		else sender.sendMessage(getItemsStr(world, rid));
	}

	private final String getItemsStr(final String world, final String rid) {
		// TODO: use digested versions and save them somewhere with timestamps !
		final RegionSpec rSpec = getRegionSpec(world.toLowerCase(), rid.toLowerCase(), false);
		if (rSpec == null) return "<not found>";
		StringBuilder b = new StringBuilder();
		for (final FBlockPos pos : rSpec.shops){
			final ShopSpec spec = blockMap.get(pos);
			b.append(spec.toString());
			b.append(" | ");
		}
		return b.toString();
	}

	/**
	 * Find shops for an item.
	 * @param sender
	 * @param input
	 */
	private void onFind(CommandSender sender, String input) {
		ItemSpec spec = ItemSpec.match(input);
		if (spec != null) sendFindItem(sender , spec);
		else{
			sender.sendMessage("[ShopService] No shops found.");
			if (sender instanceof Player){
				// TODO find region + show distance !
				Player player = (Player) sender;
				World world = player.getWorld();
				String lcWorld = world.getName().toLowerCase();
				Map<String, RegionSpec> rMap = regionMap.get(lcWorld);
				if (rMap != null && rMap.containsKey(input.toLowerCase())){
					ProtectedRegion r = Utils.getWorldGuard().getRegionManager(world).getRegion(input);
					if (r != null){
						double d = getDistanceToCenter(player.getLocation(), r);
						player.sendMessage("[ShopService] Distance to center of "+r.getId()+": "+((int) Math.round(d)));
					}
				}
			}
		}
	}

	private double getDistanceToCenter(Location location, ProtectedRegion r) {
		com.sk89q.worldedit.Vector middle = r.getMinimumPoint().add(r.getMaximumPoint()).multiply(0.5);
		Vector center = new Vector(middle.getX(), middle.getY(), middle.getZ());
		return location.toVector().distance(center);
	}

	private void sendFindItem(CommandSender sender, ItemSpec spec) {
		Set<RegionSpec> specs = idMap.get(spec.id);
		if ( specs == null){
			sender.sendMessage("[ShopService] No shops found.");
			return;
		}
		else{
			String world = null;
			if (sender instanceof Player){
				world = ((Player) sender).getWorld().getName().toLowerCase();
				// TODO: restrict to when players are on a filter region ?
			}
			sender.sendMessage("[ShopService] Shops with item type "+Material.getMaterial(spec.id).toString()+":");
			// TODO: more sophisticated.
			StringBuilder b = new StringBuilder();
			b.append("Regions: ");
			for (RegionSpec rSpec : specs ){
				if (world != null && !rSpec.worldName.equals(world)) continue;
				b.append(" ");
				b.append(rSpec.regionName);
				if (world == null) b.append("("+rSpec.worldName+")");
			}	 
			sender.sendMessage(b.toString());
		}
	}

	private void sendInfo(CommandSender sender) {
		if (sender instanceof Player) sendPlayerInfo((Player) sender);
		sendGeneralInfo(sender);
	}

	private void sendPlayerInfo(Player player) {
		World world = player.getWorld();
		ApplicableRegionSet set = Utils.getWorldGuard().getRegionManager(world).getApplicableRegions(player.getLocation());
		String lcWorld = world.getName().toLowerCase();
		for (ProtectedRegion r : set){
			String rid = r.getId();
			RegionSpec rSpec = getRegionSpec(lcWorld, rid.toLowerCase(), false);
			if (rSpec == null) continue;
			player.sendMessage(rid+": "+rSpec.shops.size()+" chest shops.");
		}
		if (set.size() > 0) player.sendMessage("To list items for a region, use: /cx shop list <region>");
	}

	private void sendGeneralInfo(CommandSender sender) {
		sender.sendMessage("[ShopService] General info: | "+idMap.size()+" Total item types | "+blockMap.size()+" total shops |");
	}


	
}
