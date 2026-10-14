package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.json.JsonMapper;

class GatewayJacksonJsonTest {

	private JsonMapper jsonMapper;

	/** Builds a JsonMapper that mirrors {@link GatewayJacksonConfiguration} defaults. */
	@BeforeEach
	void setUp() {
		jsonMapper = JsonMapper.builder()
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
				.build();
	}

	/** Serializes whole BigDecimal amounts without scientific notation. */
	@Test
	void bigDecimalUsesPlainDecimalNotation() throws Exception {
		String json = jsonMapper.writeValueAsString(new AmountHolder(new BigDecimal("3990")));
		assertEquals("{\"amount\":3990}", json);
		assertFalse(json.contains("E+"));
	}

	/** Serializes ISO-8601 calendar dates as JSON strings. */
	@Test
	void javaTimeLocalDateSerializedAsString() throws Exception {
		String json = jsonMapper.writeValueAsString(new DateHolder(LocalDate.of(2026, 10, 14)));
		assertEquals("{\"day\":\"2026-10-14\"}", json);
	}

	/** Ignores unknown JSON properties during deserialization. */
	@Test
	void unknownPropertiesAreIgnored() throws Exception {
		SampleDto dto = jsonMapper.readValue("{\"name\":\"pos\",\"unexpected\":true}", SampleDto.class);
		assertEquals("pos", dto.name());
	}

	private record AmountHolder(BigDecimal amount) {
	}

	private record DateHolder(LocalDate day) {
	}

	private record SampleDto(String name) {
	}
}
