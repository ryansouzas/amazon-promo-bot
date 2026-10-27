package com.ryansouzas.amazon_promo_bot.repositories;


import com.ryansouzas.amazon_promo_bot.entities.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByProductIdOrderByRecordedAtDesc(UUID productId);

    Optional<PriceHistory> findFirstByProductIdOrderByRecordedAtDesc(UUID productId);
}
