package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.config.TelegramProperties;
import com.ryansouzas.amazon_promo_bot.exceptions.TelegramIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class TelegramServiceTest {

    private static final String BOT_TOKEN = "test-token";
    private static final String CHAT_ID = "-123";
    private static final String MESSAGE = "Mouse Gamer em promoção!";

    private TelegramProperties telegramProperties;
    private TelegramService telegramService;
    private MockRestServiceServer mockServer;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties(BOT_TOKEN, CHAT_ID);
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        telegramService = new TelegramService(builder, telegramProperties);
    }

    @Test
    void shouldSendMessageSuccessfully() {

        String message = "Mouse Gamer em promoção!";

        mockServer.expect(requestTo("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        assertDoesNotThrow(() ->
                telegramService.sendMessage(message)
        );

        mockServer.verify();
    }


    @Test
    void shouldSendCorrectChatId() {

        mockServer.expect(requestTo("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage"))
                .andExpect(content().json("""
        {
          "chat_id": "-123"
        }
        """))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        telegramService.sendMessage(MESSAGE);

        mockServer.verify();
    }

    @Test
    void shouldSendCorrectMessageText() {

        mockServer.expect(requestTo("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage"))
                .andExpect(content().json("""
        {
          "text": "Mouse Gamer em promoção!"
        }
        """))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        telegramService.sendMessage(MESSAGE);

        mockServer.verify();

    }

    @Test
    void shouldThrowExceptionWhenTelegramReturnsBadRequest() {
        mockServer.expect(requestTo("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThrows(TelegramIntegrationException.class, () -> telegramService.sendMessage(MESSAGE));

        mockServer.verify();
    }


}
