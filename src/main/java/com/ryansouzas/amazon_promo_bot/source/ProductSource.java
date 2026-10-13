package com.ryansouzas.amazon_promo_bot.source;

import com.ryansouzas.amazon_promo_bot.model.Product;

import java.util.List;

public interface ProductSource {

    List<Product> findProducts();
}
