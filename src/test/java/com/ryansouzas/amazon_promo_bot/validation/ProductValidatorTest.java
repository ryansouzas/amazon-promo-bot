package com.ryansouzas.amazon_promo_bot.validation;

import com.ryansouzas.amazon_promo_bot.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ProductValidatorTest {

    private static final BigDecimal ORIGINAL_PRICE = new BigDecimal("200.00");

    private static final BigDecimal CURRENT_PRICE = new BigDecimal("100.00");

    private static final BigDecimal PRICE_ABOVE_ORIGINAL = new BigDecimal("400.00");

    private static final BigDecimal ZERO_PRICE = BigDecimal.ZERO;

    private static final BigDecimal NEGATIVE_PRICE = new BigDecimal("-100.00");

    private ProductValidator validator;
    private Product validProduct;


    @BeforeEach
    public void setUp(){
        validator = new ProductValidator();

        validProduct = new Product(
                "AMZ-001",
                "Mouse Gamer",
                ORIGINAL_PRICE,
                CURRENT_PRICE,
                "https://example.com/mouse"
        );

    }

    @Test
    void shouldValidateValidProductSuccessfully() {
        assertDoesNotThrow(() ->
                validator.validate(validProduct)
        );
    }

    @Test
    void shouldRejectNullProduct() {
        InvalidProductException exception =
                assertThrows(InvalidProductException.class,
                        () -> validator.validate(null));

        assertEquals("Product cannot be null", exception.getMessage());
    }

    @Test
    void shouldRejectBlankExternalId() {
        String [] invalidExternalIds = {null, "", " ", "  "};

        for (String externalId : invalidExternalIds) {
            Product product = new Product(
                    externalId,
                    validProduct.name(),
                    validProduct.originalPrice(),
                    validProduct.currentPrice(),
                    validProduct.url()
            );

            assertThrows(InvalidProductException.class,
                    () -> validator.validate(product));
        }


    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankName(String name) {
        Product product = new Product(
                validProduct.externalId(),
                name,
                validProduct.originalPrice(),
                validProduct.currentPrice(),
                validProduct.url()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectNullOriginalPrice() {
        Product product = productWithPrices(
                null, validProduct.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));

    }

    @Test
    void shouldRejectNullCurrentPrice() {
        Product product = productWithPrices(
               validProduct.originalPrice(), null
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectZeroOriginalPrice() {
        Product product = productWithPrices(
                ZERO_PRICE, validProduct.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectZeroCurrentPrice() {
        Product product = productWithPrices(
                validProduct.originalPrice(), ZERO_PRICE
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectNegativeOriginalPrice() {
        Product product = productWithPrices(
                NEGATIVE_PRICE, validProduct.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectNegativeCurrentPrice() {
        Product product = productWithPrices(
                validProduct.originalPrice(), NEGATIVE_PRICE
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));
    }

    @Test
    void shouldRejectCurrentPriceGreaterThanOriginal() {
        Product product = productWithPrices(
                validProduct.originalPrice(), PRICE_ABOVE_ORIGINAL
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(product));

    }

    @Test
    void shouldAcceptEqualPrices() {
        BigDecimal equalPrice = validProduct.originalPrice();

        Product product = productWithPrices(
                validProduct.originalPrice(), equalPrice
        );

        assertEquals( validProduct.originalPrice(), product.currentPrice());
        assertDoesNotThrow(() -> validator.validate(product));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankUrl(String url) {
        Product product = productWithUrl(url);

        assertThrows(InvalidProductException.class, () ->
                validator.validate(product));
    }

    @Test
    void shouldAcceptValidHttpsUrl() {
        Product product = productWithUrl("https://example.com/mouse");
        assertEquals( validProduct.url(), product.url());

        assertDoesNotThrow(() -> validator.validate(product));
    }

    @Test
    void shouldRejectHttpUrl() {
        Product product = productWithUrl("http://example.com/mouse");

        assertThrows(InvalidProductException.class, () ->
                validator.validate(product));
    }

    @Test
    void shouldRejectMalformedUrl() {
        Product product = productWithUrl("Https://exa mpl.com/mouse");

        assertThrows(InvalidProductException.class, () ->
                validator.validate(product));
    }

    @Test
    void shouldRejectHttpsUrlWithoutHost() {
        Product product = productWithUrl("https:/");
        assertThrows(InvalidProductException.class, () ->
                validator.validate(product));
    }

    private Product productWithPrices(BigDecimal original, BigDecimal current) {
        return new Product(
                validProduct.externalId(),
                validProduct.name(),
                original,
                current,
                validProduct.url()
        );
    }

    private Product productWithUrl(String url) {
        return new Product(
                validProduct.externalId(),
                validProduct.name(),
                validProduct.originalPrice(),
                validProduct.currentPrice(),
                url
        );
    }
}
