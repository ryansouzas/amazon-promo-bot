package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PromotionEvaluator {

    private static final BigDecimal MIN_DISCOUNT = new BigDecimal("15");

    public boolean isPromotion(Product product) {

        BigDecimal discount = calculateDiscount(product);
        return discount.compareTo(MIN_DISCOUNT) >= 0;

    }

    protected BigDecimal calculateDiscount(Product product){
        BigDecimal difference = product.originalPrice().subtract(product.currentPrice());

        return difference
                .multiply(new BigDecimal(100))
                .divide(product.originalPrice(), 2, RoundingMode.HALF_UP);
    }
}
