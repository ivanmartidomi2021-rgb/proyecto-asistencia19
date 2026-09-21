package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        // TEMP - genera hash real
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println("HASH_12345: " + enc.encode("12345"));

        SpringApplication.run(DemoApplication.class, args);
    }
}