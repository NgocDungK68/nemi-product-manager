package com.nemi;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class NemiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NemiApplication.class, args);
	}
}
