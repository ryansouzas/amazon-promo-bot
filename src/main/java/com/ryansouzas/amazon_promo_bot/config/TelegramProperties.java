package com.ryansouzas.amazon_promo_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties(
        String token,
        String chatId
) {
}
