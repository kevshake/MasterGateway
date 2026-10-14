package com.kevshake.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs startup warnings when operational settings are absent (never logs secret values).
 */
@Component
public class GatewayMissingConfigurationWarnings implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GatewayMissingConfigurationWarnings.class);

	private final Environment environment;

	/** Retains the Spring environment for post-start configuration checks. */
	public GatewayMissingConfigurationWarnings(Environment environment) {
		this.environment = environment;
	}

	/** Emits guidance when non-local deployments still rely on unset operational properties. */
	@Override
	public void run(ApplicationArguments args) {
		if (isLocalOnlyProfile()) {
			return;
		}
		warnIfUnset(
				"iso8583.security.gateway-zonal-key",
				"GATEWAY_ZONAL_KEY",
				"bank/switch HSM key ceremony record");
		warnIfUnset(
				"iso8583.security.default-terminal-key",
				"GATEWAY_DEFAULT_TERMINAL_KEY",
				"TerminalKey JPA table and terminal/SDK provisioning");
		warnIfUnset(
				"iso8583.bank.host",
				"ISO8583_BANK_HOST",
				"src/main/resources/deploy/21_bank_channel.xml and bank TLS/mTLS allow-list");
	}

	private void warnIfUnset(String propertyKey, String envVar, String mustMatchElsewhere) {
		String value = environment.getProperty(propertyKey);
		if (value == null || value.isBlank() || value.startsWith("${")) {
			log.warn(
					"[{}] Not configured. Set env {} (property {}). Value must match {}. See docs/GATEWAY_CONFIGURATION.md.",
					propertyKey,
					envVar,
					propertyKey,
					mustMatchElsewhere);
		}
	}

	/** True when exactly one active profile is {@code local} (sample PCI material allowed). */
	private boolean isLocalOnlyProfile() {
		String[] profiles = environment.getActiveProfiles();
		return profiles.length == 1 && "local".equals(profiles[0]);
	}
}
