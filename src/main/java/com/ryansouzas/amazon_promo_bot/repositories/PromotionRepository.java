package com.ryansouzas.amazon_promo_bot.repositories;


import com.ryansouzas.amazon_promo_bot.entities.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {

    List<PromotionEntity> findByProductIdOrderByPublishedAtDesc(UUID productId);

    Optional<PromotionEntity>
    findFirstByProductIdAndTelegramChatIdOrderByPublishedAtDesc(UUID productId, String telegramChatId);
}
