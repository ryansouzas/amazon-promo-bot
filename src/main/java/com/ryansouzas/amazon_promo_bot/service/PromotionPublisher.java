package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PromotionPublisher {

    private final TelegramService telegramService;

    public void publish(ProductDTO productDTO, BigDecimal discount) {

        String message = formatMessage(productDTO, discount);

        telegramService.sendMessage(message);
    }

    private String formatMessage(
            ProductDTO productDTO,
            BigDecimal discount
    ) {
        return """
                🔥 PROMOÇÃO ENCONTRADA!

                Produto: %s
                Preço original: R$ %s
                Preço atual: R$ %s
                Desconto: %s%%

                Confira: %s
                """.formatted(
                productDTO.name(),
                productDTO.originalPrice(),
                productDTO.currentPrice(),
                discount,
                productDTO.url()
        );
    }
}
