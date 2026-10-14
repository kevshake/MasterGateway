package com.kevshake.gateway.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks integration tests that inherit {@link AbstractGatewayIntegrationTest} wiring.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayIntegrationTest {
}
