package com.ryansouzas.amazon_promo_bot.validation;

import com.ryansouzas.amazon_promo_bot.service.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;

@Component
public class ProductValidator {

    public void validate(ProductDTO productDTO) {
        if (productDTO == null){
            throw new InvalidProductException("Product cannot be null");
        }

        if (isBlank(productDTO.externalId())) {
            throw new InvalidProductException(
                    "Product external ID is required"
            );
        }

        if (isBlank(productDTO.name())) {
            throw new InvalidProductException(
                    "Product name is required"
            );
        }

        validatePrices(productDTO);
        validateUrl(productDTO.url());
    }


    private void validatePrices(ProductDTO productDTO) {
        BigDecimal original =  productDTO.originalPrice();
        BigDecimal currentPrice = productDTO.currentPrice();

        if (original == null || currentPrice == null){
            throw new InvalidProductException("Product prices cannot be null");
        }

        if(original.signum() <= 0 || currentPrice.signum() <= 0){
            throw new InvalidProductException(
                    "Product price must be positive"
            );
        }

        if (currentPrice.compareTo(original) > 0){
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

            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null){
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
