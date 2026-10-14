package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	/** Verifies odd parity on every byte across many generated TDES keys. */
	@Test
	void tdesKeyMaterialMaintainsOddParityOnEveryByte() {
		for (int i = 0; i < 100; i++) {
			String key = GatewayTdesKeyMaterialGenerator.generateDoubleLengthTdesHexKey();
			assertTrue(GatewayTdesKeyMaterialGenerator.hasOddParityOnAllKeyBytes(key));
		}
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

	/** Applies a generated admin password and records a banner naming the setting when the env var is blank. */
	@Test
	void blankAdminPasswordReceivesGeneratedValueAndBanner() {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		MockEnvironment environment = emptyEnvironment(defaultsFile);
		environment.setProperty("GATEWAY_ADMIN_PASSWORD", " ");
		environment.setProperty("H2_DATASOURCE_PASSWORD", GatewayTestCredentialsHelper.strongPassword());
		environment.setProperty("GATEWAY_ZONAL_KEY", GatewayTestCredentialsHelper.tdesKey());
		environment.setProperty("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentialsHelper.tdesKey());
		environment.setProperty("ISO8583_BANK_HOST", GatewayGeneratedDefaultsBootstrap.DEFAULT_BANK_HOST);
		new GatewayEnvironmentValidator().postProcessEnvironment(environment, new SpringApplicationBuilder().build());
		String admin = environment.getProperty("GATEWAY_ADMIN_PASSWORD");
		assertNotNull(admin);
		assertTrue(admin.length() >= 16);
		boolean namesAdmin = GatewayDefaultedSettingsSupport.readDefaultedSettings(environment).stream()
				.anyMatch(setting -> "GATEWAY_ADMIN_PASSWORD".equals(setting.kind().environmentVariable()));
		assertTrue(namesAdmin);
	}

	/** Ensures remediation banners name settings but never embed generated secret values. */
	@Test
	void remediationBannersNeverContainGeneratedSecretValues() {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		MockEnvironment environment = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(environment, new SpringApplicationBuilder().build());
		String admin = environment.getProperty("GATEWAY_ADMIN_PASSWORD");
		assertNotNull(admin);
		assertFalse(GatewayDefaultedSettingsSupport.readDefaultedSettings(environment).isEmpty());
		for (GatewayDefaultedSetting setting : GatewayDefaultedSettingsSupport.readDefaultedSettings(environment)) {
			assertFalse(setting.remediationBannerLine().contains(admin));
		}
	}

	/** Ensures persisted files and directories are owner-only on POSIX filesystems. */
	@Test
	void persistedFileAndDirectoryAreOwnerOnlyOnPosix() throws Exception {
		assumeTrue(isPosixFilesystem(tempDir));
		Path defaultsFile = tempDir.resolve("nested/generated-defaults.properties");
		MockEnvironment environment = emptyEnvironment(defaultsFile);
		new GatewayEnvironmentValidator().postProcessEnvironment(environment, new SpringApplicationBuilder().build());
		Set<PosixFilePermission> filePerms = Files.getPosixFilePermissions(defaultsFile);
		assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), filePerms);
		Set<PosixFilePermission> dirPerms = Files.getPosixFilePermissions(defaultsFile.getParent());
		assertEquals(
				Set.of(
						PosixFilePermission.OWNER_READ,
						PosixFilePermission.OWNER_WRITE,
						PosixFilePermission.OWNER_EXECUTE),
				dirPerms);
	}

	/** Ensures concurrent bootstraps against the same path converge on identical generated values. */
	@Test
	void concurrentBootstrapUsesIdenticalGeneratedValues() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		Set<GatewaySettingKind> missing = EnumSet.allOf(GatewaySettingKind.class);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<String> first = pool.submit(() -> {
				start.await();
				return GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(defaultsFile, missing)
						.get(GatewaySettingKind.ADMIN_PASSWORD);
			});
			Future<String> second = pool.submit(() -> {
				start.await();
				return GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(defaultsFile, missing)
						.get(GatewaySettingKind.ADMIN_PASSWORD);
			});
			start.countDown();
			assertEquals(first.get(), second.get());
		} finally {
			pool.shutdownNow();
		}
	}

	/** Ensures a second JVM-style restart reads the same persisted admin password from disk. */
	@Test
	void springApplicationReusesPersistedDefaultsFile() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		ConfigurableApplicationContext first = null;
		ConfigurableApplicationContext second = null;
		try {
			first = new SpringApplicationBuilder(Iso8583MasterServerBase1Application.class)
					.run(forceGeneratedDefaultsArgs(defaultsFile));
			String adminFirst = first.getEnvironment().getProperty("GATEWAY_ADMIN_PASSWORD");

			Properties onDisk = new Properties();
			try (var input = Files.newInputStream(defaultsFile)) {
				onDisk.load(input);
			}
			assertEquals(adminFirst, onDisk.getProperty("GATEWAY_ADMIN_PASSWORD"));

			second = new SpringApplicationBuilder(Iso8583MasterServerBase1Application.class)
					.run(forceGeneratedDefaultsArgs(defaultsFile));
			assertEquals(adminFirst, second.getEnvironment().getProperty("GATEWAY_ADMIN_PASSWORD"));
			assertNotEquals("192.168.1.100", second.getEnvironment().getProperty("ISO8583_BANK_HOST"));
		} finally {
			if (first != null) {
				first.close();
			}
			if (second != null) {
				second.close();
			}
		}
	}

	/** Builds an environment with unset secrets for bootstrap testing. */
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

	/** Detects whether the temp directory supports POSIX permission inspection. */
	private static boolean isPosixFilesystem(Path path) {
		try {
			Files.getPosixFilePermissions(path);
			return true;
		} catch (UnsupportedOperationException ex) {
			return false;
		} catch (Exception ex) {
			return false;
		}
	}

	/** Package-local helpers for tests that need non-sample secret shapes without pulling test support types. */
	private static final class GatewayTestCredentialsHelper {

		private GatewayTestCredentialsHelper() {
		}

		static String strongPassword() {
			return GatewayGeneratedDefaultsFileStore.generateStrongPassword();
		}

		static String tdesKey() {
			return GatewayTdesKeyMaterialGenerator.generateDoubleLengthTdesHexKey();
		}
	}
}
