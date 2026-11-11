package com.ryansouzas.amazon_promo_bot.config;

import com.ryansouzas.amazon_promo_bot.controller.ProductController;
import com.ryansouzas.amazon_promo_bot.controller.PromotionController;
import com.ryansouzas.amazon_promo_bot.service.ProductService;
import com.ryansouzas.amazon_promo_bot.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PromotionController.class, ProductController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=api-user",
        "app.security.password=api-secret"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private ProductService productService;

    @Test
    void promotionsTestWithoutCredentialsReturns401() throws Exception {
        mockMvc.perform(post("/promotions/test"))
                .andExpect(status().isUnauthorized());

        verify(promotionService, never()).publishPromotions();
    }

    @Test
    void promotionsTestWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/promotions/test").with(httpBasic("api-user", "wrong")))
                .andExpect(status().isUnauthorized());

        verify(promotionService, never()).publishPromotions();
    }

    @Test
    void promotionsTestWithValidCredentialsReturns204WithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/promotions/test").with(httpBasic("api-user", "api-secret")))
                .andExpect(status().isNoContent());

        verify(promotionService).publishPromotions();
    }

    @Test
    void otherRoutesAlsoRequireAuthentication() throws Exception {
        mockMvc.perform(get("/products/B000123"))
                .andExpect(status().isUnauthorized());
    }
}
