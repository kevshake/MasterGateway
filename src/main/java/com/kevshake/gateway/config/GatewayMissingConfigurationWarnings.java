package com.kevshake.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs startup warnings for sample or missing operational settings (never logs secret values).
 */
@Component
public class GatewayMissingConfigurationWarnings implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GatewayMissingConfigurationWarnings.class);

	private final Environment environment;

	/** Captures the environment for configuration checks after the context is available. */
	public GatewayMissingConfigurationWarnings(Environment environment) {
		this.environment = environment;
	}

	/** Emits structured warnings for placeholder ISO8583 and bank connectivity settings. */
	@Override
	/** Supports run for the gateway. */
	public void run(ApplicationArguments args) {
		warnIfSampleHexKey("iso8583.security.gateway-zonal-key", "ISO8583 gateway zonal PIN key");
		warnIfSampleHexKey("iso8583.security.default-terminal-key", "ISO8583 default terminal PIN key");
		warnIfDefaultBankHost();
		warnIfMissing("GATEWAY_ADMIN_PASSWORD", "HTTP Basic user for local H2 console (spring.security.user.password)");
		warnIfMissing("H2_DATASOURCE_PASSWORD", "In-memory H2 datasource (spring.datasource.password)");
	}

	/** Supports warnIfSampleHexKey for the gateway. */
	private void warnIfSampleHexKey(String property, String label) {
		String value = environment.getProperty(property);
		if (value == null || value.isBlank()) {
			log.warn("[{}] {} is not configured. Set {} in the environment or external config for each deployment tier "
					+ "(UAT/LIVE); never commit production key material.", property, label, property);
			return;
		}
		if (value.equalsIgnoreCase("40763BB5B0B910B5CE3297E58967CD2A")
				|| value.equalsIgnoreCase("9E4F7FF1F831F1132CD9B6C740B0134C")) {
			log.warn("[{}] {} still uses the repository sample value. Replace {} via secrets management before UAT/LIVE; "
					+ "rotate keys in the terminal key store after change.", property, label, property);
		}
	}

	/** Supports warnIfDefaultBankHost for the gateway. */
	private void warnIfDefaultBankHost() {
		String host = environment.getProperty("iso8583.bank.host");
		if ("192.168.1.100".equals(host)) {
			log.warn("[iso8583.bank.host] Bank host is still the sample 192.168.1.100. Set iso8583.bank.host to the "
					+ "switch/bank TLS endpoint in application-{profile}.yml or ISO8583_BANK_HOST env for each environment.");
		}
	}

	/** Supports warnIfMissing for the gateway. */
	private void warnIfMissing(String envName, String purpose) {
		String value = environment.getProperty(envName);
		if (value == null || value.isBlank() || value.startsWith("${")) {
			log.warn("[{}] {} is not set. Export {} in the runtime environment (Kubernetes secret, VM env, or CI vars).",
					envName, purpose, envName);
		}
	}
}
