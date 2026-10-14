package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import com.kevshake.gateway.support.GatewayTestCredentials;

class GatewayStartupFailureTest {

	private final GatewayEnvironmentValidator validator = new GatewayEnvironmentValidator();

	/** Verifies the environment validator rejects known unsafe passwords on the prod profile. */
	@Test
	void validatorRejectsKnownDefaultPasswordOnProdProfile() {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles("prod");
		env.setProperty("spring.datasource.password", GatewayTestCredentials.h2DatasourcePassword());
		env.setProperty("GATEWAY_ADMIN_PASSWORD", "1234567890123456");
		env.setProperty("GATEWAY_ZONAL_KEY", GatewayTestCredentials.zonalKey());
		env.setProperty("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials.defaultTerminalKey());
		env.setProperty("ISO8583_BANK_HOST", GatewayTestCredentials.bankHost());
		try {
			validator.postProcessEnvironment(env, new SpringApplication());
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("known unsafe"));
			return;
		}
		throw new AssertionError("Expected IllegalStateException for known default password");
	}
}
