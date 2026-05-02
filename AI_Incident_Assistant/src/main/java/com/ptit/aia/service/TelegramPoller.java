package com.ptit.aia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Kept as placeholder. Bot functionality moved to IncidentTelegramBot (telegrambots library).
 */
@Service
public class TelegramPoller {
    private static final Logger log = LoggerFactory.getLogger(TelegramPoller.class);

    public TelegramPoller() {
        log.info("[TelegramPoller] Disabled – using IncidentTelegramBot (telegrambots long-polling) instead.");
    }
}
