package me.asofold.bpl.contextmanager.plshared.messaging.json;

import me.asofold.bpl.contextmanager.plshared.messaging.json.cb2922.JsonMessageAPICB2922;


/**
 *  Implemtation of Json message API, defaults to a simple message sending one.
 * @author mc_dev
 *
 */
public class JsonMessageFactory {
	
	/**
	 * 
	 * @return null if none available (relay to Messaging).
	 */
	public IJsonMessageAPI getNewAPI() {
		// TODO: Try to return a real one...
		try {
			return new JsonMessageAPICB2922();
		} catch (Throwable t) {}
		return null;
	}
}
