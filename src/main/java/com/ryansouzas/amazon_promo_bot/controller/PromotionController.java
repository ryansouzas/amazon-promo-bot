package com.ryansouzas.amazon_promo_bot.controller;

import com.ryansouzas.amazon_promo_bot.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    public final PromotionService promotionService;


    @PostMapping("/test")
    public ResponseEntity<Void> sendTest() {

        promotionService.publishPromotion();

        return ResponseEntity.noContent().build();
    }
}
