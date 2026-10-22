package com.ryansouzas.amazon_promo_bot.source;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FakeProductSource implements ProductSource{

    @Override
    public List<ProductDTO> findProducts() {
        return List.of(
                new ProductDTO(
                        "AMZ-001",
                        "Mouse Gamer",
                        new BigDecimal("200.00"),
                        new BigDecimal("100.00"),
                        "https://example.com/mouse"
                ),
                new ProductDTO(
                        "AMZ-002",
                        "Teclado Mecânico",
                        new BigDecimal("300.00"),
                        new BigDecimal("270.00"),
                        "https://example.com/teclado"
                ),
                new ProductDTO(
                        "AMZ-003",
                        "Monitor Gamer",
                        new BigDecimal("1500.00"),
                        new BigDecimal("900.00"),
                        "https://example.com/monitor"
                )
        );
    }
}
