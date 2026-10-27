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
import java.util.List;

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
        List<ProductDTO> productDTOS = productSource.findProducts();
        productDTOS.forEach(product -> {
            try {
                productValidator.validate(product);

                if (!promotionEvaluator.isPromotion(product)) {
                    return;
                }

                BigDecimal discount = promotionEvaluator.calculateDiscount(product);

                promotionPublisher.publish(product, discount);

                registerPublication(product, discount);

            }catch (InvalidProductException e){
                log.warn("Invalid product {}", e.getMessage());

            }catch (TelegramIntegrationException e){
                log.warn("Failed to publish product {}", product.externalId());
            }
        });

    }

    private void registerPublication(ProductDTO productDTO, BigDecimal discount) {

        Product product = productRepository
                .findByExternalId(productDTO.externalId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Promotion promotion = new Promotion(
                product,
                product.getOriginalPrice(),
                product.getCurrentPrice(),
                discount,
                telegramProperties.chatId()
        );

        promotionRepository.save(promotion);
    }


}
