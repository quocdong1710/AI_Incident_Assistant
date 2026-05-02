package com.ptit.aia.repository;

import com.ptit.aia.domain.MessageLog;
import com.ptit.aia.domain.Platform;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {
    Optional<MessageLog> findByMessageIdAndGroupIdAndPlatform(String messageId, String groupId, Platform platform);
}
