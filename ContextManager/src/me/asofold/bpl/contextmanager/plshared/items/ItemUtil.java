package me.asofold.bpl.contextmanager.plshared.items;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {

	/**
	 * Auxiliary method for manipulating enchantments.
	 * @param player
	 * @param args
	 * @return
	 */
	public static boolean onCmdEnchant(Player player, String[] args) {
		ItemStack stack = player.getItemInHand();
		Material mat = stack.getType();
		if ( (mat == null) || (mat == Material.AIR)){
			player.sendMessage("LOL");
			return false;
		}
		Map<Enchantment, Integer> enchs = stack.getEnchantments();
		if ( args.length == 0){
			// show info (general)
			player.sendMessage("/ench | /ench * | /ench + | /ench - | /ench <name> ... <name>:<level>...");
			String out = "";
			for (Enchantment e : Enchantment.values()){
				out += " "+e.getName() ;
			}
			player.sendMessage("All enchantment names: "+out);
			return true;
		} if ( (args.length == 1) && (args[0].equals("*"))){
			// show present enchantments
			String out = "";
			for (Enchantment e : enchs.keySet()){
				out += " "+e.getName() +":"+enchs.get(e);
			}
			player.sendMessage("Enchantments present: "+out);
			return true;
		}  if ( (args.length == 1) && (args[0].equals("-"))){
			// remove all
			String out = "";
			List <Enchantment> rem = new LinkedList<Enchantment>();
			rem.addAll(enchs.keySet());
			for (Enchantment e : rem){
				out += " "+e.getName() +":"+enchs.get(e);
				stack.removeEnchantment(e);
			}
			
			player.sendMessage("Enchantments removed: "+out);
			player.setItemInHand(stack);
			return true;
		}else if ( (args.length == 1) && args[0].equalsIgnoreCase("+")){
			// raise 
			Map<Enchantment, Integer> newEnchs = new HashMap<Enchantment, Integer>();
			String changes = "";
			for ( Enchantment e : enchs.keySet()){
				int level = enchs.get(e);
				int max = e.getMaxLevel();
				if ( level < max){
					newEnchs.put(e, max);
					changes += e.getName()+"->"+max;
				} else{
					newEnchs.put(e,  level);
				}
			}
			stack.addEnchantments(newEnchs);
			player.setItemInHand(stack);
			player.sendMessage("Changes: "+changes);
			return true;
		} else{
			// add individual enchantments
			String done = "";
			String illegal = "";
			for (String arg : args){
				String eName = arg;
				int level = 0;
				if ( arg.indexOf(":")!=-1){
					String[] sp = arg.split(":", 2);
					eName = sp[0];
					try{
						level = Integer.parseInt(sp[1]);
					} catch ( NumberFormatException w){
						player.sendMessage("Bad number on enchantment: "+arg);
						continue;
					}
				}
				Enchantment e = Enchantment.getByName(eName.toUpperCase());
				if ( e == null){
					player.sendMessage("Bad enchantment name: "+arg);
					continue;
				}
				int max = e.getMaxLevel();
				if ( (level == 0) || (level >max)) level = max;
				else if (level <0 ) {
					player. sendMessage("Bad level: "+arg);
					continue;
				}
				try{
					stack.addEnchantment(e, level);
					done += " "+e.getName()+":"+level;
				} catch ( IllegalArgumentException ex){
					illegal += " "+e.getName();
				}
			}
			player.setItemInHand(stack);
			if ( !done.isEmpty()) player.sendMessage("Done: "+done);
			if ( !illegal.isEmpty()) player.sendMessage("Impossible: "+illegal);
			return true;
		}
	}

}
