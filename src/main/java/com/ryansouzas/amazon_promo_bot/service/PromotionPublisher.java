package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PromotionPublisher {

    private final TelegramService telegramService;

    public void publish(Product product, BigDecimal discount) {

        String message = formatMessage(product, discount);

        telegramService.sendMessage(message);
    }

    private String formatMessage(
            Product product,
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
                product.name(),
                product.originalPrice(),
                product.currentPrice(),
                discount,
                product.url()
        );
    }
}
