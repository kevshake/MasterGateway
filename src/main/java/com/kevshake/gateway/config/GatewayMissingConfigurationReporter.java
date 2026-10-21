package com.kevshake.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Missing-configuration registry report for MasterGateway.
 *
 * <p>Evaluates every {@link GatewaySettingKind} before generated defaults are applied and prints one
 * banner per missing setting naming the env var, the Spring property, every other place the value
 * must match, and how to set it (values are never printed). On fleet profiles (uat, prod,
 * production, live) any missing setting stops startup after the full report, because a generated
 * PIN key or loopback bank host can never match the real bank/switch. On lab profiles the
 * generated-defaults path continues and {@link GatewayDefaultSettingReminder} repeats hourly.
 */
final class GatewayMissingConfigurationReporter {

	private static final Logger log = LoggerFactory.getLogger(GatewayMissingConfigurationReporter.class);

	/** Profiles on which generated defaults are refused (fail closed). */
	static final Set<String> FLEET_PROFILES = Set.of("uat", "prod", "production", "live");

	private GatewayMissingConfigurationReporter() {
	}

	/** Returns every setting that is unset or still an unresolved placeholder. */
	static List<GatewaySettingKind> findMissing(ConfigurableEnvironment environment) {
		List<GatewaySettingKind> missing = new ArrayList<>();
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			if (GatewayEnvironmentValidator.isUnset(GatewayEnvironmentValidator.readConfiguredValue(environment, kind))) {
				missing.add(kind);
			}
		}
		return missing;
	}

	/** True when any active profile is a fleet profile. */
	static boolean isFleetProfile(ConfigurableEnvironment environment) {
		return Arrays.stream(environment.getActiveProfiles())
				.map(p -> p.trim().toLowerCase(Locale.ROOT))
				.anyMatch(FLEET_PROFILES::contains);
	}

	/** Formats the console/log banner for one missing setting (no values). */
	static String banner(GatewaySettingKind kind, boolean fleet) {
		return "[GATEWAY MISSING CONFIGURATION] " + kind.environmentVariable()
				+ " (property " + kind.configurationProperty() + ") is not set. "
				+ (fleet
						? "Fleet profile: startup refused (generated defaults are not allowed on uat/prod/live). "
						: "Lab profile: a generated default will be used and reminded hourly. ")
				+ "Must also match: " + kind.mustMatchElsewhere()
				+ ". How to set: " + kind.howToSet()
				+ ". See docs/GATEWAY_CONFIGURATION.md.";
	}

	/**
	 * Prints the full missing-setting report and, on fleet profiles, throws listing every missing
	 * env var so operators fix them all in one pass.
	 */
	static void reportAndEnforce(ConfigurableEnvironment environment) {
		List<GatewaySettingKind> missing = findMissing(environment);
		if (missing.isEmpty()) {
			return;
		}
		boolean fleet = isFleetProfile(environment);
		List<String> names = new ArrayList<>();
		for (GatewaySettingKind kind : missing) {
			String line = banner(kind, fleet);
			log.warn(line);
			System.err.println(line);
			names.add(kind.environmentVariable());
		}
		if (fleet) {
			throw new IllegalStateException("[" + String.join(", ", names) + "] Required settings are missing on a "
					+ "fleet profile; set them from the deployment secret store (see banners above and "
					+ "docs/GATEWAY_CONFIGURATION.md). Generated defaults are refused on uat/prod/live.");
		}
	}
}
