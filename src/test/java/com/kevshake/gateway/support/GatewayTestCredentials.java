package com.kevshake.gateway.support;

/**
 * Shared non-secret test credentials (used only in unit/integration tests).
 */
public final class GatewayTestCredentials {

	public static final String ADMIN_PASSWORD = "IntegrationTest-Admin-Pass-32chars!";
	public static final String H2_DATASOURCE_PASSWORD = "IntegrationTest-H2-Pass-32chars!!";

	private GatewayTestCredentials() {
	}
}
