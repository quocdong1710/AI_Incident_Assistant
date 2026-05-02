package com.ptit.aia.service;

import com.ptit.aia.config.AiaProperties;
import com.ptit.aia.domain.Severity;
import com.ptit.aia.dto.IncomingMessage;
import com.ptit.aia.dto.TriageResult;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TriageService {
    private static final Logger log = LoggerFactory.getLogger(TriageService.class);
    private final AiaProperties properties;
    private final GeminiTriageService geminiService;

    public TriageService(AiaProperties properties, GeminiTriageService geminiService) {
        this.properties = properties;
        this.geminiService = geminiService;
    }

    public TriageResult analyze(IncomingMessage message) {
        // Try Gemini AI first
        TriageResult geminiResult = geminiService.analyze(message.text());
        if (geminiResult != null) {
            log.info("[Triage] Dung ket qua Gemini AI: isBug={} severity={}", geminiResult.bugReport(), geminiResult.severity());
            return geminiResult;
        }

        // Fallback: rule-based
        log.info("[Triage] Fallback sang rule-based");
        return analyzeRuleBased(message);
    }

    private TriageResult analyzeRuleBased(IncomingMessage message) {
        String text = message.text().toLowerCase(Locale.ROOT);
        double confidence = score(text);
        boolean bug = confidence >= properties.getConfidenceThreshold();
        boolean uncertain = confidence >= properties.getUncertainThreshold() && confidence < properties.getConfidenceThreshold();
        Severity severity = detectSeverity(text);
        String component = detectComponent(text);
        String language = looksEnglish(text) ? "en" : "vi";
        String title = buildTitle(message.text(), component, severity, language);
        String response = generateResponse(severity, language);
        return new TriageResult(bug, uncertain, confidence, title, message.text(), component, severity,
                detectImpact(text), detectEnvironment(text), null, response, language);
    }

    private double score(String text) {
        double score = 0.05;
        String[] keywords = {"loi", "bug", "error", "fail", "failed", "khong vao", "crash",
                "500", "payment", "thanh toan", "login", "dang nhap", "khong duoc",
                "lỗi", "không vào", "thanh toán", "đăng nhập", "không được", "sập", 
                "trắng xóa", "timeout", "hỏng", "chết", "chậm"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score += 0.35; // Tăng trọng số mỗi từ khóa để bù lại khi AI chết
            }
        }
        if (text.startsWith("/bug") || text.startsWith("/incident") || text.startsWith("/report")) {
            score += 0.8;
        }
        if (text.contains("@")) score += 0.15;
        return Math.min(score, 1.0);
    }

    private Severity detectSeverity(String text) {
        if (text.contains("payment") || text.contains("thanh toán") || text.contains("mat du lieu")
                || text.contains("down") || text.contains("toan bo") || text.contains("toàn bộ")
                || text.contains("loi thanh toan") || text.contains("lỗi thanh toán")
                || text.contains("sập") || text.contains("500") || text.contains("trắng xóa")
                || text.contains("không làm việc được") || text.contains("gấp")) {
            return Severity.P1;
        }
        if (text.contains("nhieu user") || text.contains("many users") || text.contains("không login")
                || text.contains("khong login") || text.contains("khong vao") || text.contains("core")
                || text.contains("nhieu nguoi") || text.contains("nhiều người") || text.contains("timeout")) {
            return Severity.P2;
        }
        return Severity.P3;
    }

    private String detectComponent(String text) {
        if (text.contains("login") || text.contains("dang nhap") || text.contains("đăng nhập")) return "Login";
        if (text.contains("payment") || text.contains("thanh toan") || text.contains("thanh toán")) return "Payment";
        if (text.contains("api") || text.contains("500")) return "Backend/API";
        if (text.contains("ui") || text.contains("giao dien") || text.contains("giao diện")) return "Frontend/UI";
        return "General";
    }

    private String detectImpact(String text) {
        if (text.contains("toan bo") || text.contains("toàn bộ") || text.contains("all users")) return "toàn bộ người dùng";
        if (text.contains("nhieu") || text.contains("nhiều") || text.contains("many")) return "nhiều (>10)";
        if (text.contains("mot so") || text.contains("một số") || text.contains("some")) return "một số người dùng";
        return "chưa xác định";
    }

    private String detectEnvironment(String text) {
        if (text.contains("staging")) return "Staging";
        if (text.contains("dev")) return "Dev";
        if (text.contains("prod") || text.contains("production")) return "Production";
        return null;
    }

    private boolean looksEnglish(String text) {
        return text.contains("error") || text.contains("failed") || text.contains("users")
                || text.contains("payment") || text.contains("since");
    }

    private String buildTitle(String original, String component, Severity severity, String language) {
        String prefix = language.equals("en") ? severity + " incident" : "Su co " + component;
        String compact = original.replaceAll("\\s+", " ").trim();
        if (compact.length() > 70) compact = compact.substring(0, 70);
        return (prefix + ": " + compact).substring(0, Math.min(100, prefix.length() + 2 + compact.length()));
    }

    private String generateResponse(Severity severity, String language) {
        if (language.equals("en")) {
            return switch (severity) {
                case P1 -> "We have recorded a critical incident and forwarded it to the technical team. We will update within 30 minutes.";
                case P2 -> "We have recorded the issue and are checking. The team will respond within 2 hours.";
                case P3 -> "We have recorded your support request and will respond today.";
            };
        } else {
            return switch (severity) {
                case P1 -> "Chúng tôi đã ghi nhận sự cố nghiêm trọng và chuyển đến đội kỹ thuật xử lý. Team sẽ cập nhật trong vòng 30 phút.";
                case P2 -> "Chúng tôi đã ghi nhận vấn đề và đang kiểm tra. Team sẽ phản hồi trong vòng 2 giờ.";
                case P3 -> "Chúng tôi đã ghi nhận yêu cầu hỗ trợ và sẽ phản hồi trong ngày.";
            };
        }
    }
}
