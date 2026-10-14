package com.kevshake.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates security-sensitive environment settings before the application context starts.
 */
public class GatewayEnvironmentValidator implements EnvironmentPostProcessor, Ordered {

	private static final int MIN_PASSWORD_LENGTH = 16;

	/** SHA-256 (hex) of sample zonal key committed historically in application-local.yml. */
	private static final String SAMPLE_ZONAL_KEY_SHA256 = sha256Hex("40763BB5B0B910B5CE3297E58967CD2A");

	/** SHA-256 (hex) of sample terminal key committed historically in application-local.yml. */
	private static final String SAMPLE_TERMINAL_KEY_SHA256 = sha256Hex("9E4F7FF1F831F1132CD9B6C740B0134C");

	/** SHA-256 (hex) of the sample bank host literal 192.168.1.100. */
	private static final String SAMPLE_BANK_HOST_SHA256 = sha256Hex("192.168.1.100");

	private static final Set<String> KNOWN_UNSAFE_PASSWORDS = Set.of(
			"change-me-local",
			"change-me",
			"password",
			"admin",
			"gateway-admin",
			"rootpass123!",
			"rootpass123",
			"sa",
			"12345678",
			"1234567890123456",
			"integrationtest-admin-pass-32chars!",
			"integrationtest-h2-pass-32chars!!");

