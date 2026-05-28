package app.admin.report.repository;

import app.admin.report.model.ViolationReport;
import app.admin.report.model.enums.ReportStatus;
import app.admin.report.model.enums.ReportTargetType;
import app.admin.report.model.enums.ViolationReason;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationReportRepository extends JpaRepository<ViolationReport, Long> {

    long countByStatus(ReportStatus status);

    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Page<ViolationReport> findByStatus(ReportStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Page<ViolationReport> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Page<ViolationReport> findByStatusAndTargetType(ReportStatus status,
                                                ReportTargetType targetType,
                                                Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "handledBy"})
    Page<ViolationReport> findByStatusAndReason(ReportStatus status,
                                            ViolationReason reason,
                                            Pageable pageable);


}
