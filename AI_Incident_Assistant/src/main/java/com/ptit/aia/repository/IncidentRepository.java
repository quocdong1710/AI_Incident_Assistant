package com.ptit.aia.repository;

import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.IncidentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByIncidentId(String incidentId);
    Optional<Incident> findByJiraIssueKey(String jiraIssueKey);
    boolean existsByJiraIssueKey(String jiraIssueKey);
    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);
}
