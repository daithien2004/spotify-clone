package com.spotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpotifyCloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpotifyCloneApplication.class, args);
    }
}
