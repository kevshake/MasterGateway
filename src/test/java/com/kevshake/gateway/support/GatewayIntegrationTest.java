package com.kevshake.gateway.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the gateway with required secrets and without binding the Netty POS port.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"GATEWAY_ADMIN_PASSWORD=" + GatewayTestCredentials.ADMIN_PASSWORD,
		"H2_DATASOURCE_PASSWORD=" + GatewayTestCredentials.H2_DATASOURCE_PASSWORD,
		"iso8583.pos.netty-server-enabled=false"
})
public @interface GatewayIntegrationTest {
}
