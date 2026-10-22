package com.ryansouzas.amazon_promo_bot.service.exceptions;

public class ProductExistsException extends RuntimeException {
    public ProductExistsException(String message) {
        super(message);
    }

}
