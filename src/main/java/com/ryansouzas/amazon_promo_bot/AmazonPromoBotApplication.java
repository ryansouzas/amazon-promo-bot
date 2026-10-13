package com.ryansouzas.amazon_promo_bot;

import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelegramProperties.class)
public class AmazonPromoBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmazonPromoBotApplication.class, args);
	}

}
