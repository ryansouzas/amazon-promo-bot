package com.ryansouzas.amazon_promo_bot.exceptions;

public class TelegramIntegrationException extends RuntimeException {
    public TelegramIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
    public TelegramIntegrationException(String message) {
        super(message);
    }
}
