package com.kevshake.gateway.config;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Prints and periodically repeats remediation banners for settings still on generated defaults.
 */
@Component
public class GatewayDefaultSettingReminder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GatewayDefaultSettingReminder.class);

	private static final long HOURLY_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "gateway-default-reminder");
		thread.setDaemon(true);
		return thread;
	});

	/** Emits startup banners and schedules hourly WARN reminders until operators replace defaults. */
	@Override
	public void run(ApplicationArguments args) {
		emitBanners(GatewayDefaultedSettingsRegistry.current());
		if (!GatewayDefaultedSettingsRegistry.current().isEmpty()) {
			scheduler.scheduleAtFixedRate(
					() -> emitBanners(GatewayDefaultedSettingsRegistry.current()),
					HOURLY_INTERVAL_MS,
					HOURLY_INTERVAL_MS,
					TimeUnit.MILLISECONDS);
		}
	}

	/** Writes one remediation line per defaulted setting to logs and the JVM console stream. */
	private static void emitBanners(List<GatewayDefaultedSettingsRegistry.GatewayDefaultedSetting> settings) {
		if (settings.isEmpty()) {
			return;
		}
		for (GatewayDefaultedSettingsRegistry.GatewayDefaultedSetting setting : settings) {
			String line = setting.remediationBannerLine();
			log.warn(line);
			System.out.println(line);
		}
	}
}
