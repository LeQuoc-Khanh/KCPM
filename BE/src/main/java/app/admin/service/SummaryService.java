package app.admin.service;

import app.admin.dto.response.DashboardSummaryResponse;
import app.admin.dto.response.DashboardSummaryProjection;
import app.admin.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository adminDashboardRepository;

    public DashboardSummaryResponse getSummary() {
        DashboardSummaryProjection p = adminDashboardRepository.getDashboardSummary();

        return DashboardSummaryResponse.builder()
                .totalCandidates(p.getCandidateTotal())
                .totalRecruiters(p.getRecruiterTotal())
                .totalActiveJobs(p.getJobTotal())
                .totalApplications(p.getApplicationTotal())
                .build();
    }
}