package me.asofold.bpl.contextmanager.hooks.chestshop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.asofold.bpl.contextmanager.command.AliasMap;
import me.asofold.bpl.contextmanager.config.compatlayer.CompatConfig;
import me.asofold.bpl.contextmanager.config.compatlayer.CompatConfigFactory;
import me.asofold.bpl.contextmanager.config.compatlayer.ConfigUtil;
import me.asofold.bpl.contextmanager.core.CMCore;
import me.asofold.bpl.contextmanager.hooks.AbstractServiceHook;
import me.asofold.bpl.contextmanager.hooks.regions.RegionsHook;
import me.asofold.bpl.contextmanager.plshared.Blocks;
import me.asofold.bpl.contextmanager.plshared.Inventories;
import me.asofold.bpl.contextmanager.plshared.Logging;
import me.asofold.bpl.contextmanager.plshared.Messaging;
import me.asofold.bpl.contextmanager.plshared.Utils;
import me.asofold.bpl.contextmanager.plshared.blocks.FBlockPos;
import me.asofold.bpl.contextmanager.plshared.items.ItemSpec;
import me.asofold.bpl.contextmanager.plshared.messaging.json.JMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.Acrobot.ChestShop.Utils.uName;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * This hook gets added in onEnable.
 * @author mc_dev
 *
 */
public class ChestShopHook extends AbstractServiceHook implements Listener{
	
	public ChestShopHook(){
		if (Bukkit.getPluginManager().getPlugin("ChestShop") == null) throw new RuntimeException("ChestShop");
	}
	
	private final static String[] labels = new String[]{"chestshop"};
	
	// TODO: filter by another region, if desired.
	
	private final Map<FBlockPos, ShopSpec> blockMap = new HashMap<FBlockPos, ShopSpec>();
	
	/**
	 * For checking ...
	 */
	private final Map<String, Map<String, RegionSpec>> regionMap = new HashMap<String, Map<String,RegionSpec>>();
	
	/**
	 * Mapping raw id to items.
	 */
	private final Map<Integer, Set<RegionSpec>> idMap = new HashMap<Integer, Set<RegionSpec>>();
	
	/**
	 * Only if one of these regions matches, regions will be added.
	 */
	private final Map<String, Set<String>> filter = new HashMap<String, Set<String>>();
	
	/**
	 * Lower case names to lower case names.
	 */
	private final Map<String, String> shopOwners = new HashMap<String, String>();

	
	// TODO: sort in to filter ! -> available item types.
	
	/**
	 * if false, only regions owned by the shop owner will get considered, but one region must match filter.
	 */
	boolean addUnowned = false;
	
	boolean useFilter = true;
	
	boolean reverseButtons = false;
	
	
	static final long msDay = 1000*60*60*24;
	
	long durExpire = 14 * msDay;
	
	private final AliasMap aliasMap = new AliasMap(
			new String[][]{
					{"find", "f"},
					{"list", "lst", "ls", "l"},
					{"info", "inf", "i"},
			}
			);

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
	
	@Override
	public String getHookName() {
		return "ShopService(ChestShop3)";
	}
	
