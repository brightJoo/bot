package com.binance.bot;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients // Feign Client 패키지 지정
@EnableBatchProcessing // Spring Batch 활성화
@SpringBootApplication
@EnableScheduling // <-- 이 부분을 추가해야 합니다.
public class BotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BotApplication.class, args);
	}

}
