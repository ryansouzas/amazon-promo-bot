package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PromotionEvaluator {

    private static final BigDecimal MIN_DISCOUNT = new BigDecimal("15");

    public boolean isPromotion(ProductDTO productDTO) {

        BigDecimal discount = calculateDiscount(productDTO);
        return discount.compareTo(MIN_DISCOUNT) >= 0;

    }

    protected BigDecimal calculateDiscount(ProductDTO productDTO){
        BigDecimal difference = productDTO.originalPrice().subtract(productDTO.currentPrice());

        return difference
                .multiply(new BigDecimal(100))
                .divide(productDTO.originalPrice(), 2, RoundingMode.HALF_UP);
    }
}
