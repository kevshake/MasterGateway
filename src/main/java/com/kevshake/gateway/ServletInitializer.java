package com.kevshake.gateway;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

	/** Wires the ISO8583 gateway application when deployed as a WAR on an external servlet container. */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Iso8583MasterServerBase1Application.class);
	}

}
