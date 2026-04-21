package com.videosite.backend.intelligence.analysis;

public interface VideoIntelligenceAnalyzer {

    String analyzerType();

    IntelligenceResult analyze(AnalyzeContext context);
}
