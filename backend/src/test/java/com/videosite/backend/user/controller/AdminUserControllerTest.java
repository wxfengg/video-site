package com.videosite.backend.user.controller;

import com.videosite.backend.user.dto.AdminUserCreateRequest;
import com.videosite.backend.user.dto.AdminUserListItemResponse;
import com.videosite.backend.user.service.AdminUserService;
import com.videosite.backend.video.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void listUsersShouldReturnPageResult() throws Exception {
        AdminUserListItemResponse item = new AdminUserListItemResponse();
        item.setId(1001L);
        item.setUsername("demo_user");
        item.setStatus(1);
        item.setCreatedAt(LocalDateTime.parse("2026-04-07T10:00:00"));

        PageResult<AdminUserListItemResponse> page = new PageResult<>(1, 1, 10, Collections.singletonList(item));

        when(adminUserService.listUsers(eq(1), eq(10), eq("demo"))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .queryParam("page", "1")
                        .queryParam("pageSize", "10")
                        .queryParam("keyword", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1001))
                .andExpect(jsonPath("$.data.records[0].username").value("demo_user"));
    }

    @Test
    void createUserShouldReturnCreatedUser() throws Exception {
        AdminUserListItemResponse created = new AdminUserListItemResponse();
        created.setId(2002L);
        created.setUsername("alice");
        created.setStatus(1);

        when(adminUserService.createUser(any(AdminUserCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/users")
                        .contentType("application/json")
                        .content("{\"username\":\"alice\",\"password\":\"123456\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2002))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void updateUserStatusShouldReturnUpdatedUser() throws Exception {
        AdminUserListItemResponse updated = new AdminUserListItemResponse();
        updated.setId(2002L);
        updated.setUsername("alice");
        updated.setStatus(0);

        when(adminUserService.updateUserStatus(eq(2002L), eq(0))).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/users/2002/status")
                        .contentType("application/json")
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2002))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void deleteUserShouldReturnDeleted() throws Exception {
        when(adminUserService.deleteUser(eq(3003L))).thenReturn("deleted");

        mockMvc.perform(delete("/api/admin/users/3003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("deleted"));
    }
}