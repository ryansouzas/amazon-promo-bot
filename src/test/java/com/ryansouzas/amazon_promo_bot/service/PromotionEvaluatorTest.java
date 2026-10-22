package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PromotionEvaluatorTest {

    private static final BigDecimal ORIGINAL_PRICE = new BigDecimal("200.00");

    private static final BigDecimal CURRENT_PRICE = new BigDecimal("100.00");

    private static final BigDecimal CURRENT_PRICE_AT_MINIMUM_DISCOUNT = new BigDecimal("170.00");

    private static final BigDecimal CURRENT_PRICE_WITHOUT_DISCOUNT = new BigDecimal("200.00");

    private static final BigDecimal CURRENT_PRICE_BELOW_MINIMUM_DISCOUNT = new BigDecimal("195.00");

    private static final BigDecimal CURRENT_PRICE_JUST_ABOVE_MINIMUM_DISCOUNT = new BigDecimal("169.98");

    private static final BigDecimal CURRENT_PRICE_JUST_BELOW_MINIMUM_DISCOUNT = new BigDecimal("170.02");

    private static final BigDecimal DECIMAL_ORIGINAL_PRICE = new BigDecimal("199.90");

    private static final BigDecimal DECIMAL_CURRENT_PRICE = new BigDecimal("149.90");

    private static final BigDecimal ROUNDING_ORIGINAL_PRICE = new BigDecimal("300.00");

    private static final BigDecimal ROUNDING_CURRENT_PRICE = new BigDecimal("199.99");

    private PromotionEvaluator promotionEvaluator;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        promotionEvaluator = new PromotionEvaluator();

        productDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer",
                ORIGINAL_PRICE,
                CURRENT_PRICE,
                "https://example.com/mouse"
        );
    }

    @Test
    void shouldAcceptDiscountAboveMinimum() {
        assertTrue(() ->
                promotionEvaluator.isPromotion(productDTO)
        );

    }

    @Test
    void shouldAcceptDiscountEqualToMinimum() {

       ProductDTO productDTO = productWithPrices(ORIGINAL_PRICE, CURRENT_PRICE_AT_MINIMUM_DISCOUNT);

        assertTrue(() ->
                promotionEvaluator.isPromotion(productDTO)
        );
    }

    @Test
    void shouldRejectDiscountBelowMinimum() {

        ProductDTO productDTO = productWithPrices(ORIGINAL_PRICE, CURRENT_PRICE_BELOW_MINIMUM_DISCOUNT);

        assertFalse(() ->
                promotionEvaluator.isPromotion(productDTO)
        );
    }

    @Test
    void shouldRejectProductWithoutDiscount() {

        ProductDTO productDTOWithoutDiscount = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_WITHOUT_DISCOUNT
        );

        assertFalse(promotionEvaluator.isPromotion(productDTOWithoutDiscount));
    }

    @Test
    void shouldAcceptDiscountJustAboveMinimum() {

        ProductDTO promotionalProductDTO = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_JUST_ABOVE_MINIMUM_DISCOUNT
        );

        assertTrue(promotionEvaluator.isPromotion(promotionalProductDTO));
    }

    @Test
    void shouldRejectDiscountJustBelowMinimum() {

        ProductDTO promotionalProductDTO = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_JUST_BELOW_MINIMUM_DISCOUNT
        );

        assertFalse(promotionEvaluator.isPromotion(promotionalProductDTO));
    }

    @Test
    void shouldCalculateFiftyPercentDiscount() {

        ProductDTO promotionalProductDTO = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("50.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProductDTO);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateFifteenPercentDiscount() {

        ProductDTO promotionalProductDTO = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_AT_MINIMUM_DISCOUNT
        );

        BigDecimal expectedDiscount = new BigDecimal("15.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProductDTO);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateZeroPercentDiscount() {

        ProductDTO productDTOWithoutDiscount = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_WITHOUT_DISCOUNT
        );

        BigDecimal expectedDiscount = new BigDecimal("0.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(productDTOWithoutDiscount);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateDiscountWithDecimalPrices() {

        ProductDTO promotionalProductDTO = productWithPrices(
                DECIMAL_ORIGINAL_PRICE,
                DECIMAL_CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("25.01");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProductDTO);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldRoundDiscountToTwoDecimalPlaces() {

        ProductDTO promotionalProductDTO = productWithPrices(
                ROUNDING_ORIGINAL_PRICE,
                ROUNDING_CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("33.34");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProductDTO);

        assertEquals(expectedDiscount, actualDiscount);
    }

    private ProductDTO productWithPrices(BigDecimal original, BigDecimal current) {
        return new ProductDTO(
                productDTO.externalId(),
                productDTO.name(),
                original,
                current,
                productDTO.url()
        );
    }
}
