package me.asofold.bpl.contextmanager.plshared.messaging.json.cb3026;

import me.asofold.bpl.contextmanager.plshared.messaging.json.IJsonMessageAPI;
import me.asofold.bpl.contextmanager.plshared.messaging.json.JMessage;
import net.minecraft.server.v1_7_R2.ChatClickable;
import net.minecraft.server.v1_7_R2.ChatComponentText;
import net.minecraft.server.v1_7_R2.ChatHoverable;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import net.minecraft.server.v1_7_R2.EnumClickAction;
import net.minecraft.server.v1_7_R2.EnumHoverAction;
import net.minecraft.server.v1_7_R2.IChatBaseComponent;
import net.minecraft.server.v1_7_R2.PacketPlayOutChat;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;

public class JsonMessageAPICB3026 implements IJsonMessageAPI{
	
	private static ChatHoverable getHoverable(String text) {
		IChatBaseComponent c = CraftChatMessage.fromString(ChatColor.stripColor(text))[0];
		return new ChatHoverable(EnumHoverAction.SHOW_TEXT, c);
	}
	
	private IChatBaseComponent modify(IChatBaseComponent component, String command, String hoverText)
	{
		if (command == null && hoverText == null) {
			return component;
		}
    	// Fill in command (!).
		ChatHoverable hover;
		if (hoverText == null) {
			// command != null
			hover = getHoverable(command);
		} else {
			hover = getHoverable(hoverText);
		}
    	if (command != null) {
    		component = component.setChatModifier(component.getChatModifier().setChatClickable(new ChatClickable(EnumClickAction.RUN_COMMAND, command)));
    	} 
    	component = component.setChatModifier(component.getChatModifier().a(hover));
    	return component;
	}


	@Override
	public void sendMessage(Player player, Object... components) {
		EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
		if (entityPlayer.playerConnection == null) {
			return;
		}
		IChatBaseComponent anchor = new ChatComponentText("");
		//List<IChatBaseComponent> all = Arrays.asList(anchor);
		//
		
		for (Object part : components) {
			String message, command, hoverText;
			if (part == null) {
				throw new IllegalArgumentException("Expect String or JMessage, not null");
			} else if (part instanceof String) {
				message = (String) part;
				command = null;
				hoverText = null;
			} else if (part instanceof JMessage) {
				JMessage setup = (JMessage) part;
				message = setup.message;
				command = setup.command;
				hoverText = setup.hoverText;
			} else {
				throw new IllegalArgumentException("Expect String or JMessage, got instead: " + part.getClass().getName());
			}
			for (IChatBaseComponent component : CraftChatMessage.fromString(message)) {
	        	modify(component, command, hoverText);
	        	anchor = anchor.a(component);
	        }
		}
		entityPlayer.playerConnection.sendPacket(new PacketPlayOutChat(anchor));
	}

}
