package com.kevshake.gateway.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * HTTP security for the embedded servlet stack (H2 console and actuator); ISO8583 TCP is handled outside Spring Security.
 */
@Configuration
@EnableWebSecurity
public class GatewaySecurityConfiguration {

	/** Anonymous actuator health with status-only payload (no component breakdown). */
	@Bean
	@Order(1)
	SecurityFilterChain actuatorHealthSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(EndpointRequest.to("health"))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(form -> form.disable());
		return http.build();
	}

	/** HTTP Basic on {@code /h2-console/**} when the local profile is active. */
	@Bean
	@Order(2)
	@Profile("local")
	SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/h2-console/**"))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}

	/** Deny-all fallback so no servlet route bypasses the scoped chains above. */
	@Bean
	@Order(3)
	SecurityFilterChain denyAllCatchAllSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/**")
				.authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(form -> form.disable());
		return http.build();
	}
}
