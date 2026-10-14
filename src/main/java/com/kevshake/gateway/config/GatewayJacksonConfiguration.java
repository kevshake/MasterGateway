package com.kevshake.gateway.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * Keeps Jackson 3 JSON serialization aligned with the pre-upgrade gateway defaults.
 */
@Configuration
public class GatewayJacksonConfiguration {

	/**
	 * Registers Java time support, lenient deserialization, and plain BigDecimal output on the shared JsonMapper.
	 */
	@Bean
	JsonMapperBuilderCustomizer gatewayJsonMapperCustomizer() {
		return builder -> {
			builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			SimpleModule plainDecimalModule = new SimpleModule("GatewayPlainBigDecimalModule");
			plainDecimalModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
			builder.addModule(plainDecimalModule);
		};
	}
}
