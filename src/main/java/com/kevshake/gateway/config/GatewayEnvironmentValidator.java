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

	/**
	 * SHA-256 (hex, lowercase) of the sample zonal PIN key documented only in historical local YAML.
	 */
	private static final String SAMPLE_ZONAL_KEY_SHA256 = "77ccd67cbb79ad78efff935e67c237cbe0e219026eb887654ad810c6cfc966e2";

	/**
	 * SHA-256 (hex, lowercase) of the sample default terminal PIN key documented only in historical local YAML.
	 */
	private static final String SAMPLE_TERMINAL_KEY_SHA256 = "de80ce38b4251c2a9ce36261265062e6eae40dd397fa7a66b15abcb0d1fc236e";

	/**
	 * SHA-256 (hex, lowercase) of the sample bank/switch host documented only in historical local YAML.
	 */
	private static final String SAMPLE_BANK_HOST_SHA256 = "2a39f1eedcd9f986327b5e4da842426f4f05b8f16f0ef385639dbec0db70eaae";

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

	/** Rejects weak values, reports every missing setting (fail closed on fleet profiles), generates lab defaults, then validates. */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		rejectUnsafeExplicitValues(environment);
		GatewayMissingConfigurationReporter.reportAndEnforce(environment);
		GatewayGeneratedDefaultsBootstrap.applyMissingDefaults(environment);
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

	/** Fails fast when an operator-set value is weak or matches a known sample fingerprint. */
	private static void rejectUnsafeExplicitValues(ConfigurableEnvironment environment) {
		validateExplicitPassword(environment, GatewaySettingKind.ADMIN_PASSWORD);
		validateExplicitPassword(environment, GatewaySettingKind.H2_DATASOURCE_PASSWORD);
		validateExplicitOperationalSecret(environment, GatewaySettingKind.ZONAL_PIN_KEY, SAMPLE_ZONAL_KEY_SHA256, true);
		validateExplicitOperationalSecret(environment, GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY, SAMPLE_TERMINAL_KEY_SHA256, true);
		validateExplicitOperationalSecret(environment, GatewaySettingKind.BANK_HOST, SAMPLE_BANK_HOST_SHA256, false);
	}

	/** Validates an operator-supplied password before generated defaults are applied. */
	private static void validateExplicitPassword(ConfigurableEnvironment environment, GatewaySettingKind kind) {
		String password = readConfiguredValue(environment, kind);
		if (isUnset(password)) {
			return;
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw illegalState(
					kind.environmentVariable() + " / " + kind.configurationProperty(),
					"Password is shorter than " + MIN_PASSWORD_LENGTH + " characters.",
					kind.howToSet());
		}
		if (KNOWN_UNSAFE_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
			throw illegalState(
					kind.environmentVariable(),
					"Password matches a known unsafe or committed test default.",
					kind.howToSet());
		}
	}

	/** Validates an operator-supplied key or host before generated defaults are applied. */
	private static void validateExplicitOperationalSecret(
			ConfigurableEnvironment environment,
			GatewaySettingKind kind,
			String sampleSha256,
			boolean hexKey) {
		String value = readConfiguredValue(environment, kind);
		if (isUnset(value)) {
			return;
		}
		String normalized = hexKey ? normalizeHex(value) : value.trim();
		if (isSampleSecretHash(normalized, sampleSha256)) {
			throw illegalState(
					kind.environmentVariable() + " / " + kind.configurationProperty(),
					"Value matches the repository sample fingerprint.",
					kind.howToSet() + " Must also match: " + kind.mustMatchElsewhere());
		}
	}

	/** Rejects missing, short, placeholder, or blocklisted admin passwords after defaults injection. */
	private static void validateAdminPassword(ConfigurableEnvironment environment) {
		String password = readConfiguredValue(environment, GatewaySettingKind.ADMIN_PASSWORD);
		if (isUnset(password)) {
			throw illegalState(
					GatewaySettingKind.ADMIN_PASSWORD.environmentVariable(),
					"Admin password is still unset after generated defaults.",
					GatewaySettingKind.ADMIN_PASSWORD.howToSet());
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw illegalState(
					GatewaySettingKind.ADMIN_PASSWORD.environmentVariable(),
					"Admin password is shorter than " + MIN_PASSWORD_LENGTH + " characters.",
					GatewaySettingKind.ADMIN_PASSWORD.howToSet());
		}
		if (KNOWN_UNSAFE_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
			throw illegalState(
					GatewaySettingKind.ADMIN_PASSWORD.environmentVariable(),
					"Admin password matches a known unsafe or committed test default.",
					GatewaySettingKind.ADMIN_PASSWORD.howToSet());
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

	/** Ensures the in-memory H2 datasource password is set and not blocklisted after defaults injection. */
	private static void validateH2DatasourcePassword(ConfigurableEnvironment environment) {
		String h2Password = readConfiguredValue(environment, GatewaySettingKind.H2_DATASOURCE_PASSWORD);
		if (isUnset(h2Password)) {
			throw illegalState(
					GatewaySettingKind.H2_DATASOURCE_PASSWORD.environmentVariable(),
					"H2 datasource password is still unset after generated defaults.",
					GatewaySettingKind.H2_DATASOURCE_PASSWORD.howToSet());
		}
		if (KNOWN_UNSAFE_PASSWORDS.contains(h2Password.toLowerCase(Locale.ROOT))) {
			throw illegalState(
					GatewaySettingKind.H2_DATASOURCE_PASSWORD.environmentVariable(),
					"H2 datasource password matches a known unsafe or committed test default.",
					GatewaySettingKind.H2_DATASOURCE_PASSWORD.howToSet());
		}
	}

	/** Ensures operational secrets and bank host are present and not sample fingerprints after defaults injection. */
	private static void validateOperationalSecrets(ConfigurableEnvironment environment) {
		String zonalKey = readConfiguredValue(environment, GatewaySettingKind.ZONAL_PIN_KEY);
		String terminalKey = readConfiguredValue(environment, GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY);
		String bankHost = readConfiguredValue(environment, GatewaySettingKind.BANK_HOST);

		if (isUnset(zonalKey)) {
			throw illegalState(
					GatewaySettingKind.ZONAL_PIN_KEY.environmentVariable(),
					"Gateway zonal PIN key is still unset after generated defaults.",
					GatewaySettingKind.ZONAL_PIN_KEY.howToSet());
		}
		if (isSampleSecretHash(normalizeHex(zonalKey), SAMPLE_ZONAL_KEY_SHA256)) {
			throw illegalState(
					GatewaySettingKind.ZONAL_PIN_KEY.environmentVariable(),
					"Gateway zonal PIN key matches the repository sample fingerprint.",
					GatewaySettingKind.ZONAL_PIN_KEY.howToSet());
		}

		if (isUnset(terminalKey)) {
			throw illegalState(
					GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY.environmentVariable(),
					"Default terminal PIN key is still unset after generated defaults.",
					GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY.howToSet());
		}
		if (isSampleSecretHash(normalizeHex(terminalKey), SAMPLE_TERMINAL_KEY_SHA256)) {
			throw illegalState(
					GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY.environmentVariable(),
					"Default terminal PIN key matches the repository sample fingerprint.",
					GatewaySettingKind.DEFAULT_TERMINAL_PIN_KEY.howToSet());
		}

		if (isUnset(bankHost)) {
			throw illegalState(
					GatewaySettingKind.BANK_HOST.environmentVariable(),
					"Bank/switch host is still unset after generated defaults.",
					GatewaySettingKind.BANK_HOST.howToSet());
		}
		if (isSampleSecretHash(bankHost.trim(), SAMPLE_BANK_HOST_SHA256)) {
			throw illegalState(
					GatewaySettingKind.BANK_HOST.environmentVariable(),
					"Bank/switch host matches the repository sample fingerprint.",
					GatewaySettingKind.BANK_HOST.howToSet());
		}
	}

	/** Reads env or property for a logical gateway setting. */
	static String readConfiguredValue(ConfigurableEnvironment environment, GatewaySettingKind kind) {
		return firstNonBlank(
				environment.getProperty(kind.environmentVariable()),
				environment.getProperty(kind.configurationProperty()));
	}

	/** True when the value is absent or still an unresolved placeholder. */
	static boolean isUnset(String value) {
		return value == null || value.isBlank() || isUnresolvedPlaceholder(value);
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

	/** Computes lowercase SHA-256 hex for runtime fingerprint comparison (never logged). */
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
