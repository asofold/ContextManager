package me.asofold.bpl.contextmanager.core;

public enum ContextType {
	/**
	 * Normal global chat.
	 */
	DEFAULT,
	/**
	 * Some named channel.
	 */
	CHANNEL,
	/**
	 * Party chat.
	 */
	PARTY,
	/**
	 * Tell or recipients.
	 */
	PRIVATE,
	/**
	 * All to hear broadcast.
	 */
	BROADCAST,
}
