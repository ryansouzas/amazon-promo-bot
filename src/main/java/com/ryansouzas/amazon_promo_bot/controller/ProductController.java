package com.ryansouzas.amazon_promo_bot.controller;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{externalId}")
    public ResponseEntity<ProductDTO> findByExternalId(@PathVariable String externalId) {
        return productService.findByExternalId(externalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {

        ProductDTO created = productService.createProduct(productDTO);
        URI location = URI.create("/products/" + created.externalId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable String externalId, @RequestBody ProductDTO productDTO) {

        if (!externalId.equals(productDTO.externalId())) {
            return ResponseEntity.badRequest().build();
        }
        ProductDTO updated = productService.updateProduct(productDTO);
        return ResponseEntity.ok(updated);
    }
}