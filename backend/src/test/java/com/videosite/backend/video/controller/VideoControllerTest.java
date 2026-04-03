package com.videosite.backend.video.controller;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.VideoDetailResponse;
import com.videosite.backend.video.dto.VideoListItemResponse;
import com.videosite.backend.video.service.VideoService;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VideoController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @Test
    void listPublicVideosShouldReturnOk() throws Exception {
        VideoListItemResponse item = new VideoListItemResponse();
        item.setId(1L);
        item.setTitle("demo");
        item.setStatus("published");

        PageResult<VideoListItemResponse> pageResult = new PageResult<>(1, 1, 10, Collections.singletonList(item));
        when(videoService.listPublicVideos(anyInt(), anyInt(), nullable(String.class))).thenReturn(pageResult);

        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("demo"));
    }

    @Test
    void getPublicVideoShouldReturnDetail() throws Exception {
        VideoDetailResponse detail = new VideoDetailResponse();
        detail.setId(2L);
        detail.setTitle("detail-video");
        detail.setStatus("published");

        when(videoService.getVideoDetail(eq(2L), eq(false))).thenReturn(detail);

        mockMvc.perform(get("/api/videos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.title").value("detail-video"));
    }

    @Test
    void deleteVideoShouldReturnOk() throws Exception {
        when(videoService.deleteVideo(eq(3L))).thenReturn("deleted");

        mockMvc.perform(delete("/api/admin/videos/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("deleted"));
    }

    @Test
    void deleteVideoShouldRejectWhenNotOffline() throws Exception {
        doThrow(new BusinessException(ErrorCode.BAD_REQUEST, "仅支持删除已下线视频"))
                .when(videoService)
                .deleteVideo(eq(4L));

        mockMvc.perform(delete("/api/admin/videos/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("仅支持删除已下线视频"));
    }

    @Test
    void createExternalVideoShouldReturnDetail() throws Exception {
        VideoDetailResponse detail = new VideoDetailResponse();
        detail.setId(9L);
        detail.setTitle("external-demo");
        detail.setStatus("ready");

        when(videoService.createExternalVideo(org.mockito.ArgumentMatchers.any())).thenReturn(detail);

        mockMvc.perform(post("/api/admin/videos/external")
                        .contentType("application/json")
                        .content("{\"title\":\"external-demo\",\"sourceProtocol\":\"hls\",\"sourceUrl\":\"https://demo.com/master.m3u8\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.title").value("external-demo"));
    }
}
