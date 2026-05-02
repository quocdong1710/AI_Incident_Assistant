package com.ptit.aia.bot;

import com.ptit.aia.domain.Incident;
import com.ptit.aia.domain.IncidentStatus;
import com.ptit.aia.dto.IncomingMessage;
import com.ptit.aia.dto.ProcessResult;
import com.ptit.aia.domain.Platform;
import com.ptit.aia.repository.IncidentRepository;
import com.ptit.aia.service.IncidentService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import jakarta.annotation.PostConstruct;

@Component
public class IncidentTelegramBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(IncidentTelegramBot.class);

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;
    private final String resolvedUsername;
    private final String botToken;
    // Virtual threads: AI triage won't block Telegram polling thread
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public IncidentTelegramBot(
            IncidentService incidentService,
            IncidentRepository incidentRepository,
            @Value("${spring.telegram.bot.token:}") String botToken,
            @Value("${spring.telegram.bot.username:aia_incident_bot}") String botUsername) {
        super(botToken);
        this.incidentService = incidentService;
        this.incidentRepository = incidentRepository;
        this.resolvedUsername = botUsername;
        this.botToken = botToken;
        registerCommands();
    }

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("[Bot] Thiếu token Telegram, bot sẽ không chạy.");
            return;
        }
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("[Bot] Đã khởi động và đăng ký thành công với username @{}", resolvedUsername);
        } catch (TelegramApiException e) {
            log.error("[Bot] Lỗi không thể khởi động bot Telegram: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return resolvedUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        Message msg = update.getMessage();
        log.info("[Bot] Nhận tin nhắn từ chatId={}: {}", msg.getChatId(), msg.getText());
        executor.submit(() -> handleMessage(msg));
    }

    private void handleMessage(Message message) {
        // Lấy text và tách username của bot ra nếu có (vd: /help@bot_name -> /help)
        String raw = message.getText().trim();
        String text = raw;
        if (text.contains("@")) {
            // Lọc bỏ phần @username đi kèm với lệnh (chỉ xử lý lệnh ở đầu câu)
            text = text.replaceAll("(?i)^(/\\w+)@\\w+", "$1");
        }

        String chatId = message.getChatId().toString();
        String messageId = message.getMessageId().toString();
        String senderId = message.getFrom().getId().toString();
        String senderName = message.getFrom().getFirstName() != null ? message.getFrom().getFirstName() : "Unknown";
        String groupName = message.getChat().getTitle() != null ? message.getChat().getTitle() : senderName;

        log.info("[Bot] Processing: chatId={} from={} text={}", chatId, senderName, text);

        // --- Slash commands ---
        if (text.equalsIgnoreCase("/help")) {
            send(chatId, helpText());
            return;
        }
        if (text.equalsIgnoreCase("/status")) {
            send(chatId, getStatusSummary());
            return;
        }
        if (text.toLowerCase().startsWith("/bug")) {
            String bugText = text.replaceFirst("(?i)/bug\\s*", "").trim();
            if (bugText.isBlank()) {
                send(chatId, "Vui lòng mô tả sự cố sau lệnh /bug\nVí dụ: <code>/bug Không đăng nhập được app</code>");
                return;
            }
            text = bugText;
        }

        // --- AI Triage pipeline (reads ALL messages, no tag needed) ---
        IncomingMessage incoming = new IncomingMessage(
                messageId, Platform.telegram, chatId, groupName,
                senderId, senderName, text, List.of(), OffsetDateTime.now()
        );

        ProcessResult result = incidentService.process(incoming);
        log.info("[Bot] Result: status={} incidentId={}", result.status(), result.incidentId());

        // Reply on created, merged, needs_confirmation — stay silent on ignored/duplicate
        switch (result.status()) {
            case "created", "merged", "needs_confirmation" -> send(chatId, result.message());
            case "ignored" -> log.debug("[Bot] Bỏ qua tin nhắn không phải bug: {}", text);
            case "duplicate_message" -> log.debug("[Bot] Tin nhắn đã xử lý trước đó");
            default -> send(chatId, result.message());
        }
    }

    private void registerCommands() {
        try {
            List<BotCommand> commands = new ArrayList<>();
            commands.add(new BotCommand("bug", "Bao cao su co hoac loi he thong moi"));
            commands.add(new BotCommand("status", "Xem danh sach incident dang mo"));
            commands.add(new BotCommand("help", "Huong dan su dung bot"));
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
            log.info("[Bot] Da dang ky {} lenh voi Telegram", commands.size());
        } catch (TelegramApiException e) {
            log.warn("[Bot] Khong the dang ky lenh: {}", e.getMessage());
        }
    }

    private void send(String chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .build());
            log.debug("[Bot] Sent to chatId={}", chatId);
        } catch (TelegramApiException e) {
            // HTML parse error? Retry as plain text
            log.warn("[Bot] HTML send failed, retrying as plain text: {}", e.getMessage());
            try {
                execute(SendMessage.builder().chatId(chatId).text(text).build());
            } catch (TelegramApiException ex) {
                log.error("[Bot] Send failed completely: {}", ex.getMessage());
            }
        }
    }

    private String getStatusSummary() {
        List<Incident> open = incidentRepository.findByStatusIn(
                List.of(IncidentStatus.Open, IncidentStatus.InProgress));
        if (open.isEmpty()) return "✅ Hiện không có incident nào đang mở.";
        StringBuilder sb = new StringBuilder("📋 <b>Incident đang mở (" + open.size() + "):</b>\n\n");
        for (Incident inc : open) {
            sb.append("• <code>").append(inc.getJiraIssueKey()).append("</code> [")
              .append(inc.getSeverity()).append("] ").append(escapeHtml(inc.getTitle())).append("\n");
        }
        return sb.toString();
    }

    private String helpText() {
        return """
                🤖 <b>AI Incident Assistant</b>

                Tôi tự động đọc <b>mọi tin nhắn</b> trong group và phát hiện sự cố kỹ thuật.

                <b>Lệnh hỗ trợ:</b>
                /bug [mô tả] – Báo cáo lỗi ngay lập tức
                /status – Xem danh sách incident đang xử lý
                /help – Hướng dẫn này

                <b>Hoặc nhắn tự nhiên – không cần tag bot:</b>
                <i>Bot sẽ tự nhận diện tin nhắn chứa lỗi và tạo ticket!</i>

                <i>Ví dụ: "Hệ thống thanh toán bị lỗi, nhiều khách không mua được"</i>
                """;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
