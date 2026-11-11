package com.ryansouzas.amazon_promo_bot.config;

import com.ryansouzas.amazon_promo_bot.controller.ProductController;
import com.ryansouzas.amazon_promo_bot.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=api-user",
        "app.security.password=api-secret"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void otherRoutesAlsoRequireAuthentication() throws Exception {
        mockMvc.perform(get("/products/B000123"))
                .andExpect(status().isUnauthorized());
    }
}
