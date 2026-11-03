package com.flash.bookingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BookingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingEngineApplication.class, args);
    }

}
