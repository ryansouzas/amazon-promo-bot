package com.ryansouzas.amazon_promo_bot.entities;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "published_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal publishedPrice;

    @Column(name = "discount_percentage", nullable = false,
            precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "telegram_chat_id", nullable = false)
    private String telegramChatId;

    @CreationTimestamp
    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


    public Promotion(
            Product product,
            BigDecimal originalPrice,
            BigDecimal publishedPrice,
            BigDecimal discountPercentage,
            String telegramChatId
    ) {
        this.product = product;
        this.originalPrice = originalPrice;
        this.publishedPrice = publishedPrice;
        this.discountPercentage = discountPercentage;
        this.telegramChatId = telegramChatId;
    }
}
