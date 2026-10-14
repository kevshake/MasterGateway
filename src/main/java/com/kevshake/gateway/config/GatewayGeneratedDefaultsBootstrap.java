package com.kevshake.gateway.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Generates or loads install-time defaults for missing gateway secrets and injects them into the environment.
 */
final class GatewayGeneratedDefaultsBootstrap {

	static final String DEFAULT_BANK_HOST = "127.0.0.1";

	static final String INTERNAL_DEFAULTS_FILE_PROPERTY = "gateway.internal.generated-defaults-file";

	private GatewayGeneratedDefaultsBootstrap() {
	}

	/** Supplies generated values for unset settings and records which keys were defaulted in the environment. */
	static void applyMissingDefaults(ConfigurableEnvironment environment) {
		String configuredFile = environment.getProperty(GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV);
		Path defaultsFile = GatewayGeneratedDefaultsFileStore.resolveDefaultsFilePath(configuredFile);
		Set<GatewaySettingKind> missingKinds = EnumSet.noneOf(GatewaySettingKind.class);
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			if (GatewayEnvironmentValidator.isUnset(GatewayEnvironmentValidator.readConfiguredValue(environment, kind))) {
				missingKinds.add(kind);
			}
		}
		Map<GatewaySettingKind, String> resolved = missingKinds.isEmpty()
				? Map.of()
				: loadOrGenerate(defaultsFile, missingKinds);
		List<GatewayDefaultedSetting> defaulted = new ArrayList<>();
		Map<String, Object> injected = new HashMap<>();

		for (GatewaySettingKind kind : missingKinds) {
			String value = resolved.get(kind);
			if (value == null || value.isBlank()) {
				throw new IllegalStateException("[" + kind.environmentVariable()
						+ "] Generated default missing after persistence.");
			}
			inject(kind, value, injected);
			defaulted.add(new GatewayDefaultedSetting(kind));
		}

		injected.put(INTERNAL_DEFAULTS_FILE_PROPERTY, defaultsFile.toString());
		injected.put(GatewayDefaultedSettingsSupport.DEFAULTED_SETTINGS_PROPERTY,
				GatewayDefaultedSettingsSupport.encodeDefaultedSettings(defaulted));
		if (!injected.isEmpty()) {
			environment.getPropertySources()
					.addFirst(new MapPropertySource("gatewayGeneratedDefaults", injected));
		}
	}

	/** Loads persisted values and generates any still-missing entries under an exclusive file lock. */
	private static Map<GatewaySettingKind, String> loadOrGenerate(Path defaultsFile, Set<GatewaySettingKind> missingKinds) {
		try {
			return GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(defaultsFile, missingKinds);
		} catch (IOException ex) {
			throw new IllegalStateException(
					"[" + GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV + "] Unable to access generated defaults file.",
					ex);
		}
	}

	/** Maps one logical setting onto its env-backed Spring property keys. */
	private static void inject(GatewaySettingKind kind, String value, Map<String, Object> target) {
		target.put(kind.environmentVariable(), value);
		target.put(kind.configurationProperty(), value);
	}
}
