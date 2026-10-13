package com.ryansouzas.amazon_promo_bot.controller;

import com.ryansouzas.amazon_promo_bot.service.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final TelegramService telegramService;

    public PromotionController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping("/test")
    public ResponseEntity<Void> sendTest() {

        String message = """
                OFERTA DE TESTE!

                Produto: Mouse Gamer
                Preço: R$ 99,90
                Desconto: 35%

                Mensagem enviada pelo Spring Boot!
                """;

        telegramService.sendMessage(message);

        return ResponseEntity.noContent().build();
    }
}
