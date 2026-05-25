package com.raiz.bakcend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class RaizBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RaizBackendApplication.class, args);
	}

}
