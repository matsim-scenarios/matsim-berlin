package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Lets the scenario read input files from URLs that need a login, such as the VSP shared-svn.
 * <p>
 * MATSim reads any input file from a URL, but opens it without authenticating ({@code IOUtils.getInputStream}
 * calls {@code URL.openStream()}), so a file behind HTTP basic auth fails with 401. The JDK does answer such a
 * challenge if the JVM has a default {@link Authenticator}; this installs one from
 * {@value #USER} and {@value #PASSWORD}, given as environment variables or system properties.
 * <p>
 * Without those the scenario behaves exactly as before: nothing is installed, and public URLs keep working.
 * Encrypted files are a different mechanism - those are public and decrypted with MATSIM_DECRYPTION_PASSWORD.
 */
final class HttpAuthentication {

	static final String USER = "MATSIM_HTTP_USER";
	static final String PASSWORD = "MATSIM_HTTP_PASSWORD";

	private static final Logger log = LogManager.getLogger(HttpAuthentication.class);
	private static boolean installed = false;

	private HttpAuthentication() {
	}

	/**
	 * Install the credentials as the JVM's default authenticator, if they are set. Does nothing on the second call.
	 */
	static synchronized void installFromEnvironment() {
		if (installed)
			return;

		String user = get(USER);
		String password = get(PASSWORD);

		if (user == null || password == null) {
			if (user != null || password != null)
				log.warn("Only one of {} and {} is set; both are needed to authenticate against input URLs.", USER, PASSWORD);
			return;
		}

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				log.info("Authenticating as {} against {} ({})", user, getRequestingHost(), getRequestingPrompt());
				return new PasswordAuthentication(user, password.toCharArray());
			}
		});
		installed = true;
		log.info("Input files from URLs that ask for a login will be read as user {}", user);
	}

	private static String get(String name) {
		String value = System.getProperty(name, System.getenv(name));
		return value == null || value.isBlank() ? null : value;
	}
}
