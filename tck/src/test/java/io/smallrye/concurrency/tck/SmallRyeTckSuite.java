package io.smallrye.concurrency.tck;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.microprofile.concurrency.tck.TckSuite;

public class SmallRyeTckSuite extends TckSuite {
	@BeforeClass
	public static void log() {
		final ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.FINEST);
		consoleHandler.setFormatter(new SimpleFormatter());

		final Logger app = Logger.getLogger("org.jboss.weld");
		app.setLevel(Level.FINEST);
		app.addHandler(consoleHandler);
	}
}
