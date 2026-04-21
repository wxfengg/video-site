package com.videosite.backend.intelligence.llm;

import java.util.List;

public interface LlmClient {

    String provider();

    String chat(List<LlmMessage> messages, boolean jsonMode);
}
