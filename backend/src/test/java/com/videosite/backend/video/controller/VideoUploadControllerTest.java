package com.videosite.backend.video.controller;

import com.videosite.backend.video.dto.UploadCoverResponse;
import com.videosite.backend.video.service.VideoUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VideoUploadController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.videosite.backend.config.*"))
class VideoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoUploadService videoUploadService;

    @Test
    void uploadCoverShouldReturnCoverUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3}
        );

        when(videoUploadService.uploadCover(eq(100L), any()))
                .thenReturn(new UploadCoverResponse("images/covers/100/abc.jpg", "/api/storage/images/covers/100/abc.jpg"));

        mockMvc.perform(multipart("/api/videos/upload/cover/100").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.objectKey").value("images/covers/100/abc.jpg"))
                .andExpect(jsonPath("$.data.coverUrl").value("/api/storage/images/covers/100/abc.jpg"));
    }

            @Test
            void uploadCoverShouldRejectUnsupportedType() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.gif",
                MediaType.IMAGE_GIF_VALUE,
                new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/videos/upload/cover/100").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("封面仅支持 jpg/jpeg、png、webp 格式")));
            }

            @Test
            void uploadCoverShouldRejectTooLargeFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[5 * 1024 * 1024 + 1]
            );

            mockMvc.perform(multipart("/api/videos/upload/cover/100").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("封面图片不能超过 5MB"));
            }
}
