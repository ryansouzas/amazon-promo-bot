package com.ryansouzas.amazon_promo_bot.source;

import com.ryansouzas.amazon_promo_bot.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FakeProductSource implements ProductSource{

    @Override
    public List<Product> findProducts() {
        return List.of(
                new Product(
                        "AMZ-001",
                        "Mouse Gamer",
                        new BigDecimal("200.00"),
                        new BigDecimal("100.00"),
                        "https://example.com/mouse"
                ),
                new Product(
                        "AMZ-002",
                        "Teclado Mecânico",
                        new BigDecimal("300.00"),
                        new BigDecimal("270.00"),
                        "https://example.com/teclado"
                ),
                new Product(
                        "AMZ-003",
                        "Monitor Gamer",
                        new BigDecimal("1500.00"),
                        new BigDecimal("900.00"),
                        "https://example.com/monitor"
                )
        );
    }
}
