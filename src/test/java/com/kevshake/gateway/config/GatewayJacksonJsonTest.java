package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevshake.gateway.support.AbstractGatewayIntegrationTest;
import com.kevshake.gateway.support.GatewayIntegrationTest;

import tools.jackson.databind.json.JsonMapper;

@GatewayIntegrationTest
class GatewayJacksonJsonTest extends AbstractGatewayIntegrationTest {

	@Autowired
	private JsonMapper jsonMapper;

	/** Serializes whole BigDecimal amounts as plain JSON numbers without scientific notation. */
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

	/** Ignores unknown JSON properties during deserialization via the shared application JsonMapper. */
	@Test
	void unknownPropertiesAreIgnored() throws Exception {
		SampleDto dto = jsonMapper.readValue("{\"name\":\"pos\",\"unexpected\":true}", SampleDto.class);
		assertEquals("pos", dto.name());
	}

	/** Holds a BigDecimal field for Jackson serialization tests. */
	private record AmountHolder(BigDecimal amount) {
	}

	/** Holds a calendar date field for Jackson Java Time tests. */
	private record DateHolder(LocalDate day) {
	}

	/** Holds a single name field for unknown-property deserialization tests. */
	private record SampleDto(String name) {
	}
}
