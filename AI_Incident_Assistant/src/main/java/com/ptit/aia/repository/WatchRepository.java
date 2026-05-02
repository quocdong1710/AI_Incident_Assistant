package com.ptit.aia.repository;

import com.ptit.aia.domain.Watch;
import com.ptit.aia.domain.WatchStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRepository extends JpaRepository<Watch, Long> {
    List<Watch> findByStatus(WatchStatus status);
}
