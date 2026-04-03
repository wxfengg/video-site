package com.videosite.backend.recommend.controller;

import com.videosite.backend.recommend.dto.VideoHotRankItemResponse;
import com.videosite.backend.recommend.service.RecommendService;
import com.videosite.backend.recommend.service.VideoHotRankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecommendController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class RecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendService recommendService;

    @MockBean
    private VideoHotRankService videoHotRankService;

    @Test
    void hotRankShouldReturnRecords() throws Exception {
        VideoHotRankItemResponse item = new VideoHotRankItemResponse();
        item.setWindowType("7d");
        item.setVideoId(1001L);
        item.setRankIndex(1);
        item.setHotScore(32.5);
        item.setTitle("春日散步");

        when(videoHotRankService.listLatest(eq("7d"), eq(5))).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/recommend/hot")
                        .queryParam("windowType", "7d")
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].videoId").value(1001))
                .andExpect(jsonPath("$.data[0].rankIndex").value(1))
                .andExpect(jsonPath("$.data[0].windowType").value("7d"));
    }
}
