package com.kevshake.gateway.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies a JDK {@link HttpClient} with explicit connect and read timeouts for outbound HTTP calls.
 */
@Configuration
public class OutboundHttpClientConfiguration {

	@Value("${gateway.http.connect-timeout-ms:5000}")
	private int connectTimeoutMs;

	@Value("${gateway.http.read-timeout-ms:30000}")
	private int readTimeoutMs;

	/**
	 * Builds the shared outbound HTTP client used by future bank or orchestration REST integrations.
	 */
	@Bean
	HttpClient gatewayHttpClient() {
		return HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(connectTimeoutMs))
				.build();
	}
}
