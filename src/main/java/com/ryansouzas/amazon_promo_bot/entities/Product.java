package com.ryansouzas.amazon_promo_bot.entities;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "current_price", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product(String externalId, String name,
                   BigDecimal originalPrice, BigDecimal currentPrice, String url) {
        this.externalId = externalId;
        this.name = name;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.url = url;
    }


}

