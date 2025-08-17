package com.kevshake.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.kevshake.gateway.repository")
@EntityScan(basePackages = "com.kevshake.gateway.entity")
public class Iso8583MasterServerBase1Application {

	public static void main(String[] args) {
		SpringApplication.run(Iso8583MasterServerBase1Application.class, args);
	}

}
