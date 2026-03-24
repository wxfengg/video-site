package com.videosite.backend.cover;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RuleBasedCoverTagAnalyzer implements CoverTagAnalyzer {

    @Override
    public String analyzerType() {
        return "rule";
    }

    @Override
    public List<TagCandidate> analyze(CoverAnalyzeContext context) {
        String text = (safe(context.getTitle()) + " " + safe(context.getDescription()) + " " + safe(context.getCoverUrl()))
                .toLowerCase(Locale.ROOT);

        Map<String, Double> scores = new LinkedHashMap<>();
        applyKeyword(text, scores, "游戏", 0.92, "game", "电竞", "英雄联盟", "王者");
        applyKeyword(text, scores, "音乐", 0.88, "music", "mv", "演唱", "歌");
        applyKeyword(text, scores, "科技", 0.86, "tech", "ai", "编程", "代码", "java", "vue");
        applyKeyword(text, scores, "教育", 0.84, "教程", "教学", "课程", "知识");
        applyKeyword(text, scores, "生活", 0.75, "vlog", "日常", "旅行", "美食");

        if (text.contains("red") || text.contains("红") || text.contains("#ff")) {
            scores.putIfAbsent("暖色调", 0.66);
        }
        if (text.contains("blue") || text.contains("蓝") || text.contains("#00")) {
            scores.putIfAbsent("冷色调", 0.66);
        }

        List<TagCandidate> tags = new ArrayList<>();
        for (Map.Entry<String, Double> item : scores.entrySet()) {
            tags.add(new TagCandidate(item.getKey(), item.getValue()));
        }

        if (tags.isEmpty()) {
            tags.add(new TagCandidate("通用", 0.5));
        }
        return tags;
    }

    private void applyKeyword(String text,
                              Map<String, Double> scores,
                              String tag,
                              double confidence,
                              String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                scores.putIfAbsent(tag, confidence);
                return;
            }
        }
    }

    private String safe(String text) {
        return StringUtils.hasText(text) ? text : "";
    }
}
