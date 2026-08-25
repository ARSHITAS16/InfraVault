package com.passwordmanager.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl != null && !dbUrl.startsWith("jdbc:")) {
			if (dbUrl.startsWith("postgres://")) {
				dbUrl = "jdbc:postgresql://" + dbUrl.substring(11);
			} else if (dbUrl.startsWith("postgresql://")) {
				dbUrl = "jdbc:postgresql://" + dbUrl.substring(13);
			}
			System.setProperty("spring.datasource.url", dbUrl);
		}
		SpringApplication.run(BackendApplication.class, args);
	}

}
