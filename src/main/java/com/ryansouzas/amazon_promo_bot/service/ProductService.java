package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.entities.PriceHistoryEntity;
import com.ryansouzas.amazon_promo_bot.entities.ProductEntity;
import com.ryansouzas.amazon_promo_bot.service.exceptions.ProductExistsException;
import com.ryansouzas.amazon_promo_bot.service.exceptions.ResourceNotFoundException;
import com.ryansouzas.amazon_promo_bot.mapper.ProductMapper;
import com.ryansouzas.amazon_promo_bot.repositories.PriceHistoryRepository;
import com.ryansouzas.amazon_promo_bot.repositories.ProductRepository;
import com.ryansouzas.amazon_promo_bot.validation.ProductValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductMapper productMapper;
    private final ProductValidator validator;

    // Busca um produto pelo identificador externo
    @Transactional(readOnly = true)
    public Optional<ProductDTO> findByExternalId(String externalId) {
        return productRepository.findByExternalId(externalId)
                .map(productMapper::toDTO);
    }

    // Valida e cadastra um novo produto, registrando seu histórico inicial
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        validator.validate(productDTO);
        if (productRepository.findByExternalId(productDTO.externalId()).isPresent()) {
            throw new ProductExistsException("Product Already Exists");
        }

        ProductEntity entity = productMapper.toEntity(productDTO);
        ProductEntity saved = productRepository.save(entity);
        savePriceHistory(saved);

        return productMapper.toDTO(saved);
    }

    // Valida e localiza um produto existente para atualização
    @Transactional
    public ProductDTO updateProduct(ProductDTO productDTO) {

        validator.validate(productDTO);

        ProductEntity entity = productRepository
                .findByExternalId(productDTO.externalId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(productDTO.externalId())
                );

        updateExistingProduct(entity, productDTO);

        return productMapper.toDTO(entity);
    }

    // Atualiza os dados e registra histórico se houver alteração de preço
    private void updateExistingProduct(
            ProductEntity entity,
            ProductDTO productDTO) {

        boolean priceChanged =
                entity.getOriginalPrice()
                        .compareTo(productDTO.originalPrice()) != 0
                        ||
                        entity.getCurrentPrice()
                                .compareTo(productDTO.currentPrice()) != 0;

        entity.setName(productDTO.name());
        entity.setOriginalPrice(productDTO.originalPrice());
        entity.setCurrentPrice(productDTO.currentPrice());
        entity.setUrl(productDTO.url());

        if (priceChanged) {
            savePriceHistory(entity);
        }
    }

    // Registra os preços atuais no histórico
    private void savePriceHistory(ProductEntity entity) {

        PriceHistoryEntity history = new PriceHistoryEntity(
                entity,
                entity.getOriginalPrice(),
                entity.getCurrentPrice()
        );

        priceHistoryRepository.save(history);
    }
}