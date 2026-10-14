package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.kevshake.gateway.Iso8583MasterServerBase1Application;
import com.kevshake.gateway.support.GatewayTestCredentials;

class GatewayApplicationStartupTest {

	/** Sample zonal key hex (application-local.yml only); used to trigger validator rejection in tests. */
	private static final String SAMPLE_ZONAL_KEY_FOR_TEST = "40763BB5B0B910B5CE3297E58967CD2A";

	/** Sample bank host (application-local.yml only); used to trigger validator rejection in tests. */
	private static final String SAMPLE_BANK_HOST_FOR_TEST = "192.168.1.100";

	/** Clears JVM-wide gateway secrets so each builder test controls the environment in isolation. */
	@BeforeEach
	void clearGatewaySystemPropertiesBeforeStartupTest() {
		GatewayTestCredentials.clearManagedSystemProperties();
	}

	/** Restores integration-test secrets for test classes that share this JVM after startup failure tests. */
	@AfterEach
	void restoreIntegrationSecretsAfterStartupTest() {
		GatewayTestCredentials.installEarlyEnvironmentSecrets();
	}

	/** Verifies Spring Boot aborts when a known-unsafe admin password is supplied on the default profile. */
	@Test
	void springApplicationBuilderFailsOnKnownDefaultAdminPassword() {
		IllegalStateException failure = assertValidatorFailure(
				"--GATEWAY_ADMIN_PASSWORD=1234567890123456");
		assertTrue(failure.getMessage().contains("GATEWAY_ADMIN_PASSWORD"));
	}

	/** Verifies Spring Boot aborts when the H2 console is enabled without the local-only profile. */
	@Test
	void springApplicationBuilderFailsWhenH2ConsoleEnabledWithoutLocalProfile() {
		IllegalStateException failure = assertValidatorFailure("--spring.h2.console.enabled=true");
		assertTrue(failure.getMessage().contains("spring.h2.console.enabled"));
	}

	/** Verifies Spring Boot aborts when repository sample zonal key material is used on the default profile. */
	@Test
	void springApplicationBuilderFailsOnSampleZonalKeyOutsideLocalProfile() {
		IllegalStateException failure = assertValidatorFailure(
				"--GATEWAY_ZONAL_KEY=" + SAMPLE_ZONAL_KEY_FOR_TEST);
		assertTrue(failure.getMessage().contains("GATEWAY_ZONAL_KEY"));
	}

	/** Verifies Spring Boot aborts when repository sample bank host is used on the prod profile. */
	@Test
	void springApplicationBuilderFailsOnSampleBankHostOnProdProfile() {
		IllegalStateException failure = assertValidatorFailure(
				"--spring.profiles.active=prod",
				"--ISO8583_BANK_HOST=" + SAMPLE_BANK_HOST_FOR_TEST);
		assertTrue(failure.getMessage().contains("sample fingerprint"));
	}

	/** Verifies Spring Boot aborts when local is combined with another profile while H2 console is enabled. */
	@Test
	void springApplicationBuilderFailsWhenLocalPlusProdWithH2Console() {
		IllegalStateException failure = assertValidatorFailure(
				"--spring.profiles.active=local,prod",
				"--spring.h2.console.enabled=true");
		assertTrue(failure.getMessage().contains("spring.h2.console.enabled"));
	}

	/** Runs the app with baseline command-line secrets plus overrides that must fail validation. */
	private static IllegalStateException assertValidatorFailure(String... overrideArgs) {
		ConfigurableApplicationContext context = null;
		try {
			context = new SpringApplicationBuilder(Iso8583MasterServerBase1Application.class)
					.run(startupArgs(overrideArgs));
			fail("Expected startup to fail validation");
			return null;
		} catch (Throwable thrown) {
			Throwable root = thrown;
			while (root.getCause() != null) {
				root = root.getCause();
			}
			assertInstanceOf(IllegalStateException.class, root);
			return (IllegalStateException) root;
		} finally {
			if (context != null) {
				context.close();
			}
		}
	}

	/** Builds command-line properties with at most one entry per key so CI env cannot mask overrides. */
	private static String[] startupArgs(String... overrideArgs) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("GATEWAY_GENERATED_DEFAULTS_FILE", GatewayTestCredentials.generatedDefaultsFile());
		values.put("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.adminPassword());
		values.put("H2_DATASOURCE_PASSWORD", GatewayTestCredentials.h2DatasourcePassword());
		values.put("GATEWAY_ZONAL_KEY", GatewayTestCredentials.zonalKey());
		values.put("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials.defaultTerminalKey());
		values.put("ISO8583_BANK_HOST", GatewayTestCredentials.bankHost());
		values.put("iso8583.pos.netty-server-enabled", "false");
		values.put("server.port", "0");
		for (String override : overrideArgs) {
			int equals = override.indexOf('=');
			String key = override.substring(2, equals);
			values.put(key, override.substring(equals + 1));
		}
		return values.entrySet().stream()
				.map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
				.toArray(String[]::new);
	}
}
