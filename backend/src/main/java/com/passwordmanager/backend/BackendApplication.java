package com.passwordmanager.backend;

import com.passwordmanager.backend.config.DatabaseUrlNormalizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		try {
			DatabaseUrlNormalizer.applyEnvironmentNormalization();
		} catch (Exception e) {
			System.err.println("Database URL normalization skipped: " + e.getMessage());
		}
		SpringApplication.run(BackendApplication.class, args);
	}

}
