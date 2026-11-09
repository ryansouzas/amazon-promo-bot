package com.ryansouzas.amazon_promo_bot.service;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import com.ryansouzas.amazon_promo_bot.entities.PriceHistory;
import com.ryansouzas.amazon_promo_bot.entities.Product;
import com.ryansouzas.amazon_promo_bot.mapper.ProductMapper;
import com.ryansouzas.amazon_promo_bot.repositories.PriceHistoryRepository;
import com.ryansouzas.amazon_promo_bot.repositories.ProductRepository;
import com.ryansouzas.amazon_promo_bot.service.exceptions.InvalidProductException;
import com.ryansouzas.amazon_promo_bot.service.exceptions.ProductExistsException;
import com.ryansouzas.amazon_promo_bot.service.exceptions.ResourceNotFoundException;
import com.ryansouzas.amazon_promo_bot.validation.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductValidator validator;

    private ProductDTO validProductDTO;
    private ProductDTO invalidProductDTO;
    private Product storedProduct;

    @BeforeEach
    void setUp(){
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

    // findByExternalId
    @Test
    void findByExternalIdShouldReturnDtoWhenProductExists() {
        // Arrange (preparação): define o identificador buscado e a entidade que o repositório deve devolver
        // Ensina os mocks: o repositório encontra o produto e o mapper converte a entidade para DTO
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));
        when(productMapper.toDTO(storedProduct))
                .thenReturn(validProductDTO);

        Optional<ProductDTO> result = productService.findByExternalId(validProductDTO.externalId());

        assertTrue(result.isPresent(), "O produto existente deveria ser encontrado");
        assertEquals(validProductDTO, result.get());

        verify(productRepository).findByExternalId(validProductDTO.externalId());
    }

    @Test
    void findByExternalIdShouldReturnEmptyWhenProductDoesNotExist() {
        // Arrange: o repositório não encontra nenhum produto com o identificador buscado
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.empty());

        // Act: executa a busca
        Optional<ProductDTO> result = productService.findByExternalId(validProductDTO.externalId());

        // Assert: o resultado deve estar vazio e o mapper não deve ser chamado
        assertFalse(result.isPresent(), "Nenhum produto deveria ser retornado");
        verify(productRepository).findByExternalId(validProductDTO.externalId());
        verify(productMapper, never()).toDTO(any());
    }

    // createProduct
    @Test
    void createProductShouldSaveProductAndInitialPriceHistoryWhenExternalIdIsNew() {
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.empty());
        when(productMapper.toEntity(validProductDTO))
                .thenReturn(storedProduct);
        when(productRepository.save(storedProduct))
                .thenReturn(storedProduct);
        when(productMapper.toDTO(storedProduct))
                .thenReturn(validProductDTO);

        productService.createProduct(validProductDTO);

        verify(productRepository).save(storedProduct);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        PriceHistory history = captor.getValue();
        assertSame(storedProduct, history.getProduct());
        assertEquals(validProductDTO.originalPrice(), history.getOriginalPrice());
        assertEquals(validProductDTO.currentPrice(), history.getCurrentPrice());
    }

    @Test
    void createProductShouldThrowProductExistsExceptionWhenExternalIdAlreadyExists() {
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));

        assertThrows(ProductExistsException.class,
                () -> productService.createProduct(validProductDTO));

        verify(productRepository, never()).save(any());
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void createProductShouldThrowInvalidProductExceptionWhenValidationFails() {
        doThrow(new InvalidProductException("Product external ID is required"))
                .when(validator).validate(invalidProductDTO);

        assertThrows(InvalidProductException.class,
                () -> productService.createProduct(invalidProductDTO));

        // Nenhuma consulta ou gravação deve acontecer após a falha de validação
        verify(productRepository, never()).findByExternalId(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(priceHistoryRepository);
    }


    // updateProduct
    @Test
    void updateProductShouldUpdateFieldsWithoutPriceHistoryWhenPricesAreUnchanged() {
        ProductDTO updatedDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer Novo",
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                "https://example.com/mouse-novo"
        );

        when(productRepository.findByExternalId(updatedDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));
        when(productMapper.toDTO(storedProduct))
                .thenReturn(updatedDTO);

        productService.updateProduct(updatedDTO);

        assertEquals("Mouse Gamer Novo", storedProduct.getName());
        assertEquals("https://example.com/mouse-novo", storedProduct.getUrl());
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    void updateProductShouldSavePriceHistoryWhenOriginalPriceChanges() {
        ProductDTO updatedDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer",
                new BigDecimal("250.00"),
                new BigDecimal("100.00"),
                "https://example.com/mouse"
        );

        when(productRepository.findByExternalId(updatedDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));
        when(productMapper.toDTO(storedProduct))
                .thenReturn(updatedDTO);

        productService.updateProduct(updatedDTO);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        assertEquals(new BigDecimal("250.00"), captor.getValue().getOriginalPrice());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getCurrentPrice());
    }

    @Test
    void updateProductShouldSavePriceHistoryWhenCurrentPriceChanges() {
        ProductDTO updatedDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer",
                new BigDecimal("200.00"),
                new BigDecimal("90.00"),
                "https://example.com/mouse"
        );

        when(productRepository.findByExternalId(updatedDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));
        when(productMapper.toDTO(storedProduct))
                .thenReturn(updatedDTO);

        productService.updateProduct(updatedDTO);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        assertEquals(new BigDecimal("200.00"), captor.getValue().getOriginalPrice());
        assertEquals(new BigDecimal("90.00"), captor.getValue().getCurrentPrice());
    }


    @Test
    void updateProductShouldThrowResourceNotFoundExceptionWhenProductDoesNotExist() {
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(validProductDTO));

        verify(productRepository, never()).save(any());
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    void updateProductShouldThrowInvalidProductExceptionWithoutQueryingRepositoryWhenValidationFails() {
        doThrow(new InvalidProductException("Product external ID is required"))
                .when(validator).validate(invalidProductDTO);

        assertThrows(InvalidProductException.class,
                () -> productService.updateProduct(invalidProductDTO));

        verifyNoInteractions(productRepository, priceHistoryRepository);
    }

    // syncProduct
    @Test
    void syncProductShouldCreateProductAndPriceHistoryWhenProductDoesNotExist() {
        when(productRepository.findByExternalId(validProductDTO.externalId()))
                .thenReturn(Optional.empty());
        when(productMapper.toEntity(validProductDTO))
                .thenReturn(storedProduct);
        when(productRepository.save(storedProduct))
                .thenReturn(storedProduct);

        Product result = productService.syncProduct(validProductDTO);

        assertSame(storedProduct, result);
        verify(productRepository).save(storedProduct);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        assertSame(storedProduct, captor.getValue().getProduct());
        assertEquals(validProductDTO.currentPrice(), captor.getValue().getCurrentPrice());
    }

    @Test
    void syncProductShouldUpdateExistingProductAndSavePriceHistoryWhenPriceChanges() {
        ProductDTO updatedDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer",
                new BigDecimal("200.00"),
                new BigDecimal("80.00"),
                "https://example.com/mouse"
        );

        when(productRepository.findByExternalId(updatedDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));

        Product result = productService.syncProduct(updatedDTO);

        assertSame(storedProduct, result);
        assertEquals(new BigDecimal("80.00"), storedProduct.getCurrentPrice());
        verify(productMapper, never()).toEntity(any());
        verify(productRepository, never()).save(any());

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertEquals(new BigDecimal("80.00"), captor.getValue().getCurrentPrice());
    }

    @Test
    void syncProductShouldUpdateExistingProductWithoutPriceHistoryWhenPricesAreUnchanged() {
        ProductDTO updatedDTO = new ProductDTO(
                "AMZ-001",
                "Mouse Gamer Novo",
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                "https://example.com/mouse"
        );

        when(productRepository.findByExternalId(updatedDTO.externalId()))
                .thenReturn(Optional.of(storedProduct));

        Product result = productService.syncProduct(updatedDTO);

        assertSame(storedProduct, result);
        assertEquals("Mouse Gamer Novo", storedProduct.getName());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    void syncProductShouldThrowInvalidProductExceptionWithoutTouchingRepositoriesWhenValidationFails() {
        doThrow(new InvalidProductException("Product external ID is required"))
                .when(validator).validate(invalidProductDTO);

        assertThrows(InvalidProductException.class,
                () -> productService.syncProduct(invalidProductDTO));

        verifyNoInteractions(productRepository, priceHistoryRepository, productMapper);
    }
}
