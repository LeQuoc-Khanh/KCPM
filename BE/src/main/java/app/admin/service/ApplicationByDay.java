package app.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import app.admin.dto.response.ApplicationsByDayResponse;
import app.admin.repository.ApplicaionByDayRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.sql.Date;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationByDay {

    private final ApplicaionByDayRepository adminDashboardApplicaionByDayRepository;

    public List<ApplicationsByDayResponse> getApplicationsChart(int days) {
        if (days <= 0) days = 7;

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(days - 1);

        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Object[]> rows = adminDashboardApplicaionByDayRepository.countApplicationsPerDay(from, to);

        Map<LocalDate, Long> countMap = new HashMap<>();
        for (Object[] r : rows) {
            // cột date() trong Postgres thường về java.sql.Date
            LocalDate d = ((Date) r[0]).toLocalDate();
            long c = ((Number) r[1]).longValue();
            countMap.put(d, c);
        }

        List<ApplicationsByDayResponse> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            long c = countMap.getOrDefault(d, 0L);
            result.add(new ApplicationsByDayResponse(toVietnamDayLabel(d.getDayOfWeek()), c));
        }

        return result;
    }

    private String toVietnamDayLabel(DayOfWeek dow) {
        // Java: MONDAY=1..SUNDAY=7
        // VN: Thứ 2 (Mon) .. Thứ 7 (Sat), Chủ Nhật (Sun) -> Cần +1 vào value
        return (dow == DayOfWeek.SUNDAY) ? "CN" : "T" + (dow.getValue() + 1);
    }
}