package me.asofold.bpl.contextmanager.plshared;

import java.util.HashMap;
import java.util.Map;

import me.asofold.bpl.contextmanager.plshared.items.ItemPresets;
import me.asofold.bpl.contextmanager.plshared.items.ItemSpec;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class Items {
	
	static Map<String, String[]> itemAliasMap = new HashMap<String, String[]>();
	static{
		// preset item aliases:
		for (String[] aliases : ItemPresets.itemAliases){
			for (String alias: aliases){
				Items.itemAliasMap.put(alias, aliases);
			}
		}
	}
	
	/**
	 * @depreated: use static{}
	 */
	public static void init(){
		
	}
	


	public static String getItemSignDisplayName(int itemId, byte dataValue) {
		return getItemSignDisplayName(itemId, (int) dataValue );
	}
	
	/**
	 * Try to find  valid item-id and data-value for given string.
	 * 
	 * - match against 
	 * - Parse for a number or number1:number2, in case of simple number return [number,0].
	 * 
	 * returns null on failure.
	 * @param arg
	 * @return
	 */
	public static int[] matchItem(String arg){
		int[] res =  unsafeMatchItem(arg);
		if ( res == null) return null;
		// TODO: check res
		Material mat = Material.getMaterial(res[0]);
		if ( mat == null) return null;
		if ( res.length == 1){
			ItemStack stack = new ItemStack(mat);
			res = new int[]{res[0], 0};
			if ( mat.isBlock()) res[1] = 0;
			else res[1] = stack.getDurability();
			return res;
		}
		int dam = res[1]; // TODO: adjust dam to standard for block if not given (needs refacturing unsafe...)
		if (mat.isBlock()){
			if ( (dam<0 || (dam>127))) return null;
			// TODO: max value ?
		} else{
			try{
				new Integer(res[1]).shortValue();
				// if ( damage > mat.getMaxDurability()) return null;
			} catch ( Throwable t){
				return null;
			}
		}
		return res;
	}
	
	/**
	 * Return array of length 1 or 2 !
	 * @param arg
	 * @return
	 */
	public static int[] unsafeMatchItem(String arg){
		if ( arg == null) return null;
		arg = arg.trim().toLowerCase();
		arg = arg.replaceAll(" ", "_");
		arg = arg.replaceAll("-", "_");
		
		if (itemAliasMap.containsKey(arg)){
			String[] aliases = itemAliasMap.get(arg);
			arg = aliases[aliases.length-1];
		}

		
		// TODO: do more checks here/below (valid number values !!!!) ?
		int number;
		// try for simple number first:
		try{
			number = Integer.parseInt(arg);
			return new int[] {number};
		} catch (NumberFormatException exc){
			
		}
		
		
		// attempt parse separation by ":" or name:
		int index = arg.indexOf(":");
		if (index == -1){
			Material mat = Material.matchMaterial(arg);
			if (mat == null ){
				return null;
			} else{
				return new int[]{ mat.getId()};
			}
		}

		int number1, number2;
		try{
			number1 = Integer.parseInt(arg.substring(0, index));
		} catch (Exception exc){
			Material mat = Material.matchMaterial(arg.substring(0,index).trim());
			if (mat == null ){
				return null;
			} else{
				number1 = mat.getId();
			}
		}
		
		try{
			number2 = Integer.parseInt(arg.substring(index+1, arg.length()));
			return new int[]{number1, number2};
		} catch (Throwable exc){
			return null;
		}
	}
	
	/**
	 * Return a String fit for display on a sign, to specify an item.
	 * @param itemId
	 * @param dataValue
	 * @return
	 */
	public static String getItemSignDisplayName(int itemId, int dataValue){
		String out = "";
		
		// TODO: match etc.
		
		
		
		String ref = ""+itemId+":"+dataValue;
//		String std = Material.getMaterial(itemId).name().toLowerCase();
		
		String[] aliases = null;
		
		String shortName = new ItemSpec(itemId, dataValue).shortestName().toLowerCase();
		if (itemAliasMap.containsKey(ref)){
			aliases = itemAliasMap.get(ref);
			for (String alias : aliases){
				if ( alias.length()<=15) return alias.toUpperCase();
			}
		} else if (shortName != null){
			if (itemAliasMap.containsKey(shortName)){
				aliases = itemAliasMap.get(shortName);
				for ( String alias:aliases){
					if ( alias.length()<=15) return alias.toUpperCase();
				}
			} else if (shortName.length()<=15){
				return shortName.toUpperCase();
			} else{
				out += itemId;
			}
		} 
		out += itemId;
		
		if (dataValue>0) { // TODO
			out += ":"+dataValue;
		} 
		
		return out;
	}


	/**
	 * Safe comparison method.
	 * Relies on stack.getData().getData() though, for blocks.
	 * @param stack
	 * @param id
	 * @param data
	 * @return
	 */
	public static boolean sameStack(ItemStack stack , int id, int data){
		if (id != stack.getTypeId()) return false;
		Material mat = stack.getType();
		if ( mat.isBlock()){
			if ( (data<0)||(data>255)) return false;
			return stack.getData().getData() == (byte)data;
		} else return data == stack.getDurability();
	}
}
