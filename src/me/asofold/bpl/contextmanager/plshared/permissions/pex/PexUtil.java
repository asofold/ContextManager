package me.asofold.bpl.contextmanager.plshared.permissions.pex;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.asofold.bpl.contextmanager.plshared.Messaging;
import me.asofold.bpl.contextmanager.plshared.players.SortablePlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;


public class PexUtil {
	
	// Unchanged signature.
	
	public static final String[] findDecoration(final PermissionUser user) {
		String prefix = checkedDeco(user.getOwnPrefix(null));
		String suffix = checkedDeco(user.getOwnSuffix(null));
		// TODO: world dependent ?
		if (prefix==null || suffix==null) {
			// TODO: What with the deprecation of user.getGroups?
			@SuppressWarnings("deprecation")
			final PermissionGroup[] groups = user.getGroups();
			if (groups == null) return new String[]{prefix, suffix};
			if (prefix == null) {
				int weight = Integer.MIN_VALUE;
				for (final PermissionGroup group : groups) {
					if ((prefix !=null) && (group.getWeight()<=weight)) continue;
					final String tempPrefix = checkedDeco(group.getPrefix(null));
					if (tempPrefix!=null) {
						weight = group.getWeight();
						prefix = tempPrefix;
					}
				}
			}
			if (suffix == null) {
				int weight = Integer.MIN_VALUE;
				for (final PermissionGroup group : groups) {
					if ((suffix !=null) && (group.getWeight()<=weight)) continue;
					final String tempSuffix = checkedDeco(group.getSuffix(null));
					if (tempSuffix!=null) {
						weight = group.getWeight();
						suffix = tempSuffix;
					}
				}
			}
			// TODO: maybe also check for sub groups ? / make configurable
		}
		return new String[]{prefix, suffix};
	}
	
	/**
	 * Workaround for problems with pex.
	 * @param deco
	 * @return
	 */
	public static final String checkedDeco(final String deco) {
		if (deco == null) return null;
		else if (deco.isEmpty()) return null;
		else if (deco.equals("null")) return null;
		return deco;
	}
	
	public static final String getOnlinePlayersString() {
		return getOnlinePlayersString(null);
	}
	
	public static final String getOnlinePlayersString(final Player[] players, final Map<String, String> extraSuffixes) {
		final StringBuilder builder = new StringBuilder(500);
		boolean empty = true;
		for (SortablePlayer sortablePlayer : SortablePlayer.getSortedPlayers(players)) {
			final Player player = sortablePlayer.getOnline();
			if (player == null) {
				continue;
			}
			if (!empty) {
				builder.append(ChatColor.DARK_GRAY+" | ");
			}
			String playerName = player.getName();
			builder.append(PexUtil.getDecoratedPlayerName(player.getUniqueId(), playerName));
			if (extraSuffixes != null) {
				String suffix = extraSuffixes.get(playerName.toLowerCase());
				if (suffix != null) builder.append(suffix);
			}
			empty = false;
		}
		builder.append(ChatColor.WHITE);
		return builder.toString();
	}
	
	public static final void sendOnlinePlayerList(final CommandSender sender) {
		sendOnlinePlayerList(sender, null);
	}
	
	public static final String getDecoratedPlayerName(final Player player) {
		final PermissionUser user = PermissionsEx.getUser(player);
		return getDecoratedPlayerName(user, player.getName());
	}
	
	public static final String getDecoratedPlayerName(final PermissionUser user, final String displayName) {
		final String[] dec = findDecoration(user);
		String out = displayName;
		if (dec[0] != null) out = Messaging.withChatColors(dec[0]) + out;
		if (dec[1] != null) out = out + Messaging.withChatColors(dec[1]);
		return ChatColor.WHITE+out;
	}
	
	public static final String getOnlinePlayersString(final Map<String, String> extraSuffixes) {
		Player[] players = Bukkit.getServer().getOnlinePlayers();
		return getOnlinePlayersString(players, extraSuffixes);
	}
	
