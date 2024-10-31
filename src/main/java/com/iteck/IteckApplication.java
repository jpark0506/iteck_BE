package com.iteck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class IteckApplication {
	public static void main(String[] args) {
		SpringApplication.run(IteckApplication.class, args);
	}
}
