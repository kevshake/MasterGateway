package com.kevshake.gateway.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the full gateway context with generated secrets and without opening the Netty POS port.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractGatewayIntegrationTest {

	static {
		GatewayTestCredentials.installEarlyEnvironmentSecrets();
	}

	/** Injects dynamic test secrets so committed files never contain usable passwords. */
	@DynamicPropertySource
	static void registerGatewayTestProperties(DynamicPropertyRegistry registry) {
		GatewayTestCredentials.registerIntegrationProperties(registry);
	}
}
