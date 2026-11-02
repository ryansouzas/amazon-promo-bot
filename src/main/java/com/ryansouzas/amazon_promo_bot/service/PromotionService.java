package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.entities.Product;
import com.ryansouzas.amazon_promo_bot.entities.Promotion;
import com.ryansouzas.amazon_promo_bot.repositories.ProductRepository;
import com.ryansouzas.amazon_promo_bot.repositories.PromotionRepository;
import com.ryansouzas.amazon_promo_bot.service.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.service.exceptions.ResourceNotFoundException;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {


    private final ProductSource productSource;
    private final ProductValidator productValidator;
    private final PromotionEvaluator promotionEvaluator;
    private final PromotionPublisher promotionPublisher;

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final TelegramProperties telegramProperties;

    public void publishPromotions(){
        List<ProductDTO> products = productSource.findProducts();
        products.forEach(productDTO -> {
            try {
                productValidator.validate(productDTO);

                if (!promotionEvaluator.isPromotion(productDTO)) {
                    return;
                }

                BigDecimal discount = promotionEvaluator.calculateDiscount(productDTO);

                Product storedProduct = productRepository
                        .findByExternalId(productDTO.externalId())
                                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

                Optional<Promotion> lastPublication =
                        promotionRepository.findFirstByProductIdAndTelegramChatIdOrderByPublishedAtDesc(
                                storedProduct.getId(),
                                telegramProperties.chatId()
                        );

                boolean publish = shouldPublish(
                        productDTO.currentPrice(),
                        lastPublication,
                        Instant.now()
                );

                if (!publish) {
                    return;
                }

                promotionPublisher.publish(productDTO, discount);

                registerPublication(storedProduct,productDTO, discount);

            }catch (InvalidProductException e){
                log.warn("Invalid product {}", e.getMessage());

            }catch (TelegramIntegrationException e){
                log.warn("Failed to publish product {}", productDTO.externalId());
            } catch (ResourceNotFoundException e) {
                log.warn("Resource not found {}", productDTO.externalId());
            }
        });

    }

    private void registerPublication(Product product,ProductDTO productDTO, BigDecimal discount) {

        Promotion promotion = new Promotion(
                product,
                productDTO.originalPrice(),
                productDTO.currentPrice(),
                discount,
                telegramProperties.chatId()
        );

        promotionRepository.save(promotion);
    }

    private boolean shouldPublish(BigDecimal currentPrice, Optional<Promotion> lastPublication,
                                  Instant now) {

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
