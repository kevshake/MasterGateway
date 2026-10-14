package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.kevshake.gateway.Iso8583MasterServerBase1Application;
import com.kevshake.gateway.support.GatewayTestCredentials;

class GatewayApplicationStartupTest {

	/** Clears JVM-wide gateway secrets so each builder test controls the environment in isolation. */
	@BeforeEach
	void clearGatewaySystemPropertiesBeforeStartupTest() {
		for (String key : List.of(
				"GATEWAY_ADMIN_PASSWORD",
				"H2_DATASOURCE_PASSWORD",
				"GATEWAY_ZONAL_KEY",
				"GATEWAY_DEFAULT_TERMINAL_KEY",
				"ISO8583_BANK_HOST")) {
			System.clearProperty(key);
		}
	}

	/** Restores integration-test secrets for test classes that share this JVM after startup failure tests. */
	@AfterEach
	void restoreIntegrationSecretsAfterStartupTest() {
		GatewayTestCredentials.installEarlyEnvironmentSecrets();
	}

	/** Verifies Spring Boot aborts when a known-unsafe admin password is supplied on the default profile. */
	@Test
	void springApplicationBuilderFailsOnKnownDefaultAdminPassword() {
		Throwable error = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(
				Iso8583MasterServerBase1Application.class)
				.properties(baseProperties())
				.properties("GATEWAY_ADMIN_PASSWORD=1234567890123456")
				.run());
		assertTrue(rootMessage(error).contains("GATEWAY_ADMIN_PASSWORD"));
	}

	/** Verifies Spring Boot aborts when the H2 console is enabled without the local-only profile. */
	@Test
	void springApplicationBuilderFailsWhenH2ConsoleEnabledWithoutLocalProfile() {
		Throwable error = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(
				Iso8583MasterServerBase1Application.class)
				.properties(baseProperties())
				.properties("spring.h2.console.enabled=true")
				.run());
		assertTrue(rootMessage(error).contains("spring.h2.console.enabled"));
	}

	/** Verifies Spring Boot aborts when repository sample zonal key material is used on the default profile. */
	@Test
	void springApplicationBuilderFailsOnSampleZonalKeyOutsideLocalProfile() {
		Throwable error = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(
				Iso8583MasterServerBase1Application.class)
				.properties(baseProperties())
				.properties("GATEWAY_ZONAL_KEY=40763BB5B0B910B5CE3297E58967CD2A")
				.run());
		assertTrue(rootMessage(error).contains("GATEWAY_ZONAL_KEY"));
	}

	/** Verifies Spring Boot aborts when repository sample bank host is used on the prod profile. */
	@Test
	void springApplicationBuilderFailsOnSampleBankHostOnProdProfile() {
		Throwable error = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(
				Iso8583MasterServerBase1Application.class)
				.profiles("prod")
				.properties(baseProperties())
				.properties("ISO8583_BANK_HOST=192.168.1.100")
				.run());
		assertTrue(rootMessage(error).contains("sample fingerprint"));
	}

	/** Verifies Spring Boot aborts when local is combined with another profile while H2 console is enabled. */
	@Test
	void springApplicationBuilderFailsWhenLocalPlusProdWithH2Console() {
		Throwable error = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(
				Iso8583MasterServerBase1Application.class)
				.profiles("local", "prod")
				.properties(baseProperties())
				.properties("spring.h2.console.enabled=true")
				.run());
		assertTrue(rootMessage(error).contains("spring.h2.console.enabled"));
	}

	/** Supplies non-sample operational secrets so failure tests isolate the property under test. */
	private static java.util.Properties baseProperties() {
		java.util.Properties properties = new java.util.Properties();
		properties.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.adminPassword());
		properties.setProperty("H2_DATASOURCE_PASSWORD", GatewayTestCredentials.h2DatasourcePassword());
		properties.setProperty("GATEWAY_ZONAL_KEY", GatewayTestCredentials.zonalKey());
		properties.setProperty("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials.defaultTerminalKey());
		properties.setProperty("ISO8583_BANK_HOST", GatewayTestCredentials.bankHost());
		properties.setProperty("iso8583.pos.netty-server-enabled", "false");
		return properties;
	}

	/** Walks to the root cause so Spring Boot wrapper exceptions still expose validator text. */
	private static String rootMessage(Throwable error) {
		Throwable current = error;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		return current.getMessage() != null ? current.getMessage() : "";
	}
}
