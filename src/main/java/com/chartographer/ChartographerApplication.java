package com.chartographer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.stereotype.Repository;

@SpringBootApplication
public class ChartographerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChartographerApplication.class, args);
	}



}
