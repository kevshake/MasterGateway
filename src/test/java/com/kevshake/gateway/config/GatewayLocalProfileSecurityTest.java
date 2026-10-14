package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kevshake.gateway.support.AbstractGatewayIntegrationTest;

@ActiveProfiles("local")
class GatewayLocalProfileSecurityTest extends AbstractGatewayIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@LocalServerPort
	private int port;

	@Value("${spring.security.user.password}")
	private String adminPassword;

	/** Keeps Netty POS listener off while exercising local H2 console security. */
	@DynamicPropertySource
	static void localProfileProperties(DynamicPropertyRegistry registry) {
		registry.add("iso8583.pos.netty-server-enabled", () -> "false");
		registry.add("spring.h2.console.enabled", () -> "true");
	}

	/** Requires authentication before the H2 console UI is reachable on the local profile. */
	@Test
	void h2ConsoleRequiresAuthenticationOnLocalProfile() throws Exception {
		mockMvc.perform(get("/h2-console/"))
				.andExpect(status().isUnauthorized());
	}

	/** Allows authenticated access to the H2 console via the embedded Tomcat listener. */
	@Test
	void h2ConsoleAllowsBasicAuthenticationOnLocalProfile() throws Exception {
		String credentials = Base64.getEncoder()
				.encodeToString(("gateway-admin:" + adminPassword).getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/h2-console/"))
				.header("Authorization", "Basic " + credentials)
				.GET()
				.build();
		HttpResponse<Void> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.discarding());
		int status = response.statusCode();
		assertTrue(
				status >= 200 && status < 400,
				() -> "Expected 2xx/3xx after Basic auth but received HTTP " + status);
	}
}
