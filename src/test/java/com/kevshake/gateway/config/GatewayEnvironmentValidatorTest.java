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
		env.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.adminPassword());
		env.setProperty("spring.h2.console.enabled", "true");
		assertThrows(IllegalStateException.class, () -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Ensures a strong admin password and disabled H2 console pass validation on the default profile. */
	@Test
	void acceptsStrongPasswordOnDefaultProfile() {
		MockEnvironment env = baseEnv();
		env.setProperty("GATEWAY_ADMIN_PASSWORD", GatewayTestCredentials.adminPassword());
		assertDoesNotThrow(() -> validator.postProcessEnvironment(env, new SpringApplication()));
	}

	/** Builds a mock environment with H2 console off and a valid datasource password but no admin password. */
	private static MockEnvironment baseEnv() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.datasource.password", GatewayTestCredentials.h2DatasourcePassword());
		env.setProperty("spring.h2.console.enabled", "false");
		env.setProperty("GATEWAY_ZONAL_KEY", GatewayTestCredentials.zonalKey());
		env.setProperty("GATEWAY_DEFAULT_TERMINAL_KEY", GatewayTestCredentials.defaultTerminalKey());
		env.setProperty("ISO8583_BANK_HOST", GatewayTestCredentials.bankHost());
		return env;
	}
}
