package com.ptit.aia.repository;

import com.ptit.aia.domain.IncidentSource;
import com.ptit.aia.domain.Platform;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentSourceRepository extends JpaRepository<IncidentSource, Long> {
    Optional<IncidentSource> findByMessageIdAndChatIdAndPlatform(String messageId, String chatId, Platform platform);
    List<IncidentSource> findByIncidentId(String incidentId);
}
