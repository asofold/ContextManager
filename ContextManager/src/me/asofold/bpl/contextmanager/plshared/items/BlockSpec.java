package me.asofold.bpl.contextmanager.plshared.items;

import me.asofold.bpl.contextmanager.plshared.Items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;


/** 
 * TODO: maybe inheritance with ItemSpec.
 * @author mc_dev
 *
 */
public class BlockSpec {
	public final int id;
	public final byte data;
	final int hash;
	public BlockSpec(int id, byte data){
		this.id = id;
		this.data = data;
		hash = (id*4096 + data) * ((data%2==1)?17:31);
	}
	
	public BlockSpec(int id, int data){
		this (id, (byte) data);
	}
	
	public BlockSpec(Block block) {
		this(block.getTypeId(), block.getData());
	}
	
	public String toString(){
		return ""+id+":"+data;
	}
	
	/**
	 * Does not check for isBLock.
	 * @return
	 */
	public String shortestName(){
		ItemSpec spec = ItemSpec.match(""+id); // LOL
		Material mat = Material.getMaterial(id); 
		if ( mat == null) return name();
		else if ( spec.data == data) return mat.name();
		else return name();
	}
	
	public String name(){
		Material mat = Material.getMaterial(id);
		if ( mat == null) return toString();
		return mat.name()+":"+data;
	}
	
	/**
	 * Match from a user input, like "wool:3", only accept blocks.
	 * @param input
	 * @return
	 */
	public static BlockSpec match(String input){
		int[] spec = Items.matchItem(input);
		if (spec == null) return null;
		Material mat = Material.getMaterial(spec[0]);
		if ( mat ==null ) return null;
		if (!mat.isBlock()) return null;
		return new BlockSpec(spec[0], (byte) spec[1]);
	}
	
	public static ItemStack getItem(String input, int amount){
		BlockSpec spec = match(input);
		if ( spec == null) return null;
		return spec.getItem(amount);
	}
	
	public ItemStack getItem(){
		return getItem(1);
	}
	public ItemStack getItem(int amount){
		if (amount <= 0 ) amount = 1; // TODO
		return new ItemStack(id,amount, (short) 0, data );
	}
	
	@Override
	public final int hashCode(){
		return hash;
	}
	
	@Override
	public final boolean equals(final Object obj){
		if ( obj instanceof BlockSpec){
			final BlockSpec other = (BlockSpec) obj;
			return (id == other.id) && (data==other.data);
		} else return false;
	}
	
}
