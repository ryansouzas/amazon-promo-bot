package com.ryansouzas.amazon_promo_bot.entities;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "current_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    public PriceHistoryEntity(
            ProductEntity product,
            BigDecimal originalPrice,
            BigDecimal currentPrice
    ) {
        this.product = product;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
    }
}

