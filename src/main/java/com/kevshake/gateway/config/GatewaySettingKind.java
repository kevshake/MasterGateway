package com.kevshake.gateway.config;

/**
 * Gateway settings that may receive install-time generated defaults when unset at startup.
 */
enum GatewaySettingKind {

	ADMIN_PASSWORD(
			"GATEWAY_ADMIN_PASSWORD",
			"spring.security.user.password",
			"Pod/VM secret, CI job env (openssl rand per run in .github/workflows/ci.yml)",
			"export GATEWAY_ADMIN_PASSWORD=$(openssl rand -base64 32) before start"),

	H2_DATASOURCE_PASSWORD(
			"H2_DATASOURCE_PASSWORD",
			"spring.datasource.password",
			"Pod/VM secret, CI job env (openssl rand per run)",
			"export H2_DATASOURCE_PASSWORD=$(openssl rand -base64 32) before start"),

	ZONAL_PIN_KEY(
			"GATEWAY_ZONAL_KEY",
			"iso8583.security.gateway-zonal-key",
			"Bank/switch HSM key ceremony record, live switch PIN zone",
			"Inject production zonal key from HSM; align with bank/switch ceremony"),

	DEFAULT_TERMINAL_PIN_KEY(
			"GATEWAY_DEFAULT_TERMINAL_KEY",
			"iso8583.security.default-terminal-key",
			"TerminalKey JPA table, terminal/SDK provisioning",
			"Provision fleet keys in TerminalKey and terminal SDK"),

	BANK_HOST(
			"ISO8583_BANK_HOST",
			"iso8583.bank.host",
			"src/main/resources/deploy/21_bank_channel.xml, bank TLS/mTLS allow-list",
			"export ISO8583_BANK_HOST=<switch-hostname-or-ip> matching deploy channel XML");

	private final String environmentVariable;

	private final String configurationProperty;

	private final String mustMatchElsewhere;

	private final String howToSet;

	GatewaySettingKind(
			String environmentVariable,
			String configurationProperty,
			String mustMatchElsewhere,
			String howToSet) {
		this.environmentVariable = environmentVariable;
		this.configurationProperty = configurationProperty;
		this.mustMatchElsewhere = mustMatchElsewhere;
		this.howToSet = howToSet;
	}

	/** Environment variable operators set for this value. */
	String environmentVariable() {
		return environmentVariable;
	}

	/** Spring Boot property key bound from YAML or env. */
	String configurationProperty() {
		return configurationProperty;
	}

	/** Other systems where the same logical value must match. */
	String mustMatchElsewhere() {
		return mustMatchElsewhere;
	}

	/** Operator instructions to replace a generated default. */
	String howToSet() {
		return howToSet;
	}
}
