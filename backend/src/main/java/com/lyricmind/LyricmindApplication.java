package com.lyricmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LyricmindApplication {

	public static void main(String[] args) {
		SpringApplication.run(LyricmindApplication.class, args);
	}

}
