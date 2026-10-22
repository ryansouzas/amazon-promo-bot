package com.ryansouzas.amazon_promo_bot.mapper;

import com.ryansouzas.amazon_promo_bot.entities.ProductEntity;
import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductEntity toEntity(ProductDTO productDTO) {
        return new ProductEntity(
                productDTO.externalId(),
                productDTO.name(),
                productDTO.originalPrice(),
                productDTO.currentPrice(),
                productDTO.url()
        );
    }

    public ProductDTO toDTO(ProductEntity entity) {
        return new ProductDTO(
                entity.getExternalId(),
                entity.getName(),
                entity.getOriginalPrice(),
                entity.getCurrentPrice(),
                entity.getUrl()
        );
    }
}
