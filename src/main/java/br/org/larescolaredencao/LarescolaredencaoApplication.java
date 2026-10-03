package br.org.larescolaredencao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class LarescolaredencaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(LarescolaredencaoApplication.class, args);
	}

}