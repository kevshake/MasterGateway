package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import com.kevshake.gateway.support.GatewayTestCredentials;

class GatewayEnvironmentValidatorTest {

	private final GatewayEnvironmentValidator validator = new GatewayEnvironmentValidator();

	/** Ensures startup fails when the admin password is absent on the default profile. */
	@Test
	void rejectsMissingAdminPasswordOnDefaultProfile() {
		MockEnvironment env = baseEnv();
		assertThrows(IllegalStateException.class, () -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Ensures startup fails when the admin password is too short. */
	@Test
	void rejectsShortAdminPassword() {
		MockEnvironment env = baseEnv();
		env.setProperty("GATEWAY_ADMIN_PASSWORD", "short");
		assertThrows(IllegalStateException.class, () -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Ensures known sample defaults are rejected even on the local-only profile. */
	@Test
	void rejectsKnownDefaultPasswordOnLocalProfile() {
		MockEnvironment env = baseEnv();
		env.setActiveProfiles("local");
		env.setProperty("GATEWAY_ADMIN_PASSWORD", "change-me-local");
		assertThrows(IllegalStateException.class, () -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Ensures H2 console cannot be enabled outside the local-only profile. */
	@Test
	void rejectsH2ConsoleOutsideLocalProfile() {
		MockEnvironment env = baseEnv();
		env.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.ADMIN_PASSWORD);
		env.setProperty("spring.h2.console.enabled", "true");
		assertThrows(IllegalStateException.class, () -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Ensures a strong admin password and disabled H2 console pass validation on the default profile. */
	@Test
	void acceptsStrongPasswordOnDefaultProfile() {
		MockEnvironment env = baseEnv();
		env.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.ADMIN_PASSWORD);
		assertDoesNotThrow(() -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	private static MockEnvironment baseEnv() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.datasource.password", GatewayTestCredentials.H2_DATASOURCE_PASSWORD);
		env.setProperty("spring.h2.console.enabled", "false");
		return env;
	}
}
