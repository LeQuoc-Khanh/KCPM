package app.admin.dto.response;

public interface DashboardSummaryProjection {
    Long getCandidateTotal();
    Long getRecruiterTotal();
    Long getJobTotal();
    Long getApplicationTotal();
}
