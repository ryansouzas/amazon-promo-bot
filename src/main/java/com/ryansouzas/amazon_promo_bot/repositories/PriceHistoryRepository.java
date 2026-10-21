package com.ryansouzas.amazon_promo_bot.repositories;


import com.ryansouzas.amazon_promo_bot.entities.PriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, UUID> {

    List<PriceHistoryEntity> findByProductIdOrderByRecordedAtDesc(UUID productId);

    Optional<PriceHistoryEntity> findFirstByProductIdOrderByRecordedAtDesc(UUID productId);
}
