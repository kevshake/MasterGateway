package com.kevshake.gateway.config;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Prints and periodically repeats remediation banners for settings still on generated defaults.
 */
@Component
public class GatewayDefaultSettingReminder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(GatewayDefaultSettingReminder.class);

	private static final long HOURLY_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

	private final Environment environment;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "gateway-default-reminder");
		thread.setDaemon(true);
		return thread;
	});

	/** Captures the Spring environment so defaulted settings are read from property sources, not static state. */
	public GatewayDefaultSettingReminder(Environment environment) {
		this.environment = environment;
	}

	/** Emits startup banners and schedules hourly WARN reminders until operators replace defaults. */
	@Override
	public void run(ApplicationArguments args) {
		List<GatewayDefaultedSetting> settings = GatewayDefaultedSettingsSupport.readDefaultedSettings(environment);
		emitBanners(settings);
		if (!settings.isEmpty()) {
			scheduler.scheduleAtFixedRate(
					() -> emitBanners(GatewayDefaultedSettingsSupport.readDefaultedSettings(environment)),
					HOURLY_INTERVAL_MS,
					HOURLY_INTERVAL_MS,
					TimeUnit.MILLISECONDS);
		}
	}

	/** Stops hourly reminders when the application context closes. */
	@PreDestroy
	void shutdownReminderScheduler() {
		scheduler.shutdownNow();
	}

	/** Writes one remediation line per defaulted setting to logs and the JVM console stream. */
	private static void emitBanners(List<GatewayDefaultedSetting> settings) {
		if (settings.isEmpty()) {
			return;
		}
		for (GatewayDefaultedSetting setting : settings) {
			String line = setting.remediationBannerLine();
			log.warn(line);
			System.out.println(line);
		}
	}
}
