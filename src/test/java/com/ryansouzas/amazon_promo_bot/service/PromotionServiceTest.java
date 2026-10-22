package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
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

    private ProductDTO validProductDTO;
    private ProductDTO invalidProductDTO;

    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");

    @BeforeEach
    void setUp() {

        validProductDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer",
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                "https://example.com/mouse"
        );

        invalidProductDTO = new ProductDTO(
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
                .thenReturn(List.of(validProductDTO));

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);


        promotionService.publishPromotions();


        verify(productValidator).validate(validProductDTO);

        verify(promotionPublisher)
                .publish(validProductDTO, DISCOUNT);
    }

    @Test
    void shouldNotPublishProductWithoutPromotion() {

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(false);

        promotionService.publishPromotions();

        verify(productValidator).validate(validProductDTO);

        verify(promotionEvaluator, never())
                .calculateDiscount(any());

        verifyNoInteractions(promotionPublisher);
    }

    @Test
    void shouldSkipInvalidProduct() {

        when(productSource.findProducts())
                .thenReturn(List.of(invalidProductDTO));

        doThrow(new InvalidProductException("Invalid product"))
                .when(productValidator)
                .validate(invalidProductDTO);

        promotionService.publishPromotions();

        verify(productValidator).validate(invalidProductDTO);

        verifyNoInteractions(promotionEvaluator);
        verifyNoInteractions(promotionPublisher);
    }

    @Test
    void shouldContinueProcessingAfterInvalidProduct() {

        when(productSource.findProducts())
                .thenReturn(List.of(invalidProductDTO, validProductDTO));

        doThrow(new InvalidProductException("Invalid product"))
                .when(productValidator)
                .validate(invalidProductDTO);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        promotionService.publishPromotions();

        verify(productValidator).validate(invalidProductDTO);
        verify(productValidator).validate(validProductDTO);

        verify(promotionPublisher)
                .publish(validProductDTO, DISCOUNT);

        verify(promotionPublisher, never())
                .publish(eq(invalidProductDTO), any(BigDecimal.class));
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
