package me.asofold.bpl.contextmanager.plshared.messaging.json;

/**
 * Simple Json message initializer (no special imports).
 * @author mc_dev
 *
 */
public class JMessage {
	public final String message;
	public final String command;
	public final String hoverText;
	
	public JMessage(String message) {
		this(message, null);
	}
	
	public JMessage(String message, String command) {
		this(message, command, null);
	}
	
	public JMessage(String message, String command, String hoverText) {
		if (message == null) {
			throw new NullPointerException("Message must not be null");
		}
		this.message = message;
		this.command = command;
		this.hoverText = hoverText;
	}
}
