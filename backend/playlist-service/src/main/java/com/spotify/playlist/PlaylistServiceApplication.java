package com.spotify.playlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // @Async rebalance (Reorder/AddTrack + periodic sweep) on the bounded taskExecutor
@EnableScheduling // periodic PlaylistRebalanceScheduler sweep
@SpringBootApplication(scanBasePackages = "com.spotify")
public class PlaylistServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaylistServiceApplication.class, args);
    }
}