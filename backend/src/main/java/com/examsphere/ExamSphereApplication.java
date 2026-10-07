package com.examsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. Authentication is fully JWT based, so the default in-memory
 * user that Spring Security would otherwise generate is switched off.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class ExamSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamSphereApplication.class, args);
    }
}
