package com.videosite.backend.intelligence.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmRequest {

    private String model;
    private List<LlmMessage> messages;
    private Double temperature;

    @JsonProperty("response_format")
    private Map<String, String> responseFormat;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<LlmMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<LlmMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Map<String, String> getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(Map<String, String> responseFormat) {
        this.responseFormat = responseFormat;
    }
}
