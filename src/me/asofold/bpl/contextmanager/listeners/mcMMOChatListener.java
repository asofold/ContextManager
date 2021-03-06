package me.asofold.bpl.contextmanager.listeners;

import me.asofold.bpl.contextmanager.chat.HistoryElement;
import me.asofold.bpl.contextmanager.core.CMCore;
import me.asofold.bpl.contextmanager.core.ContextType;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.gmail.nossr50.events.chat.McMMOPartyChatEvent;

public final class mcMMOChatListener implements Listener {
	private CMCore core;

	public mcMMOChatListener(CMCore core){
		this.core = core;
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
	final void onPartyChat(final McMMOPartyChatEvent event){
		if (core.checkPartyAnnounce(event.getSender(), event.getMessage())){
			event.setCancelled(true);
			return;
		}
	}	

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	final void onPartyChatMonitor(final McMMOPartyChatEvent event){
		core.addToHistory(new HistoryElement(ContextType.PARTY, event.getSender(), null, event.getMessage(), false));
	}
}
