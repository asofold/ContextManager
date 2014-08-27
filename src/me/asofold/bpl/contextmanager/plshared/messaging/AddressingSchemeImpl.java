package me.asofold.bpl.contextmanager.plshared.messaging;

import java.util.HashMap;
import java.util.Map;


/**
 * This class is designed to hold some parts of sentences for addressing senders on messages, or players n general.
 * TODO: implement (TODO: even more generic ? )
 * TODO: class-name ? mode-of-address ?
 * @author mc_dev
 *
 */
public class AddressingSchemeImpl implements AddressingScheme {
	Map<String, String> map = new HashMap<String,String>();
	
	
	public AddressingSchemeImpl(){
		
	}
	
	/**
	 * Case sensitive (!).
	 * @param key
	 * @return
	 */
	public String get(String key){
		String out = this.map.get(key);
		if ( out == null) out = "???";
		return out;
	}
	
}
