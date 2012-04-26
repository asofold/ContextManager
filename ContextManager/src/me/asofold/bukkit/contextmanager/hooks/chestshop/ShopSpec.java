package me.asofold.bukkit.contextmanager.hooks.chestshop;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
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
	
	long tsAccess;
	
	public ShopSpec(ItemStack stack, int amount,  double priceBuy, double priceSell){
		update(stack, amount, priceBuy, priceSell);
	}
	
	public void update(ItemStack stack, int amount,  double priceBuy, double priceSell){
		this.stack = stack;
		this.amount = amount;
		this.priceBuy = priceBuy;
		this.priceSell = priceSell;
		tsAccess = System.currentTimeMillis();
	}
	
	public final String toString(){
		final String b;
		String l = "";
		if (priceBuy<0){
			b = "";
		}
		else{
			b = ChatColor.YELLOW+f.format(priceBuy)+ChatColor.GREEN+" B"+ChatColor.GRAY;
		}
		final String s;
		if (priceSell<0){
			s = "";
		}
		else{
			s =  ChatColor.YELLOW+f.format(priceSell)+ChatColor.RED+" S"+ChatColor.GRAY;
			if (priceBuy>=0) l = " : ";
		}
		String d = "";
		int data = 0;
		final String base = Items.getName(stack, true);
		if (stack.getType().isBlock()) data = stack.getData().getData();
		else data = stack.getDurability();
		if (data != 0 && (base.indexOf(':') == -1)) d = ChatColor.GRAY+":"+ChatColor.BLUE+data;
		return ChatColor.AQUA+base+d+ChatColor.GRAY+"["+b+l+s+"]";
	}
}
