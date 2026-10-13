package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.model.Product;
import com.ryansouzas.amazon_promo_bot.source.FakeProductSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {

    private static final BigDecimal MIN_DISCOUNT = new BigDecimal("15");

    private final FakeProductSource productSource;
    private final TelegramService telegramService;

    public void publishPromotion(){
        List<Product> products = productSource.findProducts();
        products.forEach(product -> {
            BigDecimal discount = calculateDiscount(product);
            if (discount.compareTo(MIN_DISCOUNT) >= 0){

                String message = formatMessage(product, discount);
                log.info(message);
                telegramService.sendMessage(message);
            }
        });
    }

    private BigDecimal calculateDiscount(Product product){
        BigDecimal difference = product.originalPrice().subtract(product.currentPrice());

        return difference
                .multiply(new BigDecimal(100))
                .divide(product.originalPrice(), 2, RoundingMode.HALF_UP);
    }

    private String formatMessage(
            Product product,
            BigDecimal discount
    ) {
        return """
                PROMOÇÃO ENCONTRADA!

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
