package com.kevshake.gateway.config;

/**
 * One gateway setting that currently relies on an install-generated default.
 */
record GatewayDefaultedSetting(GatewaySettingKind kind) {

	/** Banner and log text naming env var and alignment points without secret values. */
	String remediationBannerLine() {
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
