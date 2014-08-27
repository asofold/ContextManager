package me.asofold.bpl.contextmanager.plshared.messaging.json;

import me.asofold.bpl.contextmanager.plshared.messaging.json.cb2922.JsonMessageAPICB2922;
import me.asofold.bpl.contextmanager.plshared.messaging.json.cb3026.JsonMessageAPICB3026;
import me.asofold.bpl.contextmanager.plshared.messaging.json.cb3043.JsonMessageAPICB3043;




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
		try {
			return new JsonMessageAPICB3043();
		} catch (Throwable t) {}
		try {
			return new JsonMessageAPICB3026();
		} catch (Throwable t) {}
		try {
			return new JsonMessageAPICB2922();
		} catch (Throwable t) {}
		return null;
	}
}
