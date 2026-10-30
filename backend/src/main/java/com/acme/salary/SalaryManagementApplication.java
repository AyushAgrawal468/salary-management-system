package com.acme.salary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class SalaryManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalaryManagementApplication.class, args);
	}

	/** "Now" for business rules (lockouts, which salary is current); tests can swap in a fixed clock. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
