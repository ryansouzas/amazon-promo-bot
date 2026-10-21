package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.model.Product;
import com.ryansouzas.amazon_promo_bot.source.ProductSource;
import com.ryansouzas.amazon_promo_bot.validation.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private ProductSource productSource;

    @Mock
    private ProductValidator productValidator;

    @Mock
    private PromotionEvaluator promotionEvaluator;

    @Mock
    private PromotionPublisher promotionPublisher;

    @InjectMocks
    private PromotionService promotionService;

    private Product validProduct;
    private Product invalidProduct;

    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");

    @BeforeEach
    void setUp() {

        validProduct = new Product(
                "AMZ-001",
                "Mouse Gamer",
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                "https://example.com/mouse"
        );

        invalidProduct = new Product(
                "",
                "Produto Inválido",
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                "https://example.com/invalid"
        );

    }

    @Test
    void shouldPublishValidPromotion() {


        when(productSource.findProducts())
                .thenReturn(List.of(validProduct));

        when(promotionEvaluator.isPromotion(validProduct))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProduct))
                .thenReturn(DISCOUNT);


        promotionService.publishPromotions();


        verify(productValidator).validate(validProduct);

        verify(promotionPublisher)
                .publish(validProduct, DISCOUNT);
    }

    @Test
    void shouldNotPublishProductWithoutPromotion() {

        when(productSource.findProducts())
                .thenReturn(List.of(validProduct));

        when(promotionEvaluator.isPromotion(validProduct))
                .thenReturn(false);

        promotionService.publishPromotions();

        verify(productValidator).validate(validProduct);

        verify(promotionEvaluator, never())
                .calculateDiscount(any());

        verifyNoInteractions(promotionPublisher);
    }

    @Test
    void shouldSkipInvalidProduct() {

        when(productSource.findProducts())
                .thenReturn(List.of(invalidProduct));

        doThrow(new InvalidProductException("Invalid product"))
                .when(productValidator)
                .validate(invalidProduct);

        promotionService.publishPromotions();

        verify(productValidator).validate(invalidProduct);

        verifyNoInteractions(promotionEvaluator);
        verifyNoInteractions(promotionPublisher);
    }

    @Test
    void shouldContinueProcessingAfterInvalidProduct() {

        when(productSource.findProducts())
                .thenReturn(List.of(invalidProduct, validProduct));

        doThrow(new InvalidProductException("Invalid product"))
                .when(productValidator)
                .validate(invalidProduct);

        when(promotionEvaluator.isPromotion(validProduct))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProduct))
                .thenReturn(DISCOUNT);

        promotionService.publishPromotions();

        verify(productValidator).validate(invalidProduct);
        verify(productValidator).validate(validProduct);

        verify(promotionPublisher)
                .publish(validProduct, DISCOUNT);

        verify(promotionPublisher, never())
                .publish(eq(invalidProduct), any(BigDecimal.class));
    }


    @Test
    void shouldHandleEmptyProductList() {


        when(productSource.findProducts())
                .thenReturn(List.of());


        promotionService.publishPromotions();


        verify(productSource).findProducts();

        verifyNoInteractions(productValidator);
        verifyNoInteractions(promotionEvaluator);
        verifyNoInteractions(promotionPublisher);
    }


}
