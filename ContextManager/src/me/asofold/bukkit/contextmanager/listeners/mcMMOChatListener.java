package me.asofold.bukkit.contextmanager.listeners;

import me.asofold.bukkit.contextmanager.chat.HistoryElement;
import me.asofold.bukkit.contextmanager.core.CMCore;
import me.asofold.bukkit.contextmanager.core.ContextType;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.gmail.nossr50.events.chat.McMMOPartyChatEvent;

public final class mcMMOChatListener implements Listener {
	private CMCore core;

	public mcMMOChatListener(CMCore core){
		this.core = core;
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	final void onPartyChat(final McMMOPartyChatEvent event){
		if (event.isCancelled()) return;
		core.addToHistory(new HistoryElement(ContextType.PARTY, event.getSender(), null, event.getMessage(), false));
	}
}
