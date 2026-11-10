package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.entities.Product;
import com.ryansouzas.amazon_promo_bot.entities.Promotion;
import com.ryansouzas.amazon_promo_bot.entities.PromotionStatus;
import com.ryansouzas.amazon_promo_bot.repositories.PromotionRepository;
import com.ryansouzas.amazon_promo_bot.service.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.source.ProductSource;
import com.ryansouzas.amazon_promo_bot.validation.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import com.ryansouzas.amazon_promo_bot.service.exceptions.PublicationInProgressException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ProductService productService;

    @Mock
    private TelegramProperties telegramProperties;

    @Mock
    private Promotion promotion;

    @InjectMocks
    private PromotionService promotionService;

    private ProductDTO validProductDTO;
    private ProductDTO invalidProductDTO;
    private Product storedProduct;

    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");
    private static final List<PromotionStatus> BLOCKING =
            List.of(PromotionStatus.PENDING, PromotionStatus.SENT);

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

        storedProduct = new Product(
                validProductDTO.externalId(),
                validProductDTO.name(),
                validProductDTO.originalPrice(),
                validProductDTO.currentPrice(),
                validProductDTO.url()
        );
        storedProduct.setId(UUID.randomUUID());


    }

    @Test
    void shouldPublishWhenValidPromotion() {


        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);


        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository.findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                storedProduct.getId(),
                "123456",
        BLOCKING))
                .thenReturn(Optional.empty());

        promotionService.publishPromotions();


        verify(productValidator).validate(validProductDTO);

        verify(promotionPublisher)
                .publish(validProductDTO, DISCOUNT);

        verify(productService).syncProduct(validProductDTO);

        verify(promotionRepository, times(2)).save(any(Promotion.class));
    }

    @Test
    void shouldNotPublishWhenProductWithoutPromotion() {

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(false);

        promotionService.publishPromotions();

        verify(productValidator).validate(validProductDTO);

        verify(promotionEvaluator, never())
                .calculateDiscount(any());

        verify(productService).syncProduct(validProductDTO);

        verifyNoInteractions(promotionPublisher);

        verifyNoInteractions(promotionRepository);
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

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.empty());

        promotionService.publishPromotions();

        verify(productValidator).validate(invalidProductDTO);
        verify(productValidator).validate(validProductDTO);

        verify(productService).syncProduct(validProductDTO);

        verify(productService, never())
                .syncProduct(invalidProductDTO);

        verify(promotionPublisher)
                .publish(validProductDTO, DISCOUNT);

        verify(promotionPublisher, never())
                .publish(eq(invalidProductDTO), any(BigDecimal.class));

        verify(promotionRepository, times(2))
                .save(any(Promotion.class));
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
        verifyNoInteractions(productService);
        verifyNoInteractions(promotionRepository);
    }

    @Test
    void shouldPublishWhenNeverPublished() {

        // Arrange
        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.empty());

        // Act
        promotionService.publishPromotions();

        // Assert
        verify(promotionPublisher, times(1))
                .publish(validProductDTO, DISCOUNT);

        verify(promotionRepository, times(2))
                .save(any(Promotion.class));
    }

    @Test
    void shouldPublishWhenPriceDecreases() {

        // Arrange
        when(promotion.getPublishedPrice())
                .thenReturn(new BigDecimal("250.00"));

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.of(promotion));

        // Act
        promotionService.publishPromotions();

        // Assert
        verify(promotionPublisher, times(1))
                .publish(validProductDTO, DISCOUNT);

        verify(promotionRepository, times(2))
                .save(any(Promotion.class));
    }

    @Test
    void shouldNotPublishWhenPriceIncreases() {

        // Arrange
        when(promotion.getPublishedPrice())
                .thenReturn(new BigDecimal("80.00"));

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.of(promotion));

        // Act
        promotionService.publishPromotions();

        // Assert
        verify(promotionPublisher, never())
                .publish(any(ProductDTO.class), any(BigDecimal.class));

        verify(promotionRepository, never())
                .save(any(Promotion.class));
    }

    @Test
    void shouldNotPublishSamePriceBefore24Hours() {

        // Arrange
        when(promotion.getPublishedPrice())
                .thenReturn(validProductDTO.currentPrice());

        when(promotion.getPublishedAt())
                .thenReturn(Instant.now().minus(Duration.ofHours(12)));

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.of(promotion));

        // Act
        promotionService.publishPromotions();

        // Assert
        verify(promotionPublisher, never())
                .publish(any(ProductDTO.class), any(BigDecimal.class));

        verify(promotionRepository, never())
                .save(any(Promotion.class));
    }

    @Test
    void shouldPublishSamePriceAfter24Hours() {

        // Arrange
        when(promotion.getPublishedPrice())
                .thenReturn(validProductDTO.currentPrice());

        when(promotion.getPublishedAt())
                .thenReturn(Instant.now().minus(Duration.ofHours(25)));

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenReturn(storedProduct);

        when(promotionEvaluator.isPromotion(validProductDTO))
                .thenReturn(true);

        when(promotionEvaluator.calculateDiscount(validProductDTO))
                .thenReturn(DISCOUNT);

        when(telegramProperties.chatId())
                .thenReturn("123456");

        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        storedProduct.getId(),
                        "123456",
                BLOCKING))
                .thenReturn(Optional.of(promotion));

        // Act
        promotionService.publishPromotions();

        // Assert
        verify(promotionPublisher, times(1))
                .publish(validProductDTO, DISCOUNT);

        verify(promotionRepository, times(2))
                .save(any(Promotion.class));
    }

    // ---------- helpers for the tests below ----------

    private void stubPromotionFlow(ProductDTO dto, Product product) {
        when(productService.syncProduct(dto)).thenReturn(product);
        when(promotionEvaluator.isPromotion(dto)).thenReturn(true);
        when(promotionEvaluator.calculateDiscount(dto)).thenReturn(DISCOUNT);
    }

    private void stubNoPreviousPublication(Product product) {
        when(promotionRepository
                .findFirstByProductIdAndTelegramChatIdAndStatusInOrderByPublishedAtDesc(
                        eq(product.getId()), eq("123456"), any()))
                .thenReturn(Optional.empty());
    }

    private List<PromotionStatus> recordStatusOnEachSave() {
        List<PromotionStatus> statuses = new ArrayList<>();
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion saved = invocation.getArgument(0);
            statuses.add(saved.getStatus());
            return saved;
        });
        return statuses;
    }

    // ---------- (a) lock ----------

    @Test
    void shouldSkipConcurrentExecutionAndQueryProductSourceOnlyOnce() throws Exception {

        CountDownLatch firstRunStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstRun = new CountDownLatch(1);

        when(productSource.findProducts()).thenAnswer(invocation -> {
            firstRunStarted.countDown();
            releaseFirstRun.await(5, TimeUnit.SECONDS);
            return List.of();
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> firstRun = executor.submit(() -> promotionService.publishPromotions());

            assertTrue(firstRunStarted.await(5, TimeUnit.SECONDS));

            assertThrows(PublicationInProgressException.class,
                    () -> promotionService.publishPromotions());

            releaseFirstRun.countDown();
            firstRun.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstRun.countDown();
            executor.shutdownNow();
        }

        verify(productSource, times(1)).findProducts();
    }

    // ---------- (b) per-product error isolation ----------

    @Test
    void shouldProcessSecondProductWhenSyncProductThrowsRuntimeExceptionOnFirst() {

        ProductDTO secondProductDTO = new ProductDTO(
                "AMZ-002",
                "Teclado Gamer",
                new BigDecimal("300.00"),
                new BigDecimal("150.00"),
                "https://example.com/teclado"
        );
        Product secondStoredProduct = new Product(
                secondProductDTO.externalId(),
                secondProductDTO.name(),
                secondProductDTO.originalPrice(),
                secondProductDTO.currentPrice(),
                secondProductDTO.url()
        );
        secondStoredProduct.setId(UUID.randomUUID());

        when(productSource.findProducts())
                .thenReturn(List.of(validProductDTO, secondProductDTO));

        when(productService.syncProduct(validProductDTO))
                .thenThrow(new RuntimeException("boom"));

        stubPromotionFlow(secondProductDTO, secondStoredProduct);
        when(telegramProperties.chatId()).thenReturn("123456");
        stubNoPreviousPublication(secondStoredProduct);

        promotionService.publishPromotions();

        verify(productService).syncProduct(validProductDTO);
        verify(productService).syncProduct(secondProductDTO);
        verify(promotionPublisher, never()).publish(eq(validProductDTO), any(BigDecimal.class));
        verify(promotionPublisher).publish(secondProductDTO, DISCOUNT);
    }

    // ---------- (c) PENDING -> SENT / PENDING -> FAILED ----------

    @Test
    void shouldSavePendingBeforePublishingAndSentAfterSuccess() {

        when(productSource.findProducts()).thenReturn(List.of(validProductDTO));
        stubPromotionFlow(validProductDTO, storedProduct);
        when(telegramProperties.chatId()).thenReturn("123456");
        stubNoPreviousPublication(storedProduct);
        List<PromotionStatus> statuses = recordStatusOnEachSave();

        promotionService.publishPromotions();

        assertEquals(List.of(PromotionStatus.PENDING, PromotionStatus.SENT), statuses);

        InOrder inOrder = inOrder(promotionRepository, promotionPublisher);
        inOrder.verify(promotionRepository).save(any(Promotion.class));
        inOrder.verify(promotionPublisher).publish(validProductDTO, DISCOUNT);
        inOrder.verify(promotionRepository).save(any(Promotion.class));
    }

    @Test
    void shouldSavePendingBeforePublishingAndFailedWhenTelegramFails() {

        when(productSource.findProducts()).thenReturn(List.of(validProductDTO));
        stubPromotionFlow(validProductDTO, storedProduct);
        when(telegramProperties.chatId()).thenReturn("123456");
        stubNoPreviousPublication(storedProduct);
        List<PromotionStatus> statuses = recordStatusOnEachSave();

        doThrow(new TelegramIntegrationException("telegram down"))
                .when(promotionPublisher).publish(validProductDTO, DISCOUNT);

        promotionService.publishPromotions();

        assertEquals(List.of(PromotionStatus.PENDING, PromotionStatus.FAILED), statuses);

        InOrder inOrder = inOrder(promotionRepository, promotionPublisher);
        inOrder.verify(promotionRepository).save(any(Promotion.class));
        inOrder.verify(promotionPublisher).publish(validProductDTO, DISCOUNT);
        inOrder.verify(promotionRepository).save(any(Promotion.class));
    }

}
