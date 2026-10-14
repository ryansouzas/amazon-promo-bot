package com.ryansouzas.amazon_promo_bot.validation;

import com.ryansouzas.amazon_promo_bot.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;

@Component
public class ProductValidator {

    public void validate(Product product) {
        if (product == null){
            throw new InvalidProductException("Product cannot be null");
        }

        if (isBlank(product.externalId())) {
            throw new InvalidProductException(
                    "Product external ID is required"
            );
        }

        if (isBlank(product.name())) {
            throw new InvalidProductException(
                    "Product name is required"
            );
        }

        validatePrices(product);
        validateUrl(product.url());
    }


    private void validatePrices(Product product) {
        BigDecimal original =  product.originalPrice();
        BigDecimal currentPrice = product.currentPrice();

        if (original == null || currentPrice == null){
            throw new InvalidProductException("Product prices cannot be null");
        }

        if(original.signum() <= 0 || currentPrice.signum() <= 0){
            throw new InvalidProductException(
                    "Product price must be positive"
            );
        }

        if (original.compareTo(currentPrice) > 0){
            throw new InvalidProductException(
                    "Current price cannot exceed original price"
            );
        }

    }

    private void validateUrl(String url) {
        if (isBlank(url)){
            throw new InvalidProductException("Product url is required");
        }

        try {
            URI uri = URI.create(url);

            if("https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null){
                throw new InvalidProductException("Product URL must be valid HTTPS URL");
            }

        }catch (IllegalArgumentException e){
            throw new InvalidProductException("Invalid product URL");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
