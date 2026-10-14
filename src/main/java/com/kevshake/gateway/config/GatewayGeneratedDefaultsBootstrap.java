package com.kevshake.gateway.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/** Supplies generated values for unset settings and records which keys were defaulted. */
	static void applyMissingDefaults(ConfigurableEnvironment environment) {
		String configuredFile = environment.getProperty(GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV);
		Path defaultsFile = GatewayGeneratedDefaultsFileStore.resolveDefaultsFilePath(configuredFile);
		Map<GatewaySettingKind, String> persisted = loadPersisted(defaultsFile);
		List<GatewayDefaultedSettingsRegistry.GatewayDefaultedSetting> defaulted = new ArrayList<>();
		Map<String, Object> injected = new HashMap<>();
		Map<GatewaySettingKind, String> newlyGenerated = new EnumMap<>(GatewaySettingKind.class);

		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			String existing = GatewayEnvironmentValidator.readConfiguredValue(environment, kind);
			if (!GatewayEnvironmentValidator.isUnset(existing)) {
				continue;
			}
			String value = persisted.get(kind);
			if (value == null || value.isBlank()) {
				value = generateValue(kind);
				newlyGenerated.put(kind, value);
			}
			inject(kind, value, injected);
			defaulted.add(new GatewayDefaultedSettingsRegistry.GatewayDefaultedSetting(kind));
		}

		if (!newlyGenerated.isEmpty()) {
			persistNewValues(defaultsFile, newlyGenerated, persisted);
		}
		injected.put(INTERNAL_DEFAULTS_FILE_PROPERTY, defaultsFile.toString());
		if (!injected.isEmpty()) {
			environment.getPropertySources()
					.addFirst(new MapPropertySource("gatewayGeneratedDefaults", injected));
		}
		GatewayDefaultedSettingsRegistry.publish(defaulted);
	}

	/** Loads persisted secrets, returning empty when the file is absent or unreadable. */
	private static Map<GatewaySettingKind, String> loadPersisted(Path defaultsFile) {
		try {
			return GatewayGeneratedDefaultsFileStore.load(defaultsFile);
		} catch (IOException ex) {
			throw new IllegalStateException(
					"[" + GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV + "] Unable to read generated defaults file.",
					ex);
		}
	}

	/** Writes freshly generated secrets so later restarts reuse the same install defaults. */
	private static void persistNewValues(
			Path defaultsFile,
			Map<GatewaySettingKind, String> newlyGenerated,
			Map<GatewaySettingKind, String> persisted) {
		Map<GatewaySettingKind, String> toSave = new EnumMap<>(GatewaySettingKind.class);
		toSave.putAll(persisted);
		toSave.putAll(newlyGenerated);
		try {
			GatewayGeneratedDefaultsFileStore.save(defaultsFile, toSave);
		} catch (IOException ex) {
			throw new IllegalStateException(
					"[" + GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV + "] Unable to write generated defaults file.",
					ex);
		}
	}

	/** Maps one logical setting onto its env-backed Spring property keys. */
	private static void inject(GatewaySettingKind kind, String value, Map<String, Object> target) {
		target.put(kind.environmentVariable(), value);
		target.put(kind.configurationProperty(), value);
	}

	/** Creates a new secret or host default for the given setting kind. */
	private static String generateValue(GatewaySettingKind kind) {
		return switch (kind) {
			case ADMIN_PASSWORD, H2_DATASOURCE_PASSWORD -> GatewayGeneratedDefaultsFileStore.generateStrongPassword();
			case ZONAL_PIN_KEY, DEFAULT_TERMINAL_PIN_KEY -> GatewayTdesKeyMaterialGenerator.generateDoubleLengthTdesHexKey();
			case BANK_HOST -> DEFAULT_BANK_HOST;
		};
	}
}
