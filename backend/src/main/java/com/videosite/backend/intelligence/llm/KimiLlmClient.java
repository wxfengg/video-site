package com.videosite.backend.intelligence.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KimiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(KimiLlmClient.class);

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KimiLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String provider() {
        return "kimi";
    }

    @Override
    public String chat(List<LlmMessage> messages, boolean jsonMode) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kimi API Key 未配置");
        }

        String protocol = properties.getProtocol();
        if ("anthropic".equalsIgnoreCase(protocol)) {
            return chatAnthropic(messages, apiKey);
        }
        return chatOpenAi(messages, jsonMode, apiKey);
    }

    // ========== OpenAI 兼容协议 (Moonshot 官方 API) ==========

    private String chatOpenAi(List<LlmMessage> messages, boolean jsonMode, String apiKey) {
        LlmRequest request = new LlmRequest();
        request.setModel(properties.getModel());
        request.setMessages(messages);
        request.setTemperature(0.3);

        if (jsonMode) {
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            request.setResponseFormat(responseFormat);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);
        String url = normalizeUrl(properties.getBaseUrl()) + "/chat/completions";

        try {
            ResponseEntity<LlmResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, LlmResponse.class);
            LlmResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kimi API 返回空响应");
            }
            if (body.getError() != null) {
                log.error("Kimi API error: {}", body.getError().getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Kimi API 错误: " + body.getError().getMessage());
            }
            String content = body.firstMessageContent();
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kimi API 返回空内容");
            }
            return content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Kimi API request failed", ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Kimi API 调用失败: " + ex.getMessage());
        }
    }

    // ========== Anthropic 兼容协议 (Kimi Coding API) ==========

    private String chatAnthropic(List<LlmMessage> messages, String apiKey) {
        AnthropicRequest req = new AnthropicRequest();
        req.setModel(properties.getModel());
        req.setMaxTokens(4096);

        List<AnthropicMessage> anthropicMessages = new ArrayList<>();
        for (LlmMessage msg : messages) {
            String role = msg.getRole();
            if ("system".equals(role)) {
                // Anthropic 的 system 是顶层字段
                req.setSystem(String.valueOf(msg.getContent()));
                continue;
            }
            AnthropicMessage am = new AnthropicMessage();
            am.setRole("user".equals(role) ? "user" : "assistant");
            am.setContent(convertContent(msg.getContent()));
            anthropicMessages.add(am);
        }
        req.setMessages(anthropicMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<AnthropicRequest> entity = new HttpEntity<>(req, headers);
        String url = normalizeUrl(properties.getBaseUrl()) + "/v1/messages";

        try {
            ResponseEntity<AnthropicResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AnthropicResponse.class);
            AnthropicResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kimi API 返回空响应");
            }
            if (body.getError() != null) {
                log.error("Kimi API error: {}", body.getError().getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Kimi API 错误: " + body.getError().getMessage());
            }
            String content = body.firstTextContent();
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kimi API 返回空内容");
            }
            return content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Kimi API request failed", ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Kimi API 调用失败: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<AnthropicContent> convertContent(Object content) {
        List<AnthropicContent> result = new ArrayList<>();
        if (content instanceof String) {
            AnthropicContent ac = new AnthropicContent();
            ac.setType("text");
            ac.setText((String) content);
            result.add(ac);
            return result;
        }
        if (content instanceof List<?>) {
            List<LlmMessage.ContentPart> parts = (List<LlmMessage.ContentPart>) content;
            for (LlmMessage.ContentPart part : parts) {
                if ("text".equals(part.getType())) {
                    AnthropicContent ac = new AnthropicContent();
                    ac.setType("text");
                    ac.setText(part.getText());
                    result.add(ac);
                } else if ("image_url".equals(part.getType())) {
                    String url = part.getImageUrl() != null ? part.getImageUrl().getUrl() : "";
                    String base64Data = extractBase64Data(url);
                    if (!base64Data.isEmpty()) {
                        AnthropicContent ac = new AnthropicContent();
                        ac.setType("image");
                        AnthropicImageSource source = new AnthropicImageSource();
                        source.setType("base64");
                        source.setMediaType(detectMediaType(url));
                        source.setData(base64Data);
                        ac.setSource(source);
                        result.add(ac);
                    }
                }
            }
        }
        return result;
    }

    private String extractBase64Data(String dataUrl) {
        if (dataUrl == null) {
            return "";
        }
        int idx = dataUrl.indexOf("base64,");
        if (idx >= 0) {
            return dataUrl.substring(idx + 7);
        }
        return dataUrl;
    }

    private String detectMediaType(String dataUrl) {
        if (dataUrl == null) {
            return "image/jpeg";
        }
        if (dataUrl.contains("image/png")) {
            return "image/png";
        }
        if (dataUrl.contains("image/webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    // ========== Anthropic DTOs ==========

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicRequest {
        private String model;
        private List<AnthropicMessage> messages;
        private String system;
        @JsonProperty("max_tokens")
        private int maxTokens = 4096;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<AnthropicMessage> getMessages() { return messages; }
        public void setMessages(List<AnthropicMessage> messages) { this.messages = messages; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicMessage {
        private String role;
        private List<AnthropicContent> content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<AnthropicContent> getContent() { return content; }
        public void setContent(List<AnthropicContent> content) { this.content = content; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicContent {
        private String type;
        private String text;
        private AnthropicImageSource source;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public AnthropicImageSource getSource() { return source; }
        public void setSource(AnthropicImageSource source) { this.source = source; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicImageSource {
        private String type;
        @JsonProperty("media_type")
        private String mediaType;
        private String data;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String mediaType) { this.mediaType = mediaType; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicResponse {
        private String id;
        private String type;
        private String role;
        private List<AnthropicContent> content;
        private AnthropicError error;
        private AnthropicUsage usage;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<AnthropicContent> getContent() { return content; }
        public void setContent(List<AnthropicContent> content) { this.content = content; }
        public AnthropicError getError() { return error; }
        public void setError(AnthropicError error) { this.error = error; }
        public AnthropicUsage getUsage() { return usage; }
        public void setUsage(AnthropicUsage usage) { this.usage = usage; }

        public String firstTextContent() {
            if (content == null || content.isEmpty()) {
                return null;
            }
            AnthropicContent c = content.get(0);
            return c != null ? c.getText() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicError {
        private String message;
        private String type;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnthropicUsage {
        @JsonProperty("input_tokens")
        private int inputTokens;
        @JsonProperty("output_tokens")
        private int outputTokens;

        public int getInputTokens() { return inputTokens; }
        public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    }
}
