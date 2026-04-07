package com.videosite.backend.video.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExternalTitleCoverService {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private final StorageService storageService;

    public ExternalTitleCoverService(StorageService storageService) {
        this.storageService = storageService;
    }

    public String generateTitleCover(Long videoId, String title) {
        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : "未命名视频";
        byte[] imageBytes = renderTitleCoverPng(normalizedTitle);
        String objectKey = buildObjectKey(videoId);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            storageService.put(objectKey, inputStream, imageBytes.length, "image/png");
        } catch (Exception ex) {
            throw new IllegalStateException("生成标题封面失败", ex);
        }

        return storageService.getUploadUrl(objectKey);
    }

    byte[] renderTitleCoverPng(String title) {
        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : "未命名视频";

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GradientPaint background = new GradientPaint(
                    0,
                    0,
                    new Color(30, 64, 175),
                    WIDTH,
                    HEIGHT,
                    new Color(99, 102, 241)
            );
            graphics.setPaint(background);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            graphics.setColor(new Color(15, 23, 42, 120));
            graphics.fillRoundRect(80, 90, WIDTH - 160, HEIGHT - 180, 36, 36);

            graphics.setColor(new Color(255, 255, 255, 180));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
            graphics.drawString("EXTERNAL VIDEO", 120, 165);

            Font titleFont = new Font("SansSerif", Font.BOLD, 68);
            graphics.setFont(titleFont);
            graphics.setColor(Color.WHITE);

            int textMaxWidth = WIDTH - 220;
            int maxLines = 3;
            FontMetrics metrics = graphics.getFontMetrics(titleFont);
            List<String> lines = wrapText(normalizedTitle, metrics, textMaxWidth, maxLines);

            int lineHeight = metrics.getHeight() + 8;
            int blockHeight = lines.size() * lineHeight;
            int startY = (HEIGHT - blockHeight) / 2 + metrics.getAscent();
            int startX = 110;

            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(lines.get(i), startX, startY + i * lineHeight);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成标题封面图片失败", ex);
        }
    }

    private List<String> wrapText(String text, FontMetrics metrics, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String normalized = StringUtils.hasText(text) ? text : "未命名视频";

        int index = 0;
        while (index < normalized.length() && lines.size() < maxLines) {
            int next = index + 1;
            while (next <= normalized.length() && metrics.stringWidth(normalized.substring(index, next)) <= maxWidth) {
                next++;
            }

            if (next == index + 1) {
                next = Math.min(index + 2, normalized.length());
            }

            int end = Math.max(index + 1, next - 1);
            lines.add(normalized.substring(index, end));
            index = end;
        }

        if (index < normalized.length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            String ellipsized = lines.get(last);
            while (!ellipsized.isEmpty() && metrics.stringWidth(ellipsized + "…") > maxWidth) {
                ellipsized = ellipsized.substring(0, ellipsized.length() - 1);
            }
            lines.set(last, ellipsized + "…");
        }

        return lines;
    }

    private String buildObjectKey(Long videoId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "images/covers/" + datePart + "/" + videoId + "_external_title_" + IdWorker.getId() + ".png";
    }
}
