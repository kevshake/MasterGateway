package com.kevshake.gateway.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;

/**
 * Reads which settings were satisfied from generated defaults via the Spring environment.
 */
final class GatewayDefaultedSettingsSupport {

	static final String DEFAULTED_SETTINGS_PROPERTY = "gateway.internal.defaulted-settings";

	private GatewayDefaultedSettingsSupport() {
	}

	/** Parses {@link #DEFAULTED_SETTINGS_PROPERTY} into remediation records. */
	static List<GatewayDefaultedSetting> readDefaultedSettings(Environment environment) {
		String raw = environment.getProperty(DEFAULTED_SETTINGS_PROPERTY);
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<GatewayDefaultedSetting> settings = new ArrayList<>();
		for (String token : raw.split(",")) {
			String envVar = token.trim();
			if (envVar.isEmpty()) {
				continue;
			}
			GatewaySettingKind kind = byEnvironmentVariable(envVar);
			if (kind != null) {
				settings.add(new GatewayDefaultedSetting(kind));
			}
		}
		return List.copyOf(settings);
	}

	/** Serializes defaulted environment variable names for injection into the environment. */
	static String encodeDefaultedSettings(List<GatewayDefaultedSetting> settings) {
		return settings.stream()
				.map(setting -> setting.kind().environmentVariable())
				.collect(Collectors.joining(","));
	}

	/** Looks up a setting kind from its environment variable name. */
	private static GatewaySettingKind byEnvironmentVariable(String envVar) {
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			if (kind.environmentVariable().equalsIgnoreCase(envVar)) {
				return kind;
			}
		}
		return null;
	}
}
