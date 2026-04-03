package com.videosite.backend.user.controller;

import com.videosite.backend.user.dto.UserSessionResponse;
import com.videosite.backend.user.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAuthService userAuthService;

    @Test
    void registerShouldReturnLoggedInSession() throws Exception {
        when(userAuthService.register(any(), any()))
                .thenReturn(new UserSessionResponse(true, 1001L, "demo_user"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"demo_user\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loggedIn").value(true))
                .andExpect(jsonPath("$.data.userId").value(1001))
                .andExpect(jsonPath("$.data.username").value("demo_user"));
    }

    @Test
    void meShouldReturnCurrentSession() throws Exception {
        when(userAuthService.me(any()))
                .thenReturn(new UserSessionResponse(true, 2002L, "alice"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loggedIn").value(true))
                .andExpect(jsonPath("$.data.userId").value(2002))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }
}
