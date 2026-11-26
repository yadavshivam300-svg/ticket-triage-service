package com.triage.service.domain.strategy;

public interface TriageStrategy {

    TriageResult triage(String title, String description);
}
