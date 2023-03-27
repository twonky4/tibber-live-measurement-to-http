package de.viseit.tibber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TibberApplication {
	public static void main(String[] args) {
		SpringApplication.run(TibberApplication.class, args);
	}
}
