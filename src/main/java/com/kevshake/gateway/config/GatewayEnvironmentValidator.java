package com.kevshake.gateway.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Validates security-sensitive environment settings before the application context starts.
 */
public class GatewayEnvironmentValidator implements EnvironmentPostProcessor, Ordered {

	private static final int MIN_PASSWORD_LENGTH = 16;
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
			"1234567890123456");

	/** Enforces admin password, H2 console, and H2 datasource password rules on the environment. */
	@Override
	/** Supports postProcessEnvironment for the gateway. */
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		validateAdminPassword(environment);
		validateH2Console(environment);
		validateH2DatasourcePassword(environment);
	}

	/** Runs early so insecure configuration never reaches auto-configuration. */
	@Override
	/** Returns the order. */
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	/** Validates AdminPassword before the transaction proceeds. */
	private static void validateAdminPassword(ConfigurableEnvironment environment) {
		String password = firstNonBlank(
				environment.getProperty("GATEWAY_ADMIN_PASSWORD"),
				environment.getProperty("spring.security.user.password"));
		boolean localOnly = isLocalOnlyProfile(environment);
		if (isUnresolvedPlaceholder(password)) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD",
					"Admin password placeholder is unresolved.",
					"Export GATEWAY_ADMIN_PASSWORD in the environment before starting the gateway.");
		}
		if (password == null || password.isBlank()) {
			throw illegalState(
					"GATEWAY_ADMIN_PASSWORD",
					"Admin password is not set.",
					"Set environment variable GATEWAY_ADMIN_PASSWORD (also bound to spring.security.user.password). "
							+ "Minimum length 16; known defaults are rejected. "
							+ "In tests, use @SpringBootTest(properties = \"GATEWAY_ADMIN_PASSWORD=...\").");
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
					"Admin password matches a known unsafe default.",
					"Choose a unique GATEWAY_ADMIN_PASSWORD; embedded defaults are not permitted"
							+ (localOnly ? " even on the local profile." : "."));
		}
	}

	/** Validates H2Console before the transaction proceeds. */
	private static void validateH2Console(ConfigurableEnvironment environment) {
		boolean enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, Boolean.FALSE);
		if (!enabled) {
			return;
		}
		if (!isLocalOnlyProfile(environment)) {
			throw illegalState(
					"spring.h2.console.enabled",
					"H2 console is enabled outside the local-only profile.",
					"Set spring.h2.console.enabled=false in application.yml and enable it only in application-local.yml "
							+ "with spring.profiles.active=local.");
		}
	}

	/** Validates H2DatasourcePassword before the transaction proceeds. */
	private static void validateH2DatasourcePassword(ConfigurableEnvironment environment) {
		String h2Password = firstNonBlank(
				environment.getProperty("spring.datasource.password"),
				environment.getProperty("H2_DATASOURCE_PASSWORD"));
		if (isUnresolvedPlaceholder(h2Password)) {
			throw illegalState(
					"H2_DATASOURCE_PASSWORD",
					"Datasource password placeholder is unresolved.",
					"Export H2_DATASOURCE_PASSWORD before starting the gateway.");
		}
		if (h2Password == null || h2Password.isBlank()) {
			throw illegalState(
					"H2_DATASOURCE_PASSWORD",
					"Spring datasource password is not set.",
					"Set environment variable H2_DATASOURCE_PASSWORD (maps to spring.datasource.password). "
							+ "Required for in-memory H2 used by terminal JPA.");
		}
	}

	/** Reports whether local only profile. */
	private static boolean isLocalOnlyProfile(ConfigurableEnvironment environment) {
		String[] profiles = environment.getActiveProfiles();
		if (profiles.length == 0) {
			return false;
		}
		return profiles.length == 1 && "local".equals(profiles[0]);
	}

	/** Reports whether unresolved placeholder. */
	private static boolean isUnresolvedPlaceholder(String value) {
		return value != null && value.startsWith("${") && value.endsWith("}");
	}

	/** Supports firstNonBlank for the gateway. */
	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}

	/** Supports illegalState for the gateway. */
	private static IllegalStateException illegalState(String setting, String problem, String remediation) {
		return new IllegalStateException("[" + setting + "] " + problem + " " + remediation);
	}
}
