package com.globaldorm.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Main Spring Boot application class for Global Dorm Orchestrator
 * Extends SpringBootServletInitializer to enable deployment on Tomcat server
 * Provides service composition for student accommodation management
 */
@SpringBootApplication
public class GlobalDormOrchestrator extends SpringBootServletInitializer {

	/**
	 * Application entry point
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(GlobalDormOrchestrator.class, args);
	}

	/**
	 * Bean configuration for HTTP client used by external services
	 * 
	 * @return RestTemplate instance for making HTTP requests
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}