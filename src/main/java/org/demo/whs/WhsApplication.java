package org.demo.whs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class WhsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhsApplication.class, args);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String rawPassword = "123456789";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        System.out.println("Raw password    : " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
    }

}
