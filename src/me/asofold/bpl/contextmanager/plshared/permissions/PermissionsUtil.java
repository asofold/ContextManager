package me.asofold.bpl.contextmanager.plshared.permissions;

import java.util.LinkedList;
import java.util.List;

import me.asofold.bpl.contextmanager.plshared.Messaging;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PermissionsUtil {

	/**
	 * Get a list with all online players that have the permission.
	 * @param perm
	 * @return
	 */
	public static final List<Player> getOnlinePlayersWithPermission(final String perm) {
		return getPlayersWithPermission(Bukkit.getServer().getOnlinePlayers(), perm);
	}

	/**
	 * Return list with players that have the permission.
	 * @param players
	 * @param perm
	 * @return
	 */
	public  static final List<Player> getPlayersWithPermission(final Player[] players, final String perm) {
		final List<Player> out = new LinkedList<Player>();
		for (Player p : players) {
			if (p.hasPermission(perm)) {
				out.add(p);
			}
		}
		return out;
	}

	/**
	 * Broadcast message to all players that have the permission.
	 * @param string
	 * @param string2
	 */
	public static final void broadcastIfPerm(final String msg, final String perm) {
		for (Player player : getOnlinePlayersWithPermission(perm)) {
			player.sendMessage(msg);
		}
	}

	/**
	 * Broadcast message to all players in that world that have the permission.
	 * @param string
	 * @param string2
	 */
	public static final void broadcastIfPerm(final World world, final String msg, final String perm) {
		for (Player player : getOnlinePlayersWithPermission(perm)) {
			if (world.equals(player.getWorld())) {
				player.sendMessage(msg);
			}
		}
	}

	/**
	 * Broadcast a json message to all players in that world that have the permission.
	 * @param world
	 * @param perm
	 * @param component
	 */
	public static final void broadcastIfPerm(final World world, final String perm, Object... components) {
		for (Player player : getOnlinePlayersWithPermission(perm)) {
			if (world.equals(player.getWorld())) {
				Messaging.sendComplexMessage(player, components);
			}
		}
	}

}
