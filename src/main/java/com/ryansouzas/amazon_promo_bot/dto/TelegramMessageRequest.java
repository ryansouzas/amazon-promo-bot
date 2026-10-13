package com.ryansouzas.amazon_promo_bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessageRequest(
        @JsonProperty("chat_id") String chatId,
        String text) {
}
