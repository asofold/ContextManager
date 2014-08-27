package me.asofold.bpl.contextmanager.plshared.items;

import me.asofold.bpl.contextmanager.plshared.Items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;


public class ItemSpec {
	public final int id;
	public final int data;
	final int hash;
	public ItemSpec(final int id, final int data){
		this.id = id;
		this.data = data;
		hash = ((id*4096 + data) * ((data%2==1)?17:31))^id;
	}
	
	public ItemSpec(final Block block) {
		this(block.getTypeId(), block.getData());
	}
	
	public ItemSpec(final ItemStack stack){
		this(stack.getTypeId(), stack.getType().isBlock()?stack.getData().getData():stack.getDurability());
	}
	
	public String toString(){
		return ""+id+":"+data;
	}
	
	public String name(){
		Material mat = Material.getMaterial(id);
		if ( mat == null) return toString();
		return mat.name()+":"+data;
	}
	
	public String shortestName(){
		ItemSpec spec = ItemSpec.match(""+id); // LOL
		Material mat = Material.getMaterial(id);
		if ( mat == null) return name();
		else if ( spec.data == data) return mat.name();
		else return name();
	}
	
	/**
	 * Match from a user input, like "potion:6428"
	 * @param input
	 * @return
	 */
	public static ItemSpec match(String input){
		int[] spec = Items.matchItem(input);
		if (spec == null) return null;
		Material mat = Material.getMaterial(spec[0]);
		if ( mat == null ) return null;
		return new ItemSpec(spec[0], spec[1]);
	}
	
	public static ItemStack getItem(String input, int amount){
		ItemSpec spec = match(input);
		if ( spec == null) return null;
		return spec.getItem(amount);
	}
	
	public ItemStack getItem(){
		return getItem(1);
	}
	
	public ItemStack getItem(int amount){
		if (amount <= 0 ) amount = 1; // TODO
		Material mat = Material.getMaterial(id);
		if ( mat == null ) return null;
		return new ItemStack(id, amount, (byte) data );
//		ItemStack out = new ItemStack(id, amount);
//		// TODO: maybe consider maxDurability ( mitght be done in matchItem though).
//		out.setDurability((short) data);
//		return out;
	}
	
	@Override
	public final int hashCode(){
		return hash;
	}
	
	@Override
	public final boolean equals(final Object obj){
		if ( obj instanceof ItemSpec){
			final ItemSpec other = (ItemSpec) obj;
			return (id == other.id) && (data==other.data);
		} else return false;
	}
}
