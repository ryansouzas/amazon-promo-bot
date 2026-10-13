package com.ryansouzas.amazon_promo_bot.service;


import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.dto.TelegramMessageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramService {

    private final RestClient restClient;
    private final TelegramProperties properties;

    public TelegramService(TelegramProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public void sendMessage(String message) {

        TelegramMessageRequest request =
                new TelegramMessageRequest(
                        properties.chatId(),
                        message
                );

        restClient.post()
                .uri("/bot{token}/sendMessage",
                        properties.token())
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
