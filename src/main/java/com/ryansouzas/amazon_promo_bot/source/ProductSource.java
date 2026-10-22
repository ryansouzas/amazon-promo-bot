package com.ryansouzas.amazon_promo_bot.source;

import com.ryansouzas.amazon_promo_bot.dto.ProductDTO;

import java.util.List;

public interface ProductSource {

    List<ProductDTO> findProducts();
}
