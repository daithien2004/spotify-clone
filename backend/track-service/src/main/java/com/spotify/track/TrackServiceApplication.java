package com.spotify.track;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.spotify")
public class TrackServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrackServiceApplication.class, args);
    }
}