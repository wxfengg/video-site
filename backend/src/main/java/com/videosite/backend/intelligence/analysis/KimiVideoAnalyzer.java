package com.videosite.backend.intelligence.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videosite.backend.intelligence.llm.LlmClient;
import com.videosite.backend.intelligence.llm.LlmMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class KimiVideoAnalyzer implements VideoIntelligenceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(KimiVideoAnalyzer.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public KimiVideoAnalyzer(List<LlmClient> clients, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        LlmClient found = null;
        for (LlmClient client : clients) {
            if ("kimi".equals(client.provider())) {
                found = client;
                break;
            }
        }
        if (found == null) {
            throw new IllegalStateException("未找到 Kimi LLM 客户端");
        }
        this.llmClient = found;
    }

    @Override
    public String analyzerType() {
        return "kimi";
    }

    @Override
    public IntelligenceResult analyze(AnalyzeContext context) {
        String prompt = buildPrompt(context);
        List<LlmMessage> messages = new ArrayList<>();

        LlmMessage systemMsg = new LlmMessage();
        systemMsg.setRole("system");
        systemMsg.setContent("你是一个视频内容分析专家。请严格根据用户提供的视频信息和关键帧截图，生成 JSON 格式的分析结果。");
        messages.add(systemMsg);

        List<LlmMessage.ContentPart> contentParts = new ArrayList<>();
        contentParts.add(LlmMessage.ContentPart.text(prompt));

        if (context.getKeyframePaths() != null) {
            for (String path : context.getKeyframePaths()) {
                try {
                    String base64 = encodeImageToBase64(path);
                    if (StringUtils.hasText(base64)) {
                        contentParts.add(LlmMessage.ContentPart.imageUrl(base64));
                    }
                } catch (Exception ex) {
                    log.warn("编码关键帧失败: {}, error={}", path, ex.getMessage());
                }
            }
        }

        LlmMessage userMsg = new LlmMessage();
        userMsg.setRole("user");
        userMsg.setContent(contentParts);
        messages.add(userMsg);

        String rawResponse = llmClient.chat(messages, true);
        String jsonResponse = stripMarkdownCodeBlock(rawResponse);

        try {
            IntelligenceResult result = objectMapper.readValue(jsonResponse, IntelligenceResult.class);
            if (result.getTags() == null) {
                result.setTags(new ArrayList<>());
            }
            if (result.getCategories() == null) {
                result.setCategories(new ArrayList<>());
            }
            return result;
        } catch (IOException ex) {
            log.error("解析 Kimi 响应失败: {}", jsonResponse, ex);
            throw new RuntimeException("解析 AI 分析结果失败: " + ex.getMessage(), ex);
        }
    }

    private String buildPrompt(AnalyzeContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下视频内容，生成 JSON 格式的结果。\n\n");
        sb.append("视频标题: ").append(safe(context.getTitle())).append("\n");
        sb.append("视频简介: ").append(safe(context.getDescription())).append("\n");

        if (StringUtils.hasText(context.getAudioTranscript())) {
            String transcript = context.getAudioTranscript();
            if (transcript.length() > 3000) {
                transcript = transcript.substring(0, 3000) + "...";
            }
            sb.append("音频转录文本: ").append(transcript).append("\n");
        }

        sb.append("\n要求输出格式:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"200字以内的中文摘要\",\n");
        sb.append("  \"tags\": [\"标签1\", \"标签2\", ...], // 8-15个，涵盖主题、风格、情感\n");
        sb.append("  \"categories\": [\"一级分类\", \"二级分类\", \"三级分类\"],\n");
        sb.append("  \"sentiment\": \"positive|neutral|negative\",\n");
        sb.append("  \"audience\": \"目标受众描述，如'对编程感兴趣的初学者'\",\n");
        sb.append("  \"keywords\": \"空格分隔的关键词，用于搜索和推荐\",\n");
        sb.append("  \"embeddingText\": \"聚合标题、简介、音频内容的标准化语义描述文本，用于相似度计算\"\n");
        sb.append("}\n");
        sb.append("\n注意:\n");
        sb.append("- 如果信息不足，基于已有内容合理推断\n");
        sb.append("- categories 必须三级，例如 [\"科技\", \"编程\", \"Java\"]\n");
        sb.append("- sentiment 只能是 positive/neutral/negative 之一\n");
        sb.append("- embeddingText 要包含视频的核心主题、风格、受众等关键信息\n");

        return sb.toString();
    }

    private String encodeImageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        if (!Files.exists(path)) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:image/jpeg;base64," + base64;
    }

    private String stripMarkdownCodeBlock(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                trimmed = trimmed.substring(3);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String safe(String text) {
        return StringUtils.hasText(text) ? text : "无";
    }
}
