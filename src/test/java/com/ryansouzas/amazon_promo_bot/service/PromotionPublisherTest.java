package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import com.ryansouzas.amazon_promo_bot.model.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionPublisherTest {

    private static final BigDecimal ORIGINAL_PRICE = new BigDecimal("200.00");

    private static final BigDecimal CURRENT_PRICE = new BigDecimal("100.00");

    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");
    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private PromotionPublisher promotionPublisher;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(
                "AMZ-001",
                "Mouse Gamer",
                ORIGINAL_PRICE,
                CURRENT_PRICE,
                "https://example.com/mouse"
        );
    }

    @Test
    void shouldPublishPromotionSuccessfully() {

        promotionPublisher.publish(product, DISCOUNT);

        verify(telegramService, times(1))
                .sendMessage(anyString());
    }

    @Test
    void shouldSendCorrectPromotionMessage() {

        ArgumentCaptor<String> messageCaptor =
                ArgumentCaptor.forClass(String.class);

        promotionPublisher.publish(product, DISCOUNT);

        verify(telegramService).sendMessage(
                messageCaptor.capture()
        );

        String capturedMessage = messageCaptor.getValue();

        String expectedMessage = """
                🔥 PROMOÇÃO ENCONTRADA!
                
                Produto: Mouse Gamer
                Preço original: R$ 200.00
                Preço atual: R$ 100.00
                Desconto: 50.00%
                
                Confira: https://example.com/mouse
                """;

        assertEquals(expectedMessage, capturedMessage);
    }

    @Test
    void shouldPropagateExceptionWhenTelegramFails() {

        TelegramIntegrationException expectedException =
                new TelegramIntegrationException(
                        "Failed to communicate with Telegram",
                        new RuntimeException("Connection failed")
                );

        doThrow(expectedException)
                .when(telegramService)
                .sendMessage(anyString());

        TelegramIntegrationException exception = assertThrows(
                TelegramIntegrationException.class,
                () -> promotionPublisher.publish(product, DISCOUNT)
        );

        Assertions.assertSame(expectedException, exception);

        verify(telegramService, times(1))
                .sendMessage(anyString());
    }
}

