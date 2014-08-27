package me.asofold.bpl.contextmanager.plshared;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.bukkit.Bukkit;

public class Logging {
	public static final void warn(final String msg){
		Bukkit.getServer().getLogger().warning(msg);
	}
	
	public static final void severe(final String msg){
		Bukkit.getServer().getLogger().severe(msg);
	}
	
	public static final void severe(final String msg, final Throwable t){
		severe(msg);
		severe(t);
	}

	public final static void severe(final Throwable t) {
		severe(toString(t));
	}
	
	public static final void warn(final String msg, final Throwable t){
		warn(msg);
		warn(t);
	}

	public final static void warn(final Throwable t) {
		warn(toString(t));
	}

	public static final String toString(final Throwable t) {
		final Writer buf = new StringWriter(500);
		final PrintWriter writer = new PrintWriter(buf);
		t.printStackTrace(writer);
		// TODO: maybe make lines and log one by one.
		return buf.toString();
	}

}
