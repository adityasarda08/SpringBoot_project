package com.dataline.BajajPortal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
//import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.dataline.BajajPortal.repository")
public class BajajPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BajajPortalApplication.class, args);
	}

}
