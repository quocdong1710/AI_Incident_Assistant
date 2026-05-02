package com.ptit.aia.service;

import com.ptit.aia.domain.Watch;
import com.ptit.aia.domain.WatchStatus;
import com.ptit.aia.repository.WatchRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WatchService {
    private static final Logger log = LoggerFactory.getLogger(WatchService.class);
    private final WatchRepository watchRepository;

    public WatchService(WatchRepository watchRepository) {
        this.watchRepository = watchRepository;
    }

    public void createWatch(String incidentId, String jiraKey, String taggedUser) {
        Watch watch = new Watch();
        watch.setIncidentId(incidentId);
        watch.setJiraIssueKey(jiraKey);
        watch.setTaggedUser(taggedUser);
        watch.setWatchStart(OffsetDateTime.now());
        watch.setExpiresAt(OffsetDateTime.now().plusHours(2));
        watch.setStatus(WatchStatus.waiting);
        watchRepository.save(watch);
        log.info("Created watch for user {} on incident {}", taggedUser, incidentId);
    }

    @Scheduled(fixedDelay = 60000)
    public void monitorWatches() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Watch> waiting = watchRepository.findByStatus(WatchStatus.waiting);
        for (Watch watch : waiting) {
            if (now.isAfter(watch.getExpiresAt())) {
                watch.setStatus(WatchStatus.overdue);
                watchRepository.save(watch);
                log.warn("[SLA Alert] Khách hàng đã tag {} trong {} nhưng chưa có phản hồi hợp lệ sau 2 giờ. Recommended action: Escalate to Team Lead.", watch.getTaggedUser(), watch.getJiraIssueKey());
            }
        }
    }
}
