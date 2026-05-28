package app.admin.dto.response;

import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DashboardSummaryResponse {
    private long totalRecruiters;
    private long totalCandidates;
    private long totalActiveJobs;
    private long totalApplications;
    private long totalArticles;
}
