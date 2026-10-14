package com.kevshake.gateway.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamWriteFeature;

/**
 * Keeps Jackson 3 JSON serialization aligned with the pre-upgrade gateway defaults.
 */
@Configuration
public class GatewayJacksonConfiguration {

	/** Tolerates unknown JSON fields and emits BigDecimal values as plain JSON numbers (no scientific notation). */
	@Bean
	JsonMapperBuilderCustomizer gatewayJsonMapperCustomizer() {
		return builder -> {
			builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
		};
	}
}
