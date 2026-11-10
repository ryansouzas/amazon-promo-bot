package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.entities.Product;
import com.ryansouzas.amazon_promo_bot.entities.Promotion;
import com.ryansouzas.amazon_promo_bot.entities.PromotionStatus;
import com.ryansouzas.amazon_promo_bot.repositories.ProductRepository;
import com.ryansouzas.amazon_promo_bot.repositories.PromotionRepository;
import com.ryansouzas.amazon_promo_bot.service.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.service.exceptions.PublicationInProgressException;
import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.source.ProductSource;
import com.ryansouzas.amazon_promo_bot.validation.ProductValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {


    private final ProductSource productSource;
    private final ProductValidator productValidator;
    private final PromotionEvaluator promotionEvaluator;
    private final PromotionPublisher promotionPublisher;

    private final PromotionRepository promotionRepository;
    private final ProductService productService;
    private final TelegramProperties telegramProperties;

    private static final List<PromotionStatus> BLOCKING_STATUSES =
            List.of(PromotionStatus.PENDING, PromotionStatus.SENT);

    private final ReentrantLock publishLock = new ReentrantLock();

    public void publishPromotions(){
        if (!publishLock.tryLock()) {
            log.warn("Publication already in progress, skipping execution");
            throw new PublicationInProgressException("Publication already in progress");
        }

        try {
            List<ProductDTO> products = productSource.findProducts();
            products.forEach(productDTO -> {
                try {
                    productValidator.validate(productDTO);

                    Product storedProduct = productService.syncProduct(productDTO);

                    if (!promotionEvaluator.isPromotion(productDTO)) {
                        return;
                    }

                    BigDecimal discount = promotionEvaluator.calculateDiscount(productDTO);

                    Optional<Promotion> lastPublication =
                            promotionRepository.findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                                    storedProduct.getId(),
                                    telegramProperties.chatId(),
                                    BLOCKING_STATUSES
                            );

                    boolean publish = shouldPublish(
                            productDTO.currentPrice(),
                            lastPublication,
                            Instant.now()
                    );

                    if (!publish) {
                        return;
                    }

                    publishAtMostOnce(storedProduct, productDTO, discount);

                } catch (InvalidProductException e) {
                    log.warn("Invalid product {}", e.getMessage());

                } catch (TelegramIntegrationException e) {
                    log.warn("Failed to publish product {}", productDTO.externalId());
                } catch (Exception e) {
                    log.error("Unexpected error processing product {}", productDTO.externalId(), e);
                }
            });
        } finally {
            publishLock.unlock();
        }
    }

    private void publishAtMostOnce(Product product, ProductDTO productDTO, BigDecimal discount) {

        Promotion promotion = new Promotion(
                product,
                productDTO.originalPrice(),
                productDTO.currentPrice(),
                discount,
                telegramProperties.chatId()
        );

        promotionRepository.save(promotion);

        try {
            promotionPublisher.publish(productDTO, discount);
        } catch (TelegramIntegrationException e) {
            promotion.markFailed();
            promotionRepository.save(promotion);
            throw e;
        }

        promotion.markSent();
        promotionRepository.save(promotion);
    }

    private boolean shouldPublish(BigDecimal currentPrice, Optional<Promotion> lastPublication, Instant now) {

        if (lastPublication.isEmpty()) {
            return true;
        }

        Promotion lastPromotion = lastPublication.get();

        BigDecimal lastPrice = lastPromotion.getPublishedPrice();

        int comparison = currentPrice.compareTo(lastPrice);

        if (comparison < 0) {
            return true;
        }

        if (comparison > 0) {
            return false;
        }

        // Mesmo preço: verificar intervalo de 24 horas
        Duration elapsedTime = Duration.between(
                lastPromotion.getPublishedAt(),
                now
        );

        return elapsedTime.compareTo(Duration.ofHours(24)) >= 0;
    }

}
