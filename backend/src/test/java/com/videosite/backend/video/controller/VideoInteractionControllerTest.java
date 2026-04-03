package com.videosite.backend.video.controller;

import com.videosite.backend.common.auth.AuthConstants;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.VideoCommentItemResponse;
import com.videosite.backend.video.dto.VideoLikeSummaryResponse;
import com.videosite.backend.video.service.VideoInteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VideoInteractionController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class VideoInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoInteractionService videoInteractionService;

    @Test
    void listCommentsShouldReturnPagedRecords() throws Exception {
        VideoCommentItemResponse item = new VideoCommentItemResponse();
        item.setId(1L);
        item.setUsername("demo");
        item.setContent("hello world");

        when(videoInteractionService.listComments(eq(7L), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(1, 1, 20, Collections.singletonList(item)));

        mockMvc.perform(get("/api/videos/7/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].content").value("hello world"));
    }

    @Test
    void likeSummaryShouldReturnCount() throws Exception {
        VideoLikeSummaryResponse response = new VideoLikeSummaryResponse();
        response.setVideoId(7L);
        response.setLikeCount(12L);
        response.setLikedByCurrentUser(true);

        when(videoInteractionService.getLikeSummary(eq(7L), eq(99L))).thenReturn(response);

        mockMvc.perform(get("/api/videos/7/likes/summary")
                        .sessionAttr(AuthConstants.USER_SESSION_USER_ID_KEY, 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.likeCount").value(12))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
    }

    @Test
    void addLikeShouldRequireSessionAndReturnOk() throws Exception {
        when(videoInteractionService.addLike(eq(99L), eq(7L), anyString())).thenReturn("ok");

        mockMvc.perform(post("/api/videos/7/likes")
                        .sessionAttr(AuthConstants.USER_SESSION_USER_ID_KEY, 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void deleteCommentShouldReturnOk() throws Exception {
        when(videoInteractionService.deleteComment(eq(99L), eq(7L), eq(3L), anyString())).thenReturn("ok");

        mockMvc.perform(delete("/api/videos/7/comments/3")
                        .sessionAttr(AuthConstants.USER_SESSION_USER_ID_KEY, 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));
    }
}
