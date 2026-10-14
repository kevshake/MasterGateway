package com.kevshake.gateway.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.kevshake.gateway.support.GatewayIntegrationTest;

@GatewayIntegrationTest
@AutoConfigureMockMvc
class GatewaySecurityMvcTest {

	@Autowired
	private MockMvc mockMvc;

	/** Confirms anonymous callers receive only the top-level health status. */
	@Test
	void healthEndpointShowsStatusOnly() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").exists())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	/** Confirms non-actuator HTTP routes are denied on the default profile. */
	@Test
	void nonHealthHttpRoutesAreDenied() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isForbidden());
	}

	/** Confirms the H2 console is not reachable when the local profile is inactive. */
	@Test
	void h2ConsoleDeniedWhenLocalProfileInactive() throws Exception {
		mockMvc.perform(get("/h2-console/"))
				.andExpect(status().isForbidden());
	}
}
