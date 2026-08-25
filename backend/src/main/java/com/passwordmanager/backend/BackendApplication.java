package com.passwordmanager.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.net.URI;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl != null && !dbUrl.isEmpty()) {
			try {
				if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
					URI uri = new URI(dbUrl.replace("postgres://", "http://").replace("postgresql://", "http://"));
					String userInfo = uri.getUserInfo();
					String host = uri.getHost();
					int port = uri.getPort();
					if (port == -1) {
						port = 5432;
					}
					String path = uri.getPath();

					String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
					System.setProperty("spring.datasource.url", jdbcUrl);

					if (userInfo != null && userInfo.contains(":")) {
						String[] parts = userInfo.split(":", 2);
						System.setProperty("spring.datasource.username", parts[0]);
						System.setProperty("spring.datasource.password", parts[1]);
					}
				}
			} catch (Exception e) {
				System.err.println("Could not parse SPRING_DATASOURCE_URL: " + e.getMessage());
			}
		}
		SpringApplication.run(BackendApplication.class, args);
	}

}
