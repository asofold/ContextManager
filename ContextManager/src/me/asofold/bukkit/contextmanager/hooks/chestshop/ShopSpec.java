package me.asofold.bukkit.contextmanager.hooks.chestshop;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.Acrobot.ChestShop.Items.Items;

/**
 * 
 * @author mc_dev
 *
 */
public class ShopSpec {
	private static final DecimalFormat f = new DecimalFormat("#.###");
	ItemStack stack;
	double priceBuy;
	double priceSell;
	int amount;
	
	final List<RegionSpec> regions = new LinkedList<RegionSpec>();
	
	public ShopSpec(ItemStack stack, int amount,  double priceBuy, double priceSell){
		update(stack, amount, priceBuy, priceSell);
	}
	
	public void update(ItemStack stack, int amount,  double priceBuy, double priceSell){
		this.stack = stack;
		this.amount = amount;
		this.priceBuy = priceBuy;
		this.priceSell = priceSell;
	}
	
	public final String toString(){
		final String b;
		String l = "";
		if (priceBuy<0){
			b = "";
		}
		else{
			b = f.format(priceBuy)+" B";
		}
		final String s;
		if (priceSell<0){
			s = "";
		}
		else{
			s = f.format(priceSell)+" S";
			if (priceBuy>=0) l = " : ";
		}
		return Items.getName(stack, true)+"["+b+l+s+"]";
	}
}
