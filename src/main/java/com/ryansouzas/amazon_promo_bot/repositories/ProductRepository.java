package com.ryansouzas.amazon_promo_bot.repositories;

import com.ryansouzas.amazon_promo_bot.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository

public interface ProductRepository
        extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByExternalId(String externalId);
}
