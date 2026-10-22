package com.ryansouzas.amazon_promo_bot.dto;

import java.math.BigDecimal;

public record ProductDTO(
        String externalId,
        String name,
        BigDecimal originalPrice,
        BigDecimal currentPrice,
        String url
) {
}
