package me.asofold.bpl.contextmanager.plshared;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import me.asofold.bpl.contextmanager.plshared.items.ItemSpec;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class Inventories {
	
	public static void init(){
		
	}
	
	/**
	 * convenience method to get inventories - checks all orthogonal neighbour blocks for chest (!).
	 * Works for chest and trapped chest but only returns the same kind. No check for attachments ..
	 * @param block
	 * @return
	 */
	public static List<Inventory> getChestInventories(final Block block){
		final List<Inventory> inventories = new LinkedList<Inventory>();
		final Material mat = block.getType();
		switch (mat) {
			case CHEST:
			case TRAPPED_CHEST:
				break;
			default:
				return inventories;
		}
		final List<Block> blocks = new LinkedList<Block>();
		blocks.add(block);
		blocks.addAll(Blocks.getNeighbourBlocks(block, mat));
		for (final Block chest : blocks){
			final BlockState cstate = chest.getState();
			if (cstate instanceof Chest){
				// might use ContainerBlock for better future compatibility (!)
				final Inventory inv = ((Chest) cstate).getInventory();
				if (!inventories.contains(inv)) {
					// Two at most, just in case ...
					inventories.add(inv);
				}
			}
		}
		return inventories;
	}
	
	
	/**
	 * @see hasItems(List<Inventory> inventories, int itemId, byte dataValue, int amount)
	 * (convenience method)
	 * @param inventory
	 * @param material
	 * @param amount
	 * @return
	 */
	public static boolean hasItems( Inventory inventory, int itemId, int dataValue, int amount){
		List<Inventory> inventories = new LinkedList<Inventory>();
		inventories.add(inventory);
		return Inventories.hasItems(inventories,itemId, dataValue, amount);
	}
	
	/**
	 * Check if the inventories have at least the amount specified for the material, if all occurences are added.
	 * TODO: add argument for durability/damage ?
	 * @param inventories
	 * @param material
	 * @param amount
	 * @return
	 */
	public static boolean hasItems(List<Inventory> inventories, int itemId, int dataValue, int amount){
		ItemSpec spec = new ItemSpec(itemId, dataValue);
		ItemStack refStack = spec.getItem();
		if ( refStack == null ) return false;
		int found = 0;
		for ( Inventory inv : inventories ){
			for (ItemStack stack : inv.all(itemId).values()){
				if ( Items.sameStack(stack,itemId,dataValue)){
					found += stack.getAmount();
					if ( found >= amount) return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @see hasSpace(List<Inventory> inventories, int itemId, byte dataValue, int amount)
	 * @param inventory
	 * @param itemId
	 * @param dataValue
	 * @param amount
	 * @return
	 */
	public static boolean hasSpace( Inventory inventory, int itemId, int dataValue, int amount){
		List<Inventory> inventories = new LinkedList<Inventory>();
		inventories.add(inventory);
		return Inventories.hasSpace(inventories,itemId, dataValue, amount);
	}
	
	/**
	 * Check if the items can be added to the inventories (sum of space of all, not individual).
	 * @param inventories
	 * @param itemId
	 * @param dataValue
	 * @param amount
	 * @return
	 */
	public static boolean hasSpace(List<Inventory> inventories, int itemId,int dataValue, int amount) {
		ItemSpec spec = new ItemSpec(itemId, dataValue);
		ItemStack refStack = spec.getItem();
		if ( refStack == null ) return false;
		int space = 0;
		int max = Material.getMaterial(itemId).getMaxStackSize();
		if (max <= 0) max = 1; // assumption (!)
		for (Inventory inv : inventories){
			if ( max > 1 ){
				// go through occupied spaces
				for (ItemStack stack : inv.all(itemId).values()){
					if ( Items.sameStack(stack, itemId, dataValue) ){
						if (stack.getAmount()< max){
							space += max - stack.getAmount();
							if ( space >= amount) return true;
						}
					}
				}
			}
			
			// go through free spaces (air)
			space += Inventories.freeSlots(inv).size()*max;
			if (space >= amount) return true;

		}
		return false;
	}
	
	public static Collection<Integer> freeSlots(Inventory inv){
		LinkedList<Integer> slots = new LinkedList<Integer>();
		for (int slot = 0; slot<inv.getSize(); slot++){
			ItemStack stack = inv.getItem(slot);
			// boolean free = false;
			if (stack == null) slots.add(slot);
			else{
				Material mat = stack.getType();
				if (mat == Material.AIR) slots.add(slot); // NOT SURE
			}
		}
		
		return slots;
	}
	


	
	/**
	 * TODO: change signatiure to int (number removed)
	 * Remove (maximally) amount of items from inventories in total (not individual),
	 * 	this will not perform checks.
	 * @param inventories
	 * @param itemId
	 * @param dataValue
	 * @param amount
	 */
	public static void removeItems(List<Inventory> inventories, int itemId, int dataValue, int amount){
		ItemSpec spec = new ItemSpec(itemId, dataValue);
		ItemStack refStack = spec.getItem();
		if ( refStack == null ) return;
		int left = amount;
		for (Inventory inv : inventories){
			Map<Integer , ? extends ItemStack> map = inv.all(itemId);
			for (int slot : map.keySet()){
				ItemStack stack = map.get(slot);
				if ( Items.sameStack(stack,itemId,dataValue)){
					if ( left >= stack.getAmount()){
						left -= stack.getAmount();
						inv.clear(slot);
						if (left <= 0 ) return;
					} else{
						stack.setAmount(stack.getAmount()-left);
						left = 0; // very correct
						return;
					}
				}
				
			}
		}
	}
	
	/**
	 * Convenience method.
	 * @param inv
	 * @param itemId
	 * @param dataValue
	 * @param amount
	 */
	public static void addItems(Inventory inv, int itemId, int dataValue, int amount){
		List<Inventory> invs = new LinkedList<Inventory>();
		invs.add(inv);
		addItems(invs, itemId, dataValue, amount);
	}
	
/**
 * TODO: change signature to int (number added).
 * This attempts to add the amount to the inventories,
 * 	this will stop if there is not enough space in total(!).
 * @param inventories
 * @param itemId
 * @param dataValue
 * @param amount
 */
	public static void addItems(List<Inventory> inventories, int itemId, int dataValue, int amount){
		ItemSpec spec = new ItemSpec(itemId, dataValue);
		ItemStack refStack = spec.getItem();
		if ( refStack == null ) return;
		int left = amount;
		int max = Material.getMaterial(itemId).getMaxStackSize();
		if (max <= 0) max = 1; // assumption (!), does not make sense for air though
		for (Inventory inv : inventories){
			if ( max>1){
				// fill existing stacks
				Map<Integer , ? extends ItemStack> map = inv.all(itemId);
				for (int slot : map.keySet()){
					ItemStack stack = map.get(slot);
					if ( Items.sameStack(stack, itemId, dataValue)){
						if (stack.getAmount() < max){
							int possible = max - stack.getAmount();
							if (possible < left){
								left -= possible;
								stack.setAmount(max);
							} else{
								stack.setAmount(stack.getAmount()+left);
								left = 0;
								return;
							}
						}
					}
				}
			}
			// fill empty slots
			ItemStack stack;
			for ( int slot: Inventories.freeSlots(inv)){ // more safe would have been iteration over freeSlots(...)
				if (slot<0) return;
				
				//stack.setData(new MaterialData(itemId, dataValue));
				//stack.getData().setData(dataValue);
				int sts ;
				if (left < max){
					sts = left;
					left = 0;
				} else{
					sts = max;
					left -= max;
				}
				stack = spec.getItem(sts);
				inv.addItem(stack);
				if (left <= 0) break;
			}
			
			
		}
	}

	public static void removeItems(Inventory inv, int id, int dataValue, int amount) {
		List<Inventory> invs = new LinkedList<Inventory>();
		invs.add(inv);
		removeItems(invs, id, dataValue, amount);
	}

	public static boolean hasSpace(Inventory inv, ItemStack stack) {
		int data;
		if (stack.getType().isBlock()){
			data = stack.getData().getData();
		} else data = stack.getDurability();
		return hasSpace(inv, stack.getTypeId(), data, stack.getAmount());
	}
	
	public static int stackCount(Inventory inv) {
		int count = 0;
		for ( ItemStack stack : inv.getContents()){
			if ( stack == null) continue;
			if (stack.getTypeId() == 0) continue;
			count++;
		}
		return count;
	}

	public static int stackCount(List<Inventory> inventories) {
		int count = 0;
		for ( Inventory inv : inventories){
			count += stackCount(inv);
		}
		return count;
	}

	/**
	 * 	Add stacks of 64 if possible or one if desired.
	 * @param player
	 * @param itemDescr
	 * @param number
	 * @return
	 */
	public static boolean inv(Player player, String itemDescr, int number){
		if ( itemDescr.equalsIgnoreCase("clear")){
			player.getInventory().clear();
			return true;
		}
		int[] itemSpec = Items.matchItem(itemDescr);
		if ( itemSpec == null ) return false;
		if (number <= 0) return false;
		if ( number > 3600 ) number = 3600; // configurable !
		int done = 0;
		int mId = itemSpec[0];
		int dv = itemSpec[1];
		//if ( (dv<0) || (dv>127))  return false;
		Material mat = Material.getMaterial(mId);
		int max = 64;// Material.getMaterial(mId).getMaxStackSize();
		if (max<=0) max = 1;
		Inventory inv = player.getInventory(); // WOW
		boolean isBlock = mat.isBlock();
		try{
			while (done < number){
				if (number -done < max) {
					max = number-done;
					if ( max <= 0) return true;
				}
				
				ItemStack stack; 
				if ( isBlock){
					stack = new ItemStack(mId, max, (short) dv);
				} else{
					stack = new ItemStack(mId, max);
					stack.setDurability((short) dv);
				}
				HashMap<Integer,ItemStack> res = inv.addItem(stack);
				if ( res != null ){
					if ( !res.isEmpty() ) return true;
				}
				done += max;
			}
		} catch (Throwable t){
			return false;
		}
		return true;
	}

	
	/**
	 * Number of free slots over all inventories.
	 * @param inventories
	 * @return
	 */
	public static int freeSlots(List<Inventory> inventories) {
		int count = 0;
		for (final Inventory inv : inventories) {
			count += freeSlots(inv).size();
		}
		return count;
	}
	
	/**
	 * Item count for the kind (id + durability).
	 * @param inventories
	 * @param ref
	 * @return
	 */
	public static int countItems(List<Inventory> inventories, ItemStack ref) {
		int count = 0;
		int id = ref.getTypeId();
		int durability = ref.getDurability();
		for (final Inventory inv : inventories) {
			for (ItemStack stack : inv.all(id).values()) {
				if (stack.getDurability() == durability) {
					count += stack.getAmount();
				}
			}
		}
		return count;
	}

	/**
	 * Space for the kind of item (id + durability).
	 * @param inventories
	 * @param ref
	 * @return
	 */
	public static int getSpace(List<Inventory> inventories, ItemStack ref) {
		int maxStackSize = ref.getMaxStackSize();
		int id = ref.getTypeId();
		int durability = ref.getDurability();
		int freeSlots = freeSlots(inventories);
		int space = freeSlots * maxStackSize;
		for (final Inventory inv : inventories) {
			for (ItemStack stack : inv.all(id).values()) {
				if (stack.getDurability() == durability) {
					int has = maxStackSize - stack.getAmount();
					if (has > 0) {
						space += has;
					}
				}
			}
		}
		return space;
	}

}
