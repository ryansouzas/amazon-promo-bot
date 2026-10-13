package com.ryansouzas.amazon_promo_bot.model;

import java.math.BigDecimal;

public record Product(
        String externalId,
        String name,
        BigDecimal originalPrice,
        BigDecimal currentPrice,
        String url
) {
}
