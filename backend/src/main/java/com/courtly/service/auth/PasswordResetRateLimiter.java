package com.courtly.service.auth;

import com.courtly.common.config.PasswordResetProperties;
import com.courtly.common.exception.ApiException;
import com.courtly.common.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gioi han so lan xin ma dat lai mat khau cho mot dia chi.
 *
 * <p>Dem theo dia chi nguoi dung nhap, <b>truoc</b> khi tra cuu tai khoan. Neu chi chan
 * khi tai khoan co that thi ke tan cong so sanh phan hoi la biet email nao da dang ky -
 * dung dieu ma quy tac "khong tiet lo email co ton tai" muon tranh.
 *
 * <p>Luu trong bo nho: du cho mot tien trinh (do an chay mot instance) va mat khi khoi dong
 * lai. Neu sau nay chay nhieu instance thi phai chuyen sang Redis hoac bang trong database.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    /** Don bo nho khi map phinh to, tranh giu vo han dia chi rac. */
    private static final int CLEANUP_THRESHOLD = 5_000;

    private final PasswordResetProperties properties;
    private final ConcurrentHashMap<String, Attempts> byDestination = new ConcurrentHashMap<>();

    private record Attempts(Instant windowStart, Instant lastSentAt, int count) {
    }

    /** Ghi nhan mot lan gui, hoac nem 429 neu qua day. */
    public void checkAndRecord(String destination) {
        Instant now = Instant.now();
        cleanUpIfCrowded(now);

        byDestination.compute(destination, (key, current) -> {
            if (current == null || current.windowStart().isBefore(now.minus(WINDOW))) {
                return new Attempts(now, now, 1);
            }

            long waitedSeconds = Duration.between(current.lastSentAt(), now).toSeconds();
            long remaining = properties.resendCooldownSeconds() - waitedSeconds;
            if (remaining > 0) {
                throw tooMany(remaining, "Vui long doi " + remaining + " giay roi gui lai ma.");
            }

            if (current.count() >= properties.maxSendsPerHour()) {
                long untilWindowEnds = Duration.between(now, current.windowStart().plus(WINDOW)).toSeconds();
                throw tooMany(untilWindowEnds,
                        "Ban da yeu cau gui ma qua nhieu lan. Vui long thu lai sau mot gio.");
            }

            return new Attempts(current.windowStart(), now, current.count() + 1);
        });
    }

    /** Chi dung trong test de bat dau tu trang thai sach. */
    public void reset(String destination) {
        byDestination.remove(destination);
    }

    /**
     * {@code retryAfterSeconds} nam trong {@code fieldErrors} de frontend chay dem nguoc
     * dung bang thoi gian con lai, thay vi doan lai tu dau.
     */
    private ApiException tooMany(long retryAfterSeconds, String message) {
        return ApiException.tooManyRequests(ErrorCode.RESET_TOO_MANY_REQUESTS, message,
                Map.of("retryAfterSeconds", String.valueOf(Math.max(1, retryAfterSeconds))));
    }

    private void cleanUpIfCrowded(Instant now) {
        if (byDestination.size() < CLEANUP_THRESHOLD) {
            return;
        }
        byDestination.values().removeIf(item -> item.windowStart().isBefore(now.minus(WINDOW)));
    }
}
