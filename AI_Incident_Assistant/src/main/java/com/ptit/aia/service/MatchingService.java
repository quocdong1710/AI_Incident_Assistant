package com.ptit.aia.service;

import com.ptit.aia.config.AiaProperties;
import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.IncidentStatus;
import com.ptit.aia.dto.TriageResult;
import com.ptit.aia.repository.IncidentRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
    private final IncidentRepository incidentRepository;
    private final AiaProperties properties;

    public MatchingService(IncidentRepository incidentRepository, AiaProperties properties) {
        this.incidentRepository = incidentRepository;
        this.properties = properties;
    }

    public Optional<Match> findDuplicate(TriageResult triage) {
        List<Incident> open = incidentRepository.findByStatusIn(List.of(IncidentStatus.Open, IncidentStatus.InProgress, IncidentStatus.Reopened));
        return open.stream()
                .map(incident -> new Match(incident, cosine(triage.title() + " " + triage.description(), incident.getTitle() + " " + incident.getDescription())))
                .filter(match -> match.similarity() >= properties.getSimilarityThreshold())
                .max((a, b) -> Double.compare(a.similarity(), b.similarity()));
    }

    private double cosine(String a, String b) {
        Map<String, Long> va = vectorize(a);
        Map<String, Long> vb = vectorize(b);
        double dot = 0;
        for (String key : va.keySet()) dot += va.getOrDefault(key, 0L) * vb.getOrDefault(key, 0L);
        double normA = Math.sqrt(va.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(vb.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0 || normB == 0) return 0;
        return dot / (normA * normB);
    }

    private Map<String, Long> vectorize(String value) {
        return Arrays.stream(value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", " ").split("\\s+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    public record Match(Incident incident, double similarity) {}
}
