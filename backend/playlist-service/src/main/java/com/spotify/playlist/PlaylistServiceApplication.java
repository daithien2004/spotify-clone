package com.spotify.playlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.spotify")
public class PlaylistServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaylistServiceApplication.class, args);
    }
}