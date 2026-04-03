package com.videosite.backend.user.controller;

import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.user.dto.UserFavoriteItemResponse;
import com.videosite.backend.user.dto.UserWatchProgressResponse;
import com.videosite.backend.user.service.UserCenterService;
import com.videosite.backend.video.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserCenterController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class UserCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserCenterService userCenterService;

    @Test
    void listFavoritesShouldReturnPagedData() throws Exception {
        UserFavoriteItemResponse item = new UserFavoriteItemResponse();
        item.setId(1L);
        item.setTitle("demo video");

        when(userCenterService.listFavorites(eq(99L), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(1, 1, 10, Collections.singletonList(item)));

        mockMvc.perform(get("/api/users/me/favorites")
                        .sessionAttr(AuthConstants.USER_SESSION_USER_ID_KEY, 99L)
                        .sessionAttr(AuthConstants.USER_SESSION_USERNAME_KEY, "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("demo video"));
    }

    @Test
    void updateProgressShouldReturnLatestProgress() throws Exception {
        UserWatchProgressResponse response = new UserWatchProgressResponse();
        response.setVideoId(3L);
        response.setProgressSec(120);
        response.setCompletionRate(0.9d);
        response.setCompleted90(true);

        when(userCenterService.updateWatchProgress(eq(99L), eq(3L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/users/me/history/3/progress")
                        .sessionAttr(AuthConstants.USER_SESSION_USER_ID_KEY, 99L)
                        .sessionAttr(AuthConstants.USER_SESSION_USERNAME_KEY, "demo")
                        .contentType("application/json")
                        .content("{\"progressSec\":120,\"durationSecSnapshot\":133}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.videoId").value(3))
                .andExpect(jsonPath("$.data.progressSec").value(120))
                .andExpect(jsonPath("$.data.completed90").value(true));
    }
}