	public static final void sendOnlinePlayerList(final CommandSender sender, final Map<String, String> afkSuffixes) {
		sendOnlinePlayerList(sender, afkSuffixes, null);
	}
	public static final void sendOnlinePlayerList(final CommandSender sender, final Map<String, String> afkSuffixes, String prefix) {
		sendOnlinePlayerList(sender, afkSuffixes, prefix, false);
	}
	public static final void sendOnlinePlayerList(final CommandSender sender, final Map<String, String> afkSuffixes, String prefix, boolean ignoreCanSee) {
		Player[] players = Bukkit.getServer().getOnlinePlayers();
		if (!ignoreCanSee && sender instanceof Player) { // vanish api !!
			Player sP = (Player) sender;
			for (int i = 0; i <players.length; i++) {
				if (!sP.canSee(players[i])) players[i] = null;
			}
		}
		String pr = "";
		if (prefix != null) {
			List<Player> matches = new LinkedList<Player>();
			prefix = prefix.trim().toLowerCase();
			pr = ChatColor.GRAY+"("+prefix+"...)";
			for (Player player : players) {
				if (player == null); // ignore
				else if (player.getName().toLowerCase().startsWith(prefix)) matches.add(player);
			}
			players = new Player[matches.size()];
			matches.toArray(players);
		}
		int sz = 0;
		for (Player player : players) {
			if (player !=null) sz++;
		}
		if (sz == 0) {
			Messaging.sendMessage(sender, ChatColor.YELLOW+"= "+ChatColor.GRAY+"0 "+ChatColor.DARK_GRAY+"players"+pr+ChatColor.YELLOW+" =");
		} else{
			String list = getOnlinePlayersString(players, afkSuffixes);
			String n = "s";
			if (sz == 1) n = "";
			Messaging.sendMessage(sender, ChatColor.YELLOW+"= "+ChatColor.GRAY+sz+ChatColor.DARK_GRAY+" player"+n+pr+ChatColor.YELLOW+" = "+ChatColor.WHITE+list);
		}
	}
	
	// Changed to (additional) UUID use.
	
//	/**
//	 * Attempt to get online players, otherwise get the UUID.!
//	 * @param name
//	 * @return
//	 */
//	public static final boolean hasPermissionLastPlayed(final String name, final String permission) {
//		final Player player = Players.getPlayerExact(name);
//		if (player != null) {
//			return player.hasPermission(permission);
//		}
//		else {
//			try {
//				final UUID id = Players.getUUID(name, true);
//				if (id != null) {
//					return PermissionsEx.getPermissionManager().getUser(id).has(permission);
//				}
//			}
//			catch (Throwable t) {}
//		}
//		// No result = not have permission.
//		return false;
//	}
	
	public static final boolean hasPermission(final UUID id, final String perm) {
		final PermissionUser user = PermissionsEx.getPermissionManager().getUser(id);
		return user.has(perm);
	}
	
	/**
	 * 
	 * @param id
	 * @param name Can be null (pex values are used then).
	 * @return
	 */
	public static final String getDecoratedPlayerName(final UUID id, String name) {
		final PermissionUser user = PermissionsEx.getPermissionManager().getUser(id);
		if (name == null) {
			// TODO: policy... could use TrustCore, and optionally cross check and deny on mismatch, etc.
			name = user.getName();
		}
		return getDecoratedPlayerName(user, name);
	}
	
	public static final String getMildlyDecoratedPlayerName(final Player player) {
		return getMildlyDecoratedPlayerName(player.getUniqueId(), player.getName());
	}
	
	public static final String getMildlyDecoratedPlayerName(final UUID id, final String playerName) {
		String decorated = getDecoratedPlayerName(id, playerName);
		int j = decorated.indexOf('[');
		if ((j!=-1) && (j<Math.max(6, playerName.length()/2))) { // hacky
			int i = decorated.indexOf(']');
			if ((i != -1)&& (i>j)) {
				if (i < decorated.length()-1) decorated = decorated.substring(i+1,decorated.length()).trim();
			}
		}
		return decorated;
	}
	
	public static final String[] findDecoration(final UUID id) {
		return findDecoration(PermissionsEx.getPermissionManager().getUser(id));
	}

}
