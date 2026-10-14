package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import com.kevshake.gateway.Iso8583MasterServerBase1Application;

class GatewayGeneratedDefaultsTest {

	@TempDir
	Path tempDir;

	/** Generates double-length TDES hex keys with odd parity (32 hex characters). */
	@Test
	void tdesKeyMaterialIsDoubleLengthHex() {
		String key = GatewayTdesKeyMaterialGenerator.generateDoubleLengthTdesHexKey();
		assertEquals(32, key.length());
		assertTrue(key.matches("[0-9A-F]{32}"));
	}

	/** Persists generated secrets to the configured file and reuses them on the next bootstrap. */
	@Test
	void persistsAndReusesGeneratedSecrets() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		MockEnvironment firstPass = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(firstPass, new SpringApplicationBuilder().build());
		assertTrue(Files.exists(defaultsFile));
		String adminFirst = firstPass.getProperty("GATEWAY_ADMIN_PASSWORD");
		assertNotNull(adminFirst);

		MockEnvironment secondPass = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(secondPass, new SpringApplicationBuilder().build());
		assertEquals(adminFirst, secondPass.getProperty("GATEWAY_ADMIN_PASSWORD"));
	}

	/** Uses loopback for bank host when ISO8583_BANK_HOST is unset. */
	@Test
	void defaultsBankHostToLoopback() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		MockEnvironment environment = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(environment, new SpringApplicationBuilder().build());
		assertEquals(GatewayGeneratedDefaultsBootstrap.DEFAULT_BANK_HOST, environment.getProperty("ISO8583_BANK_HOST"));
	}

	/** Ensures remediation banners name settings but never embed generated secret values. */
	@Test
	void remediationBannersNeverContainGeneratedSecretValues() {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		MockEnvironment environment = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(environment, new SpringApplicationBuilder().build());
		String admin = environment.getProperty("GATEWAY_ADMIN_PASSWORD");
		assertNotNull(admin);
		assertFalse(GatewayDefaultedSettingsRegistry.current().isEmpty());
		for (GatewayDefaultedSettingsRegistry.GatewayDefaultedSetting setting : GatewayDefaultedSettingsRegistry.current()) {
			assertFalse(setting.remediationBannerLine().contains(admin));
		}
	}

	/** Ensures a second JVM-style restart reads the same persisted admin password from disk. */
	@Test
	void springApplicationReusesPersistedDefaultsFile() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		ConfigurableApplicationContext first = new SpringApplicationBuilder(Iso8583MasterServerBase1Application.class)
				.run(forceGeneratedDefaultsArgs(defaultsFile));
		String adminFirst = first.getEnvironment().getProperty("GATEWAY_ADMIN_PASSWORD");
		first.close();

		Properties onDisk = new Properties();
		try (var input = Files.newInputStream(defaultsFile)) {
			onDisk.load(input);
		}
		assertEquals(adminFirst, onDisk.getProperty("GATEWAY_ADMIN_PASSWORD"));

		ConfigurableApplicationContext second = new SpringApplicationBuilder(Iso8583MasterServerBase1Application.class)
				.run(forceGeneratedDefaultsArgs(defaultsFile));
		try {
			assertEquals(adminFirst, second.getEnvironment().getProperty("GATEWAY_ADMIN_PASSWORD"));
			assertNotEquals("192.168.1.100", second.getEnvironment().getProperty("ISO8583_BANK_HOST"));
		} finally {
			second.close();
		}
	}

	/** Builds an environment with unset placeholders matching application.yml. */
	private static MockEnvironment emptyEnvironment(Path defaultsFile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("GATEWAY_GENERATED_DEFAULTS_FILE", defaultsFile.toString());
		environment.setProperty("spring.h2.console.enabled", "false");
		return environment;
	}

	/** Command-line args that force missing secrets even when the test JVM inherits CI env vars. */
	private static String[] forceGeneratedDefaultsArgs(Path defaultsFile) {
		return new String[] {
				"--GATEWAY_GENERATED_DEFAULTS_FILE=" + defaultsFile,
				"--GATEWAY_ADMIN_PASSWORD=",
				"--H2_DATASOURCE_PASSWORD=",
				"--GATEWAY_ZONAL_KEY=",
				"--GATEWAY_DEFAULT_TERMINAL_KEY=",
				"--ISO8583_BANK_HOST=",
				"--spring.datasource.url=jdbc:h2:mem:generated-defaults;DB_CLOSE_DELAY=-1;MODE=MySQL",
				"--spring.datasource.driver-class-name=org.h2.Driver",
				"--spring.datasource.username=sa",
				"--server.port=0",
				"--iso8583.pos.netty-server-enabled=false"
		};
	}
}