	/** Applies password, H2, and PCI operational secret rules to the environment before refresh. */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		validateAdminPassword(environment);
		validateH2Console(environment);
		validateH2DatasourcePassword(environment);
		validateOperationalSecrets(environment);
	}

	/** Runs early so insecure configuration never reaches auto-configuration. */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	/** Rejects missing, short, placeholder, or blocklisted admin passwords. */
	private static void validateAdminPassword(ConfigurableEnvironment environment) {
		String password = firstNonBlank(
				environment.getProperty("GATEWAY_ADMIN_PASSWORD"),
				environment.getProperty("spring.security.user.password"));
		if (isUnresolvedPlaceholder(password)) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD / spring.security.user.password",
					"Admin password placeholder is unresolved.",
					"Set env GATEWAY_ADMIN_PASSWORD (bound to spring.security.user.password). "
							+ "Also set in: CI GitHub Actions secrets, runtime pod/VM env, and docs/GATEWAY_CONFIGURATION.md. "
							+ "Minimum 16 characters; must not match known unsafe defaults.");
		}
		if (password == null || password.isBlank()) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD / spring.security.user.password",
					"Admin password is not set.",
					"Export GATEWAY_ADMIN_PASSWORD before startup (CI secret, runtime env). "
							+ "Used for HTTP Basic on /h2-console when spring.profiles.active=local only.");
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD",
					"Admin password is shorter than " + MIN_PASSWORD_LENGTH + " characters.",
					"Use a longer GATEWAY_ADMIN_PASSWORD (minimum " + MIN_PASSWORD_LENGTH + " characters).");
		}
		if (KNOWN_UNSAFE_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD",
					"Admin password matches a known unsafe or committed test default.",
					"Generate a unique GATEWAY_ADMIN_PASSWORD (e.g. openssl rand -base64 32) for CI and each environment.");
		}
	}

	/** Allows H2 console only when the sole active profile is {@code local}. */
	private static void validateH2Console(ConfigurableEnvironment environment) {
		boolean enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, Boolean.FALSE);
		if (!enabled) {
			return;
		}
		if (!isLocalOnlyProfile(environment)) {
			throw illegalState(
					"spring.h2.console.enabled",
					"H2 console is enabled while active profiles are not exactly [local].",
					"Keep spring.h2.console.enabled=false in application.yml; enable only via application-local.yml "
							+ "with spring.profiles.active=local (never combine local with prod/uat).");
		}
	}

	/** Ensures the in-memory H2 datasource password is set and not blocklisted. */
	private static void validateH2DatasourcePassword(ConfigurableEnvironment environment) {
		String h2Password = firstNonBlank(
				environment.getProperty("spring.datasource.password"),
				environment.getProperty("H2_DATASOURCE_PASSWORD"));
		if (isUnresolvedPlaceholder(h2Password)) {
			throw illegalState(
					"H2_DATASOURCE_PASSWORD / spring.datasource.password",
					"H2 datasource password placeholder is unresolved.",
					"Set env H2_DATASOURCE_PASSWORD (maps to spring.datasource.password). "
							+ "Also set in: CI secrets, runtime env, and docs/GATEWAY_CONFIGURATION.md.");
		}
		if (h2Password == null || h2Password.isBlank()) {
			throw illegalState(
					"H2_DATASOURCE_PASSWORD",
					"H2 datasource password is not set.",
					"Export H2_DATASOURCE_PASSWORD for the in-memory terminal JPA database.");
		}
		if (KNOWN_UNSAFE_PASSWORDS.contains(h2Password.toLowerCase(Locale.ROOT))) {
			throw illegalState(
					"H2_DATASOURCE_PASSWORD",
					"H2 datasource password matches a known unsafe or committed test default.",
					"Generate a unique H2_DATASOURCE_PASSWORD per environment.");
		}
	}

	/** Blocks sample PCI fingerprints and unset keys/hosts outside local-only profile. */
	private static void validateOperationalSecrets(ConfigurableEnvironment environment) {
		if (isLocalOnlyProfile(environment)) {
			return;
		}
		String zonalKey = firstNonBlank(
				environment.getProperty("GATEWAY_ZONAL_KEY"),
				environment.getProperty("iso8583.security.gateway-zonal-key"));
		String terminalKey = firstNonBlank(
				environment.getProperty("GATEWAY_DEFAULT_TERMINAL_KEY"),
				environment.getProperty("iso8583.security.default-terminal-key"));
		String bankHost = firstNonBlank(
				environment.getProperty("ISO8583_BANK_HOST"),
				environment.getProperty("iso8583.bank.host"));

		if (isBlankOrPlaceholder(zonalKey)) {
			throw illegalState(
					"GATEWAY_ZONAL_KEY / iso8583.security.gateway-zonal-key",
					"Gateway zonal PIN key is not configured.",
					"Set env GATEWAY_ZONAL_KEY (maps to iso8583.security.gateway-zonal-key). "
							+ "Must match: bank/switch HSM key ceremony record and live switch PIN zone. "
							+ "Never use repository sample keys outside spring.profiles.active=local.");
		}
		if (isSampleSecretHash(normalizeHex(zonalKey), SAMPLE_ZONAL_KEY_SHA256)) {
			throw illegalState(
					"GATEWAY_ZONAL_KEY / iso8583.security.gateway-zonal-key",
					"Gateway zonal PIN key matches the repository sample fingerprint.",
					"Load production key from HSM ceremony; align with bank/switch and docs/GATEWAY_CONFIGURATION.md.");
		}

		if (isBlankOrPlaceholder(terminalKey)) {
			throw illegalState(
					"GATEWAY_DEFAULT_TERMINAL_KEY / iso8583.security.default-terminal-key",
					"Default terminal PIN key is not configured.",
					"Set env GATEWAY_DEFAULT_TERMINAL_KEY. Must align with TerminalKey table and terminal/SDK provisioning.");
		}
		if (isSampleSecretHash(normalizeHex(terminalKey), SAMPLE_TERMINAL_KEY_SHA256)) {
			throw illegalState(
					"GATEWAY_DEFAULT_TERMINAL_KEY / iso8583.security.default-terminal-key",
					"Default terminal PIN key matches the repository sample fingerprint.",
					"Provision per-terminal keys in TerminalKey and SDK; do not use sample material outside local profile.");
		}

		if (isBlankOrPlaceholder(bankHost)) {
			throw illegalState(
					"ISO8583_BANK_HOST / iso8583.bank.host",
					"Bank/switch host is not configured.",
					"Set env ISO8583_BANK_HOST (maps to iso8583.bank.host). "
							+ "Must match deploy/21_bank_channel.xml host and bank TLS/mTLS allow-list.");
		}
		if (isSampleSecretHash(bankHost.trim(), SAMPLE_BANK_HOST_SHA256)) {
			throw illegalState(
					"ISO8583_BANK_HOST / iso8583.bank.host",
					"Bank/switch host matches the repository sample fingerprint.",
					"Point iso8583.bank.host at the live switch endpoint; update deploy/21_bank_channel.xml and TLS trust.");
		}
	}

	/** True when exactly one active Spring profile is {@code local}. */
	private static boolean isLocalOnlyProfile(ConfigurableEnvironment environment) {
		String[] profiles = environment.getActiveProfiles();
		if (profiles.length != 1) {
			return false;
		}
		return "local".equals(profiles[0]);
	}

	/** Detects unresolved {@code ${...}} property placeholders. */
	private static boolean isUnresolvedPlaceholder(String value) {
		return value != null && value.startsWith("${") && value.endsWith("}");
	}

	/** True for null, blank, or unresolved placeholder values. */
	private static boolean isBlankOrPlaceholder(String value) {
		return value == null || value.isBlank() || isUnresolvedPlaceholder(value);
	}

	/** Returns the first non-blank string, or null when both are absent. */
	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}

	/** Trims and uppercases hex key material for fingerprint comparison. */
	private static String normalizeHex(String value) {
		return value.trim().toUpperCase(Locale.ROOT);
	}

	/** Compares SHA-256 fingerprints without logging the secret value. */
	private static boolean isSampleSecretHash(String normalizedValue, String sampleSha256Hex) {
		return sampleSha256Hex.equals(sha256Hex(normalizedValue));
	}

	/** Computes lowercase SHA-256 hex for sample fingerprint checks. */
	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash).toLowerCase(Locale.ROOT);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	/** Builds a startup failure message that names the setting but never secret values. */
	private static IllegalStateException illegalState(String setting, String problem, String remediation) {
		return new IllegalStateException("[" + setting + "] " + problem + " " + remediation);
	}
}
