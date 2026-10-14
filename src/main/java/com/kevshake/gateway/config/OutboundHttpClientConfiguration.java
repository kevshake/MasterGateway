package com.kevshake.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Supplies a WebClient with explicit connect and read timeouts for any outbound HTTP calls.
 */
@Configuration
public class OutboundHttpClientConfiguration {

	@Value("${gateway.http.connect-timeout-ms:5000}")
	private int connectTimeoutMs;

	@Value("${gateway.http.read-timeout-ms:30000}")
	private int readTimeoutMs;

	/**
	 * Builds the application WebClient using Reactor Netty with configured connect and response timeouts.
	 */
	@Bean
	WebClient gatewayWebClient() {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
				.responseTimeout(Duration.ofMillis(readTimeoutMs));
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
