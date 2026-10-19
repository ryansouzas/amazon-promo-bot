package com.ryansouzas.amazon_promo_bot.service;


import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.dto.TelegramMessageRequest;
import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class TelegramService {

    private final RestClient restClient;
    private final TelegramProperties properties;

    public TelegramService( RestClient.Builder builder, TelegramProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public void sendMessage(String message) {

        TelegramMessageRequest request =
                new TelegramMessageRequest(
                        properties.chatId(),
                        message
                );

        try {

            restClient.post()
                    .uri("/bot{token}/sendMessage",
                            properties.token())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent message to telegram");

        } catch (RestClientException e) {

            log.error(
                    "Failed to send message to telegram {}",
                    e.getClass().getSimpleName());

            throw new TelegramIntegrationException(
                    "Failed to comunicate with Telegram", e);
        }
    }

}
