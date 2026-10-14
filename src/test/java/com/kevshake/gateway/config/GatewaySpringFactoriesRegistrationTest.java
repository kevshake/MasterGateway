package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class GatewaySpringFactoriesRegistrationTest {

	/** Ensures Boot 4.1 loads the validator via {@code META-INF/spring.factories}. */
	@Test
	void springFactoriesRegistersGatewayEnvironmentValidator() throws Exception {
		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream("META-INF/spring.factories")) {
			if (in == null) {
				throw new AssertionError("META-INF/spring.factories missing from classpath");
			}
			String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(contents.contains("org.springframework.boot.EnvironmentPostProcessor="));
			assertTrue(contents.contains(GatewayEnvironmentValidator.class.getName()));
		}
	}
}
