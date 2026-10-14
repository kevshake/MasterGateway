package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import com.kevshake.gateway.Iso8583MasterServerBase1Application;
import com.kevshake.gateway.support.GatewayTestCredentials;

class GatewayStartupFailureTest {

	private final GatewayEnvironmentValidator validator = new GatewayEnvironmentValidator();

	/** Verifies the environment validator rejects a missing admin password before context refresh. */
	@Test
	void validatorRejectsMissingAdminPasswordOnDefaultProfile() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.datasource.password", GatewayTestCredentials.H2_DATASOURCE_PASSWORD);
		try {
			validator.postProcessEnvironment(env, new SpringApplication());
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("GATEWAY_ADMIN_PASSWORD"));
			return;
		}
		throw new AssertionError("Expected IllegalStateException for missing admin password");
	}

	/** Verifies the environment validator rejects known unsafe passwords on the prod profile. */
	@Test
	void validatorRejectsKnownDefaultPasswordOnProdProfile() {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles("prod");
		env.setProperty("spring.datasource.password", GatewayTestCredentials.H2_DATASOURCE_PASSWORD);
		env.setProperty("GATEWAY_ADMIN_PASSWORD", "1234567890123456");
		try {
			validator.postProcessEnvironment(env, new SpringApplication());
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("known unsafe default"));
			return;
		}
		throw new AssertionError("Expected IllegalStateException for known default password");
	}
}
