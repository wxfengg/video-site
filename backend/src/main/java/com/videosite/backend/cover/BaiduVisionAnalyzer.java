package com.videosite.backend.cover;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BaiduVisionAnalyzer implements CoverTagAnalyzer {

    @Override
    public String analyzerType() {
        return "baidu_vision";
    }

    @Override
    public List<TagCandidate> analyze(CoverAnalyzeContext context) {
        return Collections.emptyList();
    }
}
