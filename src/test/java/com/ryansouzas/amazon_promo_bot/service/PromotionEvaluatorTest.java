package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.model.Product;
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
    private Product product;

    @BeforeEach
    void setUp() {
        promotionEvaluator = new PromotionEvaluator();

        product = new Product(
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
                promotionEvaluator.isPromotion(product)
        );

    }

    @Test
    void shouldAcceptDiscountEqualToMinimum() {

       Product product = productWithPrices(ORIGINAL_PRICE, CURRENT_PRICE_AT_MINIMUM_DISCOUNT);

        assertTrue(() ->
                promotionEvaluator.isPromotion(product)
        );
    }

    @Test
    void shouldRejectDiscountBelowMinimum() {

        Product product = productWithPrices(ORIGINAL_PRICE, CURRENT_PRICE_BELOW_MINIMUM_DISCOUNT);

        assertFalse(() ->
                promotionEvaluator.isPromotion(product)
        );
    }

    @Test
    void shouldRejectProductWithoutDiscount() {

        Product productWithoutDiscount = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_WITHOUT_DISCOUNT
        );

        assertFalse(promotionEvaluator.isPromotion(productWithoutDiscount));
    }

    @Test
    void shouldAcceptDiscountJustAboveMinimum() {

        Product promotionalProduct = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_JUST_ABOVE_MINIMUM_DISCOUNT
        );

        assertTrue(promotionEvaluator.isPromotion(promotionalProduct));
    }

    @Test
    void shouldRejectDiscountJustBelowMinimum() {

        Product promotionalProduct = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_JUST_BELOW_MINIMUM_DISCOUNT
        );

        assertFalse(promotionEvaluator.isPromotion(promotionalProduct));
    }

    @Test
    void shouldCalculateFiftyPercentDiscount() {

        Product promotionalProduct = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("50.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProduct);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateFifteenPercentDiscount() {

        Product promotionalProduct = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_AT_MINIMUM_DISCOUNT
        );

        BigDecimal expectedDiscount = new BigDecimal("15.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProduct);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateZeroPercentDiscount() {

        Product productWithoutDiscount = productWithPrices(
                ORIGINAL_PRICE,
                CURRENT_PRICE_WITHOUT_DISCOUNT
        );

        BigDecimal expectedDiscount = new BigDecimal("0.00");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(productWithoutDiscount);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldCalculateDiscountWithDecimalPrices() {

        Product promotionalProduct = productWithPrices(
                DECIMAL_ORIGINAL_PRICE,
                DECIMAL_CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("25.01");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProduct);

        assertEquals(expectedDiscount, actualDiscount);
    }

    @Test
    void shouldRoundDiscountToTwoDecimalPlaces() {

        Product promotionalProduct = productWithPrices(
                ROUNDING_ORIGINAL_PRICE,
                ROUNDING_CURRENT_PRICE
        );

        BigDecimal expectedDiscount = new BigDecimal("33.34");

        BigDecimal actualDiscount =
                promotionEvaluator.calculateDiscount(promotionalProduct);

        assertEquals(expectedDiscount, actualDiscount);
    }

    private Product productWithPrices(BigDecimal original, BigDecimal current) {
        return new Product(
                product.externalId(),
                product.name(),
                original,
                current,
                product.url()
        );
    }
}
