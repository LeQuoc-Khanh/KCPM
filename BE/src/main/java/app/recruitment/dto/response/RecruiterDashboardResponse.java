package app.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterDashboardResponse {
    private long totalActiveJobs;      // Số tin đang tuyển (Status = OPEN/PUBLISHED)
    private long totalCandidates;      // Tổng số hồ sơ nhận được
    private long newCandidatesToday;   // Số hồ sơ mới trong ngày hôm nay
    private Map<String, Long> pipelineStats; // Thống kê theo trạng thái (VD: PENDING: 10, INTERVIEW: 5...)
}