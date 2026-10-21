package com.kevshake.gateway.config;

import com.kevshake.gateway.support.GatewayTestCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Missing-config registry report: full banner set, fail closed on fleet profiles, lab defaults otherwise. */
class GatewayMissingConfigurationReporterTest {

	@TempDir
	Path tempDir;

	/** Runs the validator EPP capturing stderr; returns the captured text and any thrown exception. */
	private static String[] run(MockEnvironment env) {
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		PrintStream original = System.err;
		System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
		String error = "";
		try {
			new GatewayEnvironmentValidator().postProcessEnvironment(env, new SpringApplication());
		} catch (IllegalStateException ex) {
			error = ex.getMessage();
		} finally {
			System.setErr(original);
		}
		return new String[] {captured.toString(StandardCharsets.UTF_8), error};
	}

	/** Environment with nothing configured except the generated-defaults file path. */
	private MockEnvironment empty(String... profiles) {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles(profiles);
		env.setProperty("spring.h2.console.enabled", "false");
		env.setProperty("GATEWAY_GENERATED_DEFAULTS_FILE", tempDir.resolve("defaults.properties").toString());
		return env;
	}

	/** Every fleet profile refuses startup after listing every missing env var. */
	@Test
	void fleetProfilesFailClosedListingEverySetting() {
		for (String profile : new String[] {"uat", "prod", "production", "live"}) {
			String[] out = run(empty(profile));
			for (GatewaySettingKind kind : GatewaySettingKind.values()) {
				assertTrue(out[1].contains(kind.environmentVariable()), profile + " " + kind);
				assertTrue(out[0].contains(kind.environmentVariable()), profile + " banner " + kind);
			}
			assertTrue(out[0].contains("startup refused"), profile);
		}
	}

	/** Banners name the property, every other place it must match and how to set it, never a value. */
	@Test
	void bannerNamesPropertyPairingAndHowTo() {
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			String line = GatewayMissingConfigurationReporter.banner(kind, true);
			assertTrue(line.contains(kind.configurationProperty()));
			assertTrue(line.contains(kind.mustMatchElsewhere()));
			assertTrue(line.contains(kind.howToSet()));
		}
	}

	/** Lab (default) profile prints the banners, then continues with generated defaults. */
	@Test
	void labProfileReportsAndContinuesWithDefaults() {
		MockEnvironment env = empty();
		String[] out = run(env);
		assertEquals("", out[1]);
		assertTrue(out[0].contains("GATEWAY_ZONAL_KEY"));
		assertTrue(out[0].contains("generated default will be used"));
		assertFalse(GatewayEnvironmentValidator.isUnset(env.getProperty("GATEWAY_ZONAL_KEY")));
	}

	/** A fully configured fleet profile starts with no missing-config banners. */
	@Test
	void fullyConfiguredFleetProfileStarts() {
		MockEnvironment env = empty("prod");
		env.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.adminPassword());
		env.setProperty("H2_DATASOURCE_PASSWORD", GatewayTestCredentials.h2DatasourcePassword());
		env.setProperty("GATEWAY_ZONAL_KEY", GatewayTestCredentials.zonalKey());
		env.setProperty("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials.defaultTerminalKey());
		env.setProperty("ISO8583_BANK_HOST", GatewayTestCredentials.bankHost());
		String[] out = run(env);
		assertEquals("", out[1]);
		assertFalse(out[0].contains("GATEWAY MISSING CONFIGURATION"));
		assertDoesNotThrow(() -> GatewayMissingConfigurationReporter.reportAndEnforce(env));
	}

	/** Weak explicit values still stop startup before any defaults (unchanged behaviour). */
	@Test
	void weakExplicitValueStillStops() {
		MockEnvironment env = empty();
		env.setProperty("GATEWAY_ADMIN_PASSWORD", "short");
		assertTrue(run(env)[1].contains("GATEWAY_ADMIN_PASSWORD"));
	}
}