	public void tellStock(final Player player, final Block chestBlock, final Block signBlock) {
		final Sign sign = (Sign) signBlock.getState();
		final String[] lines = sign.getLines();
		// TODO: pluginlib shops ...
		if (!com.Acrobot.ChestShop.Signs.ChestShopSign.isValid(lines)) return;
		final ItemStack stack = com.Acrobot.Breeze.Utils.MaterialUtil.getItem(lines[3]);
		if (stack == null || stack.getType() == Material.AIR) return; 
		final List<Inventory> inventories = Inventories.getChestInventories(chestBlock);
		final String priceSpec = lines[2];
		final double priceBuy = com.Acrobot.Breeze.Utils.PriceUtil.getBuyPrice(priceSpec);
		String spec = "";
		if (priceBuy >= 0.0) {
			spec += "Items on stock: " + Inventories.countItems(inventories, stack) + " ";
		}
		final double priceSell= com.Acrobot.Breeze.Utils.PriceUtil.getSellPrice(priceSpec);
		if (priceSell >= 0.0) {
			spec += "Space for items: " + Inventories.getSpace(inventories, stack) + " ";
		}
		if (!spec.isEmpty()) {
			player.sendMessage(ChatColor.AQUA + spec);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	final void onPlayerInteract(final PlayerInteractEvent event){
//		if (event.isCancelled()) return;
		Action action = event.getAction();
		if (action!= Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return; // [mind leftclick sell]
		final Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.WALL_SIGN) {
			if ((block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) && event.isCancelled()) {
				final Block above = block.getRelative(BlockFace.UP);
				final Material mat = above.getType();
				if (mat == Material.WALL_SIGN || mat == Material.SIGN_POST) {
					tellStock(event.getPlayer(), block, above);
				}
			}
			return;
		}
		// Check wall signs for ChestShop syntax:
		final Sign sign = (Sign) block.getState();
		final String[] lines = sign.getLines();
		
		// TODO: split here if desied to add signChange event ? [would change concept though]
		
//		if (!uSign.isValid(lines)) return;
		if (!com.Acrobot.ChestShop.Signs.ChestShopSign.isValid(lines)) return;
		final Player player = event.getPlayer();
		final String playerName = player.getName();
		// TODO: maybe better heck or leave it out:
		final String shopOwner = getShopOwner(lines[0]);
		if (playerName.equalsIgnoreCase(shopOwner)) return; // ignore the shop owner.
		final String priceSpec = lines[2];
		// Chest:
		final Block ref = block.getRelative(BlockFace.DOWN);
		List<Inventory> inventories = Inventories.getChestInventories(ref);
		if (inventories.isEmpty()){
			// only allow standard chest setup.
			update(block, null, null, 0, -1.0, -1.0);
			return;
		}
		// Price etc:
		int amount = -1;
		try{
			amount = Integer.parseInt(lines[1].trim());
		}
		catch (Throwable t){
		}
		if (amount <= 0){
			update(block, null, null, 0, -1.0, -1.0);
			return;
		}
		double priceBuy = com.Acrobot.Breeze.Utils.PriceUtil.getBuyPrice(priceSpec);
		double priceSell= com.Acrobot.Breeze.Utils.PriceUtil.getSellPrice(priceSpec);
		
		ItemStack stack = com.Acrobot.Breeze.Utils.MaterialUtil.getItem(lines[3]);
		if(stack == null){
			update(block, null, null, 0, -1.0, -1.0);
			return;
		}
		final int id = stack.getTypeId();
		final int data;
		if (stack.getType().isBlock()) data = stack.getData().getData();
		else data = stack.getDurability();
		if (priceBuy >= 0){
			// Invalidate if out of stock.
			if (!Inventories.hasItems(inventories, id, data, amount)) priceBuy = -1.0;
		}
		else{
			// ignore buying
			if (reverseButtons && action == Action.LEFT_CLICK_BLOCK) return;
			else if (!reverseButtons && action == Action.RIGHT_CLICK_BLOCK) return;
		}
		
		
		if (priceSell >= 0){
			// Invalidate if chest has not enough space.
			if (!Inventories.hasSpace(inventories, id, data, amount)) priceSell = -1.0;
		}
		else{
			// ignore selling
			if (!reverseButtons && action == Action.LEFT_CLICK_BLOCK) return;
			else if (reverseButtons && action == Action.RIGHT_CLICK_BLOCK) return;
		}
		
		update(block, shopOwner, stack, amount, priceBuy, priceSell);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	final void onBlockBreak(BlockBreakEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		if (block.getType() != Material.WALL_SIGN) return;
		// TODO: maybe this is fastest, maybe not.
		update(block, null, null, 0, -1.0, -1.0);
	}
	
	public final void update(final Block block, String shopOwner, final ItemStack stack, final int amount, final double priceBuy, final double priceSell) {
		final FBlockPos pos = Blocks.FBlockPos(block);
		final ShopSpec spec = blockMap.get(pos);
		if (spec == null){
			if (priceBuy > 0 || priceSell > 0){
				checkAddShopSpec(pos, block, shopOwner, stack, amount, priceBuy, priceSell);
			}
		}
		else{
			// TODO: update might be more specialized later on.
			if (priceBuy < 0 && priceSell < 0){
				// Do remove this shop.
				removeShopSpec(pos, spec);
			}
			else{
				// TODO: 
				final int id = spec.stack.getTypeId();
				spec.update(shopOwner, stack, amount, priceBuy, priceSell);
				if (id != stack.getTypeId()) checkId(spec.regions, id);
			}
		}	
	}

	/**
	 * convenience method for simple removal.
	 * @param pos
	 * @return
	 */
	private boolean removeShop(FBlockPos pos){
		ShopSpec spec = blockMap.get(pos);
		if (spec == null) return false;
		removeShopSpec(pos, spec);
		return true;
	}

	/**
	 * Call this to remove a spec.
	 * @param pos
	 * @param spec
	 */
	private void removeShopSpec(FBlockPos pos, ShopSpec spec) {
		blockMap.remove(pos);
		final int id = spec.stack.getTypeId();
		Set<RegionSpec> rs = idMap.get(id);
		// TODO: 
		for (RegionSpec rSpec : spec.regions){
			rSpec.shops.remove(pos);
			boolean has = false;
			
			
			if (rSpec.shops.isEmpty()){
				removeRegionSpec(rSpec);
			}
			else{
				// TODO: might still have to remove the id mapping !
				for (final FBlockPos refPos : rSpec.shops){
					final ShopSpec refSpec = blockMap.get(refPos);
					if (refSpec.stack.getTypeId() == id){
						has = true;
						break;
					}
				}
			}
			if (!has){
				
				if (rs!=null){
					rs.remove(rSpec);
					if (rs.isEmpty()) idMap.remove(id);
				}
			}
		}
		// "cleanup":
		spec.regions.clear();
		spec.stack = null;
	}
	
	/**
	 * check if the id is still in the region, and remove mappings otherwise.
	 * @param regions
	 * @param id
	 */
	private final void checkId(final List<RegionSpec> regions, final int id) {
		final Set<RegionSpec> rs = idMap.get(id);
		if (rs == null) return;
		for (RegionSpec rSpec : regions){
			 if (rs.contains(rSpec)){
				 boolean has = false;
				 for (FBlockPos pos : rSpec.shops){
					 ShopSpec spec = blockMap.get(pos);
					 if (spec == null) continue;
					 if (spec.stack.getTypeId() == id){
						 has = true;
						 break;
					 }
				 }
				 if (!has){
					 rs.remove(rSpec);
					 if (rs.isEmpty()){
						 idMap.remove(id);
						 break;
					 }
				 }
			 }
		}
		
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
	 * @param shopOwner
	 * @param stack
	 * @param amount
	 * @param priceBuy
	 * @param priceSell
	 * @return
	 */
	public final boolean checkAddShopSpec(final FBlockPos pos, final Block block, final String shopOwner, final ItemStack stack, final int amount,
			final double priceBuy, final double priceSell) {
		final Location loc = block.getLocation();
		
		// Check if passes filter / regions:
		final List<String> valid = getValidRids(loc, shopOwner);
		if (valid == null || valid.isEmpty()) return false;
		
		// Do add !
		final ShopSpec spec = new ShopSpec(shopOwner, stack, amount, priceBuy, priceSell);
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
		final String lcWorld = loc.getWorld().getName().toLowerCase();
		for (final String lcId : valid){
			final RegionSpec rSpec = getRegionSpec(lcWorld, lcId, true);
			rs.add(rSpec);
			rSpec.shops.add(pos);
			spec.regions.add(rSpec);
		}
		return true;
	}

	public final List<String> getValidRids(final Location loc, final String shopOwner) {
		final World world = loc.getWorld();
		final String lcWorld = world.getName().toLowerCase();
		final Set<String> rids = filter.get(lcWorld);
		if (useFilter && rids == null) return null;
		final ApplicableRegionSet set = Utils.getWorldGuard().getRegionManager(world).getApplicableRegions(loc);
		if (useFilter){
			boolean matchedFilter = false;
			for (final ProtectedRegion r : set){
				final String lcId = r.getId().toLowerCase();
				if (rids.contains(lcId)){
					matchedFilter = true;
					break;
				}
			}
			if (!matchedFilter) return null;
		}
		final List<String> valid = new LinkedList<String>();
		for (final ProtectedRegion r : set){
			final String lcId = r.getId().toLowerCase();
			if (addUnowned || shopOwner == null || r.isOwner(shopOwner) || r.isMember(shopOwner)) valid.add(lcId);
		}
		return valid;
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
	public String[] getCommandLabelAliases(String label) {
		if (label.equals("chestshop")){
			return new String[]{"chestshops", "shop" , "shops"};
		}
		else return null;
	}

	@Override
	public Listener getListener() {
		return this;
	}

	@Override
	public void onCommand(CommandSender sender, String label, String[] args) {
		int len = args.length;
		// ignore label, currently
		String cmd = null;
		if (len>0) cmd = aliasMap.getMappedCommandLabel(args[0]);
		
		// shop / shops:
		if (len == 0){
			sendUsage(sender);
			return;
		}
		else if (cmd.equals("reload")){
			if (!me.asofold.bpl.contextmanager.util.Utils.checkPerm(sender, "contextmanager.admin.cmd.reload")) return;
			if (len == 2 && args[1].equalsIgnoreCase("data")){
				loadData();
				sender.sendMessage("[ShopService] Reloaded settings (kept data).");
			}
			if (len == 2 && args[1].equalsIgnoreCase("all")){
				loadSettings();
				loadData();
				sender.sendMessage("[ShopService] Reloaded settings and data.");
			}
			else{
				loadSettings();
				sender.sendMessage("[ShopService] Reloaded settings (kept data).");
			}
			return;
		}
		else if (cmd.equals("save")){
			if (!me.asofold.bpl.contextmanager.util.Utils.checkPerm(sender, "contextmanager.admin.cmd.save")) return;
			saveData();
			sender.sendMessage("[ShopService] Saved data.");
			return;
		}
		else if (len == 2 && cmd.equals("clear") && args[1].equalsIgnoreCase("data")){
			if (!me.asofold.bpl.contextmanager.util.Utils.checkPerm(sender, "contextmanager.admin.cmd.reload")) return;
			clearData();
			sender.sendMessage("[ShopService] Cleared data.");
			return;
		}
		else if (len == 1 && cmd.equals("info")){
			sendInfo(sender);
			return;
		}
		else if (len == 2 && cmd.equals("find")){
			onFindOrList(sender, null, args[1], null);
			return;
		}
		else if (len == 3 && cmd.equals("find")){
			onFindOrList(sender, null, args[1], args[2]);
			return;
		}
		else if (len == 3 && cmd.equals("find")){
			onFindOrList(sender, args[1], args[2], null);
			return;
		}
		else if (len == 4 && cmd.equals("find")){
			onFindOrList(sender, args[1], args[2], args[3]);
			return;
		}
		else if (len == 2 && (cmd.equals("list") || cmd.equals("info"))) onListOrFind(sender, null, args[1], null);
		else if (len == 3 && (cmd.equals("list") || cmd.equals("info"))) onListOrFind(sender, args[1], args[2], null);
		else if (len == 4 && (cmd.equals("list") || cmd.equals("info"))) onListOrFind(sender, args[1], args[2], args[3]);
		else if (len == 1){
			// remaining commands:
			onListOrFind(sender, null, args[0], null); 
		}
		else if (len == 2){
			// remaining commands:
			onListOrFind(sender, args[0], args[1], null);
		}
		else if (len == 3){
			// remaining commands:
			onListOrFind(sender, args[0], args[1], args[2]);
		}
		else sendUsage(sender); // hmm
		// TODO: list
	}
	
	
	
	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args)
	{
		final Set<String> choices = new LinkedHashSet<String>(); // TODO
		if (args.length == 1){
			String arg = args[0].trim().toLowerCase();
			if (me.asofold.bpl.contextmanager.util.Utils.hasPermission(sender, "contextmanager.admin.cmd.reload")){
				if ("reload".startsWith(arg)) choices.add("reload");
				if ("clear".startsWith(arg)) choices.add("clear");
			}
			if (me.asofold.bpl.contextmanager.util.Utils.hasPermission(sender, "contextmanager.admin.cmd.save")){
				if ("save".startsWith(arg)) choices.add("save");
			}
			aliasMap.fillInTabCompletions(arg, choices, null);
		}
		else if (args.length == 2){
			String cmd = args[0].trim().toLowerCase();
			String arg = args[1].trim().toLowerCase();
			if (me.asofold.bpl.contextmanager.util.Utils.hasPermission(sender, "contextmanager.admin.cmd.reload")){
				if (cmd.equals("reload")){
					for (String ref : new String[]{
							"all", "data", "settings",
					}){
						if (ref.startsWith(arg)) choices.add(ref);
					}
					if ("configuration".startsWith(arg)) choices.add("settings");
				}
				else if (cmd.equals("clear") && "data".startsWith(arg)){
					choices.add("data");
				}
			}
		}
		return me.asofold.bpl.contextmanager.util.Utils.sortedList(choices); // No completion for names.
	}

	@Override
	public boolean delegateFind(CommandSender sender, String[] args) {
		String world = null;
		String query;
		String prefix;
		if (args.length == 4){
			world = args[1];
			query = args[2];
			prefix = args[3];
		}
		if (args.length == 3){
			world = args[1];
			query = args[2];
			prefix = null;
		}
		else if (args.length == 2){
			if (sender instanceof Player) world = ((Player) sender).getWorld().getName();
			query = args[1];
			prefix = null;
		}
		else return false;
		if (onFind(sender, world, query)) return true;
		else if (onList(sender, world, query, prefix)) return true;
		else return false;
	}

	private void onFindOrList(CommandSender sender, String world, String input, String prefix) {
		if (onFind(sender, world, input)) return;
		if (!onList(sender, world, input, prefix)) sender.sendMessage("[ShopService] No matches found.");
	}

	private void onListOrFind(CommandSender sender, String world, String input, String prefix) {
		if (onList(sender, world, input, prefix)) return;
		if (!onFind(sender, world, input)) sender.sendMessage("[ShopService] No matches found.");
 	}

	private void sendUsage(CommandSender sender) {
		sender.sendMessage("[ShopService] Options  (/cx shop ...): info | find <item> | find <region> | list <region> | list <world> <region> |");
	}

	private boolean onList(CommandSender sender, String world, String rid, String prefix) {
		if ((sender instanceof Player) && world == null) world = ((Player)sender).getWorld().getName();
		List<String> msgs = new LinkedList<String>();
		if (world == null){
			for (String worldName : regionMap.keySet()){
				String msg = getItemsStr(worldName, rid, prefix);
				if (msg != null) msgs.add("("+worldName+"): "+msg);
			}
		}
		else{
			String msg = getItemsStr(world, rid, prefix);
			if (msg != null) msgs.add(msg);
		}
		
		if (msgs.isEmpty()) return false;
		else{
			msgs.add(0, "[ShopService] Items for "+rid+" (world: "+((world==null)?"<all>":world)+"):");
			sender.sendMessage(msgs.toArray(new String[msgs.size()]));
			return true;
		}
	}

	private final String getItemsStr(final String world, final String rid, final String prefix) {
		// TODO: use digested versions and save them somewhere with timestamps !
		final RegionSpec rSpec = getRegionSpec(world.toLowerCase(), rid.toLowerCase(), false);
		if (rSpec == null) return null;
		return rSpec.getItemStrings(blockMap, prefix == null ? null : prefix.trim().toLowerCase());
	}

	/**
	 * Find shops for an item.
	 * @param sender
	 * @param input
	 */
	private boolean onFind(CommandSender sender, String worldName, String input) {
		ItemSpec spec = ItemSpec.match(input);
		if (spec != null){
			return sendFindItem(sender , worldName, spec);
		}
		else{
			boolean found = false;
			if (sender instanceof Player){
				// TODO find region + show distance !
				Player player = (Player) sender;
				World world = player.getWorld();
				String lcWorld = world.getName().toLowerCase();
				if (lcWorld.equalsIgnoreCase(worldName)){
					Map<String, RegionSpec> rMap = regionMap.get(lcWorld);
					if (rMap != null && rMap.containsKey(input.toLowerCase())){
						ProtectedRegion r = Utils.getWorldGuard().getRegionManager(world).getRegion(input);
						if (r != null){
							RegionsHook.sendDistance(player, r);
//							double d = getDistanceToCenter(player.getLocation(), r);
//							player.sendMessage("[ShopService] Distance to center of "+r.getId()+": "+((int) Math.round(d)));
							found = true;
						}
					}
				}

			}
			return found;
		}
	}

	

//	private double getDistanceToCenter(Location location, ProtectedRegion r) {
//		com.sk89q.worldedit.Vector middle = r.getMinimumPoint().add(r.getMaximumPoint()).multiply(0.5);
//		Vector center = new Vector(middle.getX(), middle.getY(), middle.getZ());
//		return location.toVector().distance(center);
//	}

	private boolean sendFindItem(CommandSender sender, String world, ItemSpec spec) {
		Set<RegionSpec> specs = idMap.get(spec.id);
		if (world != null) world = world.toLowerCase(); 
		if ( specs == null){
			return false;
		}
		else{
			if (world == null && sender instanceof Player){
				world = ((Player) sender).getWorld().getName().toLowerCase();
				// TODO: restrict to when players are on a filter region ?
			}
			sender.sendMessage("[ShopService] Shops with item type "+Material.getMaterial(spec.id).toString()+":");
			// TODO: more sophisticated.
			List<Object> components = new LinkedList<Object>();
			components.add("Regions:");
			Player player = null;
			String playerWorld = null;
			if (sender instanceof Player) {
				player = (Player) sender;
				playerWorld = player.getWorld().getName();
			}
			for (RegionSpec rSpec : specs ){
				if (world != null && !rSpec.worldName.equals(world)) continue;
				components.add(" ");
				String rid;
				if (world == null) rid = rSpec.regionName + "("+rSpec.worldName+")";
				else rid = rSpec.regionName;
				if (playerWorld != null && playerWorld.equalsIgnoreCase(rSpec.worldName)) {
					// Player, the region can be found.
					components.add(new JMessage(rid, "/context region find " + rSpec.regionName, "Click to find the region " + rSpec.regionName + " !"));
				} else {
					components.add(rid);
				}
			}	 
			Messaging.sendComplexMessage(sender, components);
		}
		return true;
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

	private int getRegionCount() {
		int n = 0;
		for (Map<String, RegionSpec> set : regionMap.values()){
			n += set.size();
		}
		return n;
	}

	private void sendGeneralInfo(CommandSender sender) {
		sender.sendMessage("[ShopService] General info: | "+idMap.size()+" Total item types | "+blockMap.size()+" total chest shops on "+getRegionCount()+" regions.");
	}
	
	@Override
	public void onEnable(Plugin plugin) {
		loadSettings();
		loadData();
	}
	
	@Override
	public void onDisable() {
		saveData();
		clear();
	}

	@Override
	public void onRemove() {
		clear();
	}
	
	private void clearData() {
		idMap.clear();
		shopOwners.clear();
		// not sure this is really needed, might make de referencing fatser, for later.
		for (Map<String, RegionSpec> map : regionMap.values()){
			for (RegionSpec rSpec : map.values()){
				rSpec.shops.clear();
			}
		}
		regionMap.clear();
		for (ShopSpec spec : blockMap.values()){
			spec.regions.clear();
			spec.stack = null;
		}
		blockMap.clear();
	}

	private void clear() {
		clearData();
		filter.clear();
	}

	private File getDataFolder(){
		File out = new File(new File(CMCore.getPlugin().getDataFolder(), "hooks"),"chestshop");
		if (!out.exists()) out.mkdirs();
		return out;
	}

	private File getSettingsFile() {
		return new File (getDataFolder(), "settings.yml");
	}
	
	private File getFilterFile() {
		return new File (getDataFolder(), "filter.yml");
	}
	
	private File getDataFile() {
		return new File (getDataFolder(), "shops.yml");
	}
	
	private static CompatConfig getDefaultConfig(){
		CompatConfig cfg = CompatConfigFactory.getConfig(null);
		cfg.set("use-filter", true);
		cfg.set("add-unowned", false);
		cfg.set("expiration-duration", 31);
		cfg.set("reverse-buttons", false);
		return cfg;
	}
	
	private void loadSettings() {
		File file = getSettingsFile();
		CompatConfig cfg = CompatConfigFactory.getConfig(file);
		cfg.load();
		if (ConfigUtil.forceDefaults(getDefaultConfig(), cfg)) cfg.save();
		useFilter = cfg.getBoolean("use-filter", true);
		addUnowned = cfg.getBoolean("add-unowned", false);
		durExpire = cfg.getLong("expiration-duration", 31L)*msDay;
		reverseButtons = cfg.getBoolean("reverse-buttons", false);
		loadFilter();
	}
	
	/**
	 * (Part of loadData.)
	 */
	private void loadFilter() {
		filter.clear();
		File file = getFilterFile();
		if (!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				Logging.warn("[ShopService] Could not create filter file", e);
			}
			return;
		}
		CompatConfig cfg = CompatConfigFactory.getConfig(file);
		cfg.load();
		for (String world : cfg.getStringKeys("allow-regions")){
			for (String rid : cfg.getStringList("allow-regions."+world, new LinkedList<String>())){
				addFilter(world, rid);
			}
		}
	}
	
	private final void saveData() {
		final File file = getDataFile();
		final CompatConfig cfg = CompatConfigFactory.getConfig(file);
		int i = 0;
		final long ts = System.currentTimeMillis();
		final List<FBlockPos> rem = new LinkedList<FBlockPos>();
		for (final FBlockPos pos : blockMap.keySet()){
			ShopSpec spec = blockMap.get(pos);
			if (spec == null) continue; // overly ...
			if (spec.priceBuy <0 && spec.priceSell < 0) continue; // overly ...
			if (ts - spec.tsAccess > durExpire){
				rem.add(pos);
				continue;
			}
			i++;
			final String keyBase = "s"+i+".";
			writeShopSpec(cfg, keyBase, pos, spec);
		}
		cfg.save();
		for (final FBlockPos pos : rem){
			removeShop(pos);
		}
	}
	
	private final void loadData() {
		clearData();
		final File file = getDataFile();
		final CompatConfig cfg = CompatConfigFactory.getConfig(file);
		cfg.load();
		final long ts = System.currentTimeMillis();
		final List<String> keys = cfg.getStringKeys();
		for (final String key : keys){
			// TODO: try catch !
			try{
				readShopSpec(cfg, key, ts);
			}
			catch(Throwable t){
				Logging.warn("[ShopService] Bad shop spec at: "+key, t);
			}
		}
	}
	
	private final void writeShopSpec(final CompatConfig cfg, final String keyBase, FBlockPos pos, final ShopSpec spec){
		cfg.set(keyBase+"w", pos.w);
		cfg.set(keyBase+"x", pos.x);
		cfg.set(keyBase+"y", pos.y);
		cfg.set(keyBase+"z", pos.z);
		cfg.set(keyBase+"id", spec.stack.getTypeId());
		if (spec.owner != null) cfg.set(keyBase+"o", spec.owner);
		if (spec.amount != 1) cfg.set(keyBase + "n", spec.amount);
		final int d;
		if (spec.stack.getType().isBlock()) d = spec.stack.getData().getData();
		else d = spec.stack.getDurability();
		if (d != 0) cfg.set(keyBase+"d", d);
		if (spec.priceBuy>=0) cfg.set(keyBase+"pb", spec.priceBuy);
		if (spec.priceSell>=0) cfg.set(keyBase+"ps", spec.priceSell);
		cfg.set(keyBase+"ts", spec.tsAccess);
	}

	/**
	 * Read from config + add to internals
	 * @param cfg
	 * @param key
	 * @param ts
	 */
	private final void readShopSpec(final CompatConfig cfg, final String key, final long ts) {
		final String keyBase = key + ".";
		final long tsA = cfg.getLong(keyBase+"ts", 0L);
		if (ts - tsA > durExpire) return; // ignore expired entries.
		final String w = cfg.getString(keyBase+"w");
		final World world = Bukkit.getWorld(w);
		if (world == null) return;
		final int x = cfg.getInt(keyBase+"x");
		final int y = cfg.getInt(keyBase+"y");
		final int z = cfg.getInt(keyBase+"z");
		final String shopOwner = getShopOwner(cfg.getString(keyBase + "o", null));
		FBlockPos pos = new FBlockPos(w, x, y, z);
		Block block = world.getBlockAt(x, y, z);
		final double pb = cfg.getDouble(keyBase+"pb", -1.0);
		final double ps = cfg.getDouble(keyBase+"ps", -1.0);
		final int id = cfg.getInt(keyBase+"id", 0);
		final int amount = cfg.getInt(keyBase+"n", 1);
		if (id == 0) return;
		int d = cfg.getInt(keyBase+"d", 0);
		final Material mat = Material.getMaterial(id);
		final ItemStack stack;
		if (mat.isBlock()) stack = new ItemStack(mat, 0, (short) d);
		else stack = new ItemStack(mat, 0, (short) d);
		checkAddShopSpec(pos, block, shopOwner, stack, amount, pb, ps);
		// set timestamp if added: [WORKAROUND]
		ShopSpec spec = blockMap.get(pos);
		if (spec != null) spec.tsAccess = tsA;
	}

	/**
	 * Get standard lower case name, ensure that references are used internally.
	 * @param name
	 * @return
	 */
	private final String getShopOwner(final String name) {
		// TODO: add entries for long name mapping from chestshop ?
		if (name == null) return null; // admin shop
		final String longName = uName.getName(name);
		final String lcn;
		if (longName != null) lcn = longName.trim().toLowerCase();
		else lcn = name.trim().toLowerCase();
		final String ref = shopOwners.get(lcn);
		if (ref == null){
			shopOwners.put(lcn, lcn);
			return lcn;
		}
		return ref;
	}
	
	
	
}
