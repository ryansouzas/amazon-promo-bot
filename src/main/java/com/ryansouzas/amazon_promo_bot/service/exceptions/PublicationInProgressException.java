package com.ryansouzas.amazon_promo_bot.service.exceptions;

public class PublicationInProgressException extends RuntimeException {
    public PublicationInProgressException(String message) {
        super(message);
    }

}
