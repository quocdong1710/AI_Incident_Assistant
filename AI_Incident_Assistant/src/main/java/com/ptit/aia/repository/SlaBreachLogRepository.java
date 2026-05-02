package com.ptit.aia.repository;

import com.ptit.aia.domain.SlaBreachLog;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaBreachLogRepository extends JpaRepository<SlaBreachLog, Long> {
    List<SlaBreachLog> findByBreachAtAfter(OffsetDateTime since);
}
