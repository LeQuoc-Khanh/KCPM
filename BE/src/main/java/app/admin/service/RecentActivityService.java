package app.admin.service;

import app.admin.dto.response.RecentActivityResponse;
import app.admin.repository.RecentActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentActivityService {

    private final RecentActivityRepository adminActivityRepository;

    public List<RecentActivityResponse> getRecentActivities(int limit) {
        if (limit <= 0) limit = 5;
        List<Object[]> rows = adminActivityRepository.findRecentApplicationActivities(limit);
        return mapRows(rows);
    }

    // ✅ thêm method để controller /all-activities gọi được
    public List<RecentActivityResponse> getAllActivities(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        int offset = page * size;
        List<Object[]> rows = adminActivityRepository.findApplicationActivitiesPaged(size, offset);
        return mapRows(rows);
    }

    private List<RecentActivityResponse> mapRows(List<Object[]> rows) {
        List<RecentActivityResponse> result = new ArrayList<>();
        Instant now = Instant.now();

        for (Object[] r : rows) {
            Long applicationId = ((Number) r[0]).longValue();
            String candidateName = (String) r[1];
            String companyName = (String) r[2];

            Instant createdAt = parseInstant(r[3], now);

            String message = String.format("%s vừa ứng tuyển vào %s", candidateName, companyName);

            result.add(RecentActivityResponse.builder()
                    .refId(applicationId)
                    .message(message)
                    .createdAt(createdAt)
                    .timeAgo(toTimeAgoVi(createdAt, now))
                    .build());
        }
        return result;
    }

    private Instant parseInstant(Object tsObj, Instant fallback) {
        if (tsObj instanceof Timestamp ts) return ts.toInstant();
        if (tsObj instanceof Instant i) return i;
        return fallback;
    }

    private String toTimeAgoVi(Instant createdAt, Instant now) {
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";

        long hours = ChronoUnit.HOURS.between(createdAt, now);
        if (hours < 24) return hours + " giờ trước";

        long days = ChronoUnit.DAYS.between(createdAt, now);
        return days + " ngày trước";
    }
}
