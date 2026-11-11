package com.ryansouzas.amazon_promo_bot.config;

import com.ryansouzas.amazon_promo_bot.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String USER = "test-user";
    private static final String PASSWORD = "test-password";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionService promotionService;

    @Test
    void promotionsTest_withoutCredentials_returns401AndDoesNotPublish() throws Exception {
        mockMvc.perform(post("/promotions/test"))
                .andExpect(status().isUnauthorized());

        verify(promotionService, never()).publishPromotions();
    }

    @Test
    void promotionsTest_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/promotions/test").with(httpBasic(USER, "wrong")))
                .andExpect(status().isUnauthorized());

        verify(promotionService, never()).publishPromotions();
    }

    @Test
    void promotionsTest_withValidCredentials_returns204AndPublishes() throws Exception {
        mockMvc.perform(post("/promotions/test").with(httpBasic(USER, PASSWORD)))
                .andExpect(status().isNoContent());

        verify(promotionService).publishPromotions();
    }

    @Test
    void otherRoutes_withoutCredentials_return401() throws Exception {
        mockMvc.perform(get("/products/ABC123"))
                .andExpect(status().isUnauthorized());
    }
}
