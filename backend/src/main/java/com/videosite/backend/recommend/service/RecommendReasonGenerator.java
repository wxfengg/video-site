package com.videosite.backend.recommend.service;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class RecommendReasonGenerator {

    public String generate(double contentScore, double hotScore, List<String> categories) {
        // 规则 1: 内容相似度高
        if (contentScore > 0.7) {
            return "与你看过的视频内容相似";
        }
        // 规则 2: 热门度高
        if (hotScore > 0.8) {
            return "近期热门视频";
        }
        // 规则 3: AI 分类匹配
        if (!CollectionUtils.isEmpty(categories)) {
            String category = categories.get(0);
            return "AI 推荐：" + category + "类内容";
        }
        // 默认
        return "猜你喜欢";
    }
}
