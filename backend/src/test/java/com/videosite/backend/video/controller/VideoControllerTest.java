package com.videosite.backend.video.controller;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
