package com.videosite.backend.video.service;

import com.videosite.backend.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalTitleCoverServiceTest {

    @Test
    void generateTitleCoverShouldUploadPngAndReturnStorageUrl() throws Exception {
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUploadUrl(anyString()))
                .thenAnswer(invocation -> "/api/storage/" + invocation.getArgument(0, String.class));

        ExternalTitleCoverService service = new ExternalTitleCoverService(storageService);
        String coverUrl = service.generateTitleCover(123L, "这是一个测试外链标题");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<Long> sizeCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(storageService).put(keyCaptor.capture(), streamCaptor.capture(), sizeCaptor.capture(), typeCaptor.capture());

        byte[] bytes = streamCaptor.getValue().readAllBytes();

        assertNotNull(coverUrl);
        assertTrue(coverUrl.startsWith("/api/storage/images/covers/"));
        assertTrue(keyCaptor.getValue().startsWith("images/covers/"));
        assertEquals("image/png", typeCaptor.getValue());
        assertTrue(bytes.length > 100);
        assertEquals(bytes.length, sizeCaptor.getValue().longValue());
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals((byte) 0x50, bytes[1]);
        assertEquals((byte) 0x4E, bytes[2]);
        assertEquals((byte) 0x47, bytes[3]);
    }

    @Test
    void renderTitleCoverPngShouldSupportBlankTitleFallback() {
        StorageService storageService = mock(StorageService.class);
        ExternalTitleCoverService service = new ExternalTitleCoverService(storageService);

        byte[] imageBytes = service.renderTitleCoverPng("  ");

        assertTrue(imageBytes.length > 100);
    }
}
