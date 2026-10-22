package com.ryansouzas.amazon_promo_bot.validation;

import com.ryansouzas.amazon_promo_bot.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
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
    private ProductDTO validProductDTO;


    @BeforeEach
    public void setUp(){
        validator = new ProductValidator();

        validProductDTO = new ProductDTO(
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
                validator.validate(validProductDTO)
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
            ProductDTO productDTO = new ProductDTO(
                    externalId,
                    validProductDTO.name(),
                    validProductDTO.originalPrice(),
                    validProductDTO.currentPrice(),
                    validProductDTO.url()
            );

            assertThrows(InvalidProductException.class,
                    () -> validator.validate(productDTO));
        }


    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankName(String name) {
        ProductDTO productDTO = new ProductDTO(
                validProductDTO.externalId(),
                name,
                validProductDTO.originalPrice(),
                validProductDTO.currentPrice(),
                validProductDTO.url()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectNullOriginalPrice() {
        ProductDTO productDTO = productWithPrices(
                null, validProductDTO.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));

    }

    @Test
    void shouldRejectNullCurrentPrice() {
        ProductDTO productDTO = productWithPrices(
               validProductDTO.originalPrice(), null
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectZeroOriginalPrice() {
        ProductDTO productDTO = productWithPrices(
                ZERO_PRICE, validProductDTO.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectZeroCurrentPrice() {
        ProductDTO productDTO = productWithPrices(
                validProductDTO.originalPrice(), ZERO_PRICE
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectNegativeOriginalPrice() {
        ProductDTO productDTO = productWithPrices(
                NEGATIVE_PRICE, validProductDTO.currentPrice()
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectNegativeCurrentPrice() {
        ProductDTO productDTO = productWithPrices(
                validProductDTO.originalPrice(), NEGATIVE_PRICE
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectCurrentPriceGreaterThanOriginal() {
        ProductDTO productDTO = productWithPrices(
                validProductDTO.originalPrice(), PRICE_ABOVE_ORIGINAL
        );

        assertThrows(InvalidProductException.class,
                () -> validator.validate(productDTO));

    }

    @Test
    void shouldAcceptEqualPrices() {
        BigDecimal equalPrice = validProductDTO.originalPrice();

        ProductDTO productDTO = productWithPrices(
                validProductDTO.originalPrice(), equalPrice
        );

        assertEquals( validProductDTO.originalPrice(), productDTO.currentPrice());
        assertDoesNotThrow(() -> validator.validate(productDTO));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectBlankUrl(String url) {
        ProductDTO productDTO = productWithUrl(url);

        assertThrows(InvalidProductException.class, () ->
                validator.validate(productDTO));
    }

    @Test
    void shouldAcceptValidHttpsUrl() {
        ProductDTO productDTO = productWithUrl("https://example.com/mouse");
        assertEquals( validProductDTO.url(), productDTO.url());

        assertDoesNotThrow(() -> validator.validate(productDTO));
    }

    @Test
    void shouldRejectHttpUrl() {
        ProductDTO productDTO = productWithUrl("http://example.com/mouse");

        assertThrows(InvalidProductException.class, () ->
                validator.validate(productDTO));
    }

    @Test
    void shouldRejectMalformedUrl() {
        ProductDTO productDTO = productWithUrl("Https://exa mpl.com/mouse");

        assertThrows(InvalidProductException.class, () ->
                validator.validate(productDTO));
    }

    @Test
    void shouldRejectHttpsUrlWithoutHost() {
        ProductDTO productDTO = productWithUrl("https:/");
        assertThrows(InvalidProductException.class, () ->
                validator.validate(productDTO));
    }

    private ProductDTO productWithPrices(BigDecimal original, BigDecimal current) {
        return new ProductDTO(
                validProductDTO.externalId(),
                validProductDTO.name(),
                original,
                current,
                validProductDTO.url()
        );
    }

    private ProductDTO productWithUrl(String url) {
        return new ProductDTO(
                validProductDTO.externalId(),
                validProductDTO.name(),
                validProductDTO.originalPrice(),
                validProductDTO.currentPrice(),
                url
        );
    }
}
