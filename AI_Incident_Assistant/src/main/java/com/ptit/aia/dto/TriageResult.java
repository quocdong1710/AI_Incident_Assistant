package com.ptit.aia.dto;

import com.ptit.aia.domain.Severity;

public record TriageResult(
        boolean bugReport,
        boolean uncertain,
        double confidence,
        String title,
        String description,
        String component,
        Severity severity,
        String impactScope,
        String environment,
        String stepsToReproduce,
        String suggestedResponse,
        String sourceLanguage
) {}
