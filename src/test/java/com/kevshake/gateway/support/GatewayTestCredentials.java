package com.kevshake.gateway.support;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Generates non-committed secrets for integration tests (env overrides supported in CI).
 */
public final class GatewayTestCredentials {

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final String INTEGRATION_H2_JDBC_URL = "jdbc:h2:mem:gateway-it-"
			+ java.util.UUID.randomUUID()
			+ ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";

	private static volatile String cachedAdminPassword;

	private static volatile String cachedH2Password;

	private static volatile String cachedZonalKey;

	private static volatile String cachedTerminalKey;

	private static volatile String cachedBankHost;

	private GatewayTestCredentials() {
	}

	/**
	 * Publishes secrets as system properties so {@link com.kevshake.gateway.config.GatewayEnvironmentValidator}
	 * sees them before {@code @DynamicPropertySource} runs.
	 */
	public static void installEarlyEnvironmentSecrets() {
		setSystemPropertyIfAbsent("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials::adminPassword);
		setSystemPropertyIfAbsent("H2_DATASOURCE_PASSWORD", GatewayTestCredentials::h2DatasourcePassword);
		setSystemPropertyIfAbsent("GATEWAY_ZONAL_KEY", GatewayTestCredentials::zonalKey);
		setSystemPropertyIfAbsent("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials::defaultTerminalKey);
		setSystemPropertyIfAbsent("ISO8583_BANK_HOST", GatewayTestCredentials::bankHost);
	}

	private static void setSystemPropertyIfAbsent(String key, java.util.function.Supplier<String> supplier) {
		if (System.getProperty(key) == null || System.getProperty(key).isBlank()) {
			String fromEnv = System.getenv(key);
			if (fromEnv != null && !fromEnv.isBlank()) {
				System.setProperty(key, fromEnv);
			} else {
				System.setProperty(key, supplier.get());
			}
		}
	}

	/** Registers required gateway secrets and non-sample operational properties for integration tests. */
	public static void registerIntegrationProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> INTEGRATION_H2_JDBC_URL);
		registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
		registry.add("spring.datasource.username", () -> "sa");
		registry.add("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials::adminPassword);
		registry.add("spring.security.user.password", GatewayTestCredentials::adminPassword);
		registry.add("H2_DATASOURCE_PASSWORD", GatewayTestCredentials::h2DatasourcePassword);
		registry.add("spring.datasource.password", GatewayTestCredentials::h2DatasourcePassword);
		registry.add("GATEWAY_ZONAL_KEY", GatewayTestCredentials::zonalKey);
		registry.add("iso8583.security.gateway-zonal-key", GatewayTestCredentials::zonalKey);
		registry.add("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials::defaultTerminalKey);
		registry.add("iso8583.security.default-terminal-key", GatewayTestCredentials::defaultTerminalKey);
		registry.add("ISO8583_BANK_HOST", GatewayTestCredentials::bankHost);
		registry.add("iso8583.bank.host", GatewayTestCredentials::bankHost);
		registry.add("iso8583.pos.netty-server-enabled", () -> "false");
	}

	/** Resolves the admin password from the environment or generates an ephemeral test value. */
	public static String adminPassword() {
		return cachedOrGenerate("GATEWAY_ADMIN_PASSWORD", cachedAdminPassword, () -> {
			cachedAdminPassword = fromEnvOrRandom("GATEWAY_ADMIN_PASSWORD", "GW-ADMIN");
			return cachedAdminPassword;
		});
	}

	/** Resolves the H2 datasource password from the environment or generates an ephemeral test value. */
	public static String h2DatasourcePassword() {
		return cachedOrGenerate("H2_DATASOURCE_PASSWORD", cachedH2Password, () -> {
			cachedH2Password = fromEnvOrRandom("H2_DATASOURCE_PASSWORD", "GW-H2");
			return cachedH2Password;
		});
	}

	/** Supplies a non-sample zonal key hex string for non-local integration tests. */
	public static String zonalKey() {
		return cachedOrGenerate("GATEWAY_ZONAL_KEY", cachedZonalKey, () -> {
			cachedZonalKey = fromEnvOrHex("GATEWAY_ZONAL_KEY", 32);
			return cachedZonalKey;
		});
	}

	/** Supplies a non-sample default terminal key hex string for non-local integration tests. */
	public static String defaultTerminalKey() {
		return cachedOrGenerate("GATEWAY_DEFAULT_TERMINAL_KEY", cachedTerminalKey, () -> {
			cachedTerminalKey = fromEnvOrHex("GATEWAY_DEFAULT_TERMINAL_KEY", 32);
			return cachedTerminalKey;
		});
	}

	/** Supplies a non-sample bank host for non-local integration tests. */
	public static String bankHost() {
		return cachedOrGenerate("ISO8583_BANK_HOST", cachedBankHost, () -> {
			String fromEnv = System.getenv("ISO8583_BANK_HOST");
			if (fromEnv != null && !fromEnv.isBlank()) {
				cachedBankHost = fromEnv;
			} else {
				cachedBankHost = "10.62." + RANDOM.nextInt(256) + "." + (1 + RANDOM.nextInt(250));
			}
			return cachedBankHost;
		});
	}

	private static String cachedOrGenerate(String envName, String cached, java.util.function.Supplier<String> generator) {
		String fromEnv = System.getenv(envName);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		if (cached != null && !cached.isBlank()) {
			return cached;
		}
		return generator.get();
	}

	private static String fromEnvOrRandom(String envName, String prefix) {
		String fromEnv = System.getenv(envName);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		byte[] bytes = new byte[24];
		RANDOM.nextBytes(bytes);
		return prefix + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String fromEnvOrHex(String envName, int hexChars) {
		String fromEnv = System.getenv(envName);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		StringBuilder sb = new StringBuilder(hexChars);
		for (int i = 0; i < hexChars; i++) {
			sb.append(Integer.toHexString(RANDOM.nextInt(16)).toUpperCase());
		}
		return sb.toString();
	}
}
