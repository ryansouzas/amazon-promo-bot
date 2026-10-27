package com.ryansouzas.amazon_promo_bot.repositories;


import com.ryansouzas.amazon_promo_bot.entities.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    List<Promotion> findByProductIdOrderByPublishedAtDesc(UUID productId);

    Optional<Promotion>
    findFirstByProductIdAndTelegramChatIdOrderByPublishedAtDesc(UUID productId, String telegramChatId);
}
