package me.asofold.bukkit.contextmanager.hooks.regions;

import me.asofold.bukkit.contextmanager.hooks.AbstractServiceHook;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import asofold.pluginlib.shared.Utils;
import asofold.pluginlib.shared.permissions.pex.PexUtil;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionsHook extends AbstractServiceHook {

	@Override
	public String getHookName() {
		return "Regions";
	}

	@Override
	public String[] getCommandLabels() {
		return new String[]{"regions", "rgs"};
	}

	@Override
	public void onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 2){
			if (!trySendDistance(sender, args[1], true)) sendUsage(sender);
		}
		else sendUsage(sender);
	}
	
	/**
	 * Send distance to region, if player is owner or member or has permission.
	 * @param sender
	 * @param rid
	 * @param message if to messsage on failure.
	 * @return
	 */
	public boolean trySendDistance(CommandSender sender, String rid, boolean message) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;
		World world = player.getWorld();
		ProtectedRegion region = Utils.getWorldGuard().getRegionManager(world).getRegion(rid);
		if (region != null){
			String playerName = player.getName();
			if (!region.isMember(playerName) && !region.isOwner(playerName)){
				if (!PexUtil.hasPermission(playerName, "regions.find.w."+(world.getName().toLowerCase())+".r."+(region.getId().toLowerCase())) ) region = null;
			}
		}
		if (region == null){
			if (message) player.sendMessage(ChatColor.RED+"[Regions] Not available: "+rid);
			return false;
		}
		sendDistance(player, region);
		return true;
	}

	boolean sendUsage(CommandSender sender){
		sender.sendMessage(ChatColor.RED+"Only players: /cx regions find <region> | /cx find <region>");
		return false;
	}

	public static void sendDistance(Player player, ProtectedRegion region) {
		Location loc = player.getLocation();
		BlockVector min = region.getMinimumPoint();
		BlockVector max = region.getMaximumPoint();
		int cx = (max.getBlockX() + min.getBlockX()) / 2;
		int cy = (max.getBlockY() + min.getBlockY()) / 2;
		int cz = (max.getBlockZ() + min.getBlockZ()) / 2;
		
		int rx = (max.getBlockX() - min.getBlockX()) / 2;
		int ry = (max.getBlockY() - min.getBlockY()) / 2;
		int rz = (max.getBlockZ() - min.getBlockZ()) / 2;
		
		int dx = cx - loc.getBlockX();
		int dy = cy - loc.getBlockY();
		int dz = cz - loc.getBlockZ();
		StringBuilder b = new StringBuilder();
		b.append(ChatColor.DARK_GRAY+"[Regions] "+ChatColor.GREEN+region.getId()+ChatColor.GRAY+": "+ChatColor.AQUA);
		if (dx > rx) b.append((dx-rx)+" South ");
		else if (dx < -rx) b.append((-dx-rx)+" North ");
		if (dz > rz) b.append((dz-rz) + " West ");
		else if (dz < -rz) b.append((-dz-rz) + " East ");
		if (dy > ry) b.append((dy-ry) + " Up");
		else if (dy < -ry) b.append((-dy-ry) + " Down");
		player.sendMessage(b.toString());
	}

	@Override
	public boolean delegateFind(CommandSender sender, String[] args) {
		if (args.length == 2) return trySendDistance(sender, args[1], false);
		else return false;
	}
	
	

}
