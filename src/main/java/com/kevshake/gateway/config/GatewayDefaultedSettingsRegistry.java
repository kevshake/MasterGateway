package com.kevshake.gateway.config;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds which settings were satisfied from generated defaults for startup banners and reminders.
 */
public final class GatewayDefaultedSettingsRegistry {

	private static final CopyOnWriteArrayList<GatewayDefaultedSetting> ACTIVE = new CopyOnWriteArrayList<>();

	private GatewayDefaultedSettingsRegistry() {
	}

	/** Replaces the active defaulted-setting list after environment post-processing. */
	public static void publish(List<GatewayDefaultedSetting> settings) {
		ACTIVE.clear();
		if (settings != null && !settings.isEmpty()) {
			ACTIVE.addAll(settings);
		}
	}

	/** Returns an immutable snapshot of settings still using generated defaults. */
	public static List<GatewayDefaultedSetting> current() {
		return Collections.unmodifiableList(List.copyOf(ACTIVE));
	}

	/** Describes one setting that currently relies on a generated default. */
	public record GatewayDefaultedSetting(GatewaySettingKind kind) {

		/** Banner and log text naming env var and alignment points without secret values. */
		public String remediationBannerLine() {
			return "[GATEWAY DEFAULT ACTIVE] "
					+ kind.configurationProperty()
					+ " / env "
					+ kind.environmentVariable()
					+ " — must also match: "
					+ kind.mustMatchElsewhere()
					+ ". How to set: "
					+ kind.howToSet()
					+ ". See docs/GATEWAY_CONFIGURATION.md.";
		}
	}
}
