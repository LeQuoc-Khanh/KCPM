package app.recruitment.repository;

import app.recruitment.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByCandidateIdAndJobPostingId(Long candidateId, Long jobPostingId);

    Optional<JobApplication> findByCandidateIdAndJobPostingId(Long candidateId, Long jobPostingId);

    long countByJobPostingRecruiterId(Long recruiterId);

    long countByJobPostingRecruiterIdAndAppliedAtAfter(Long recruiterId, LocalDateTime date);

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a WHERE a.jobPosting.recruiter.id = :recruiterId GROUP BY a.status")
    List<Object[]> countApplicationsByStatus(@Param("recruiterId") Long recruiterId);

    List<JobApplication> findByJobPostingId(Long jobPostingId);

    // Giữ lại hàm chuẩn này (trả về long)
    long countByJobPostingId(Long jobPostingId);

    List<JobApplication> findByJobPostingIdAndMatchScoreGreaterThanEqualOrderByMatchScoreDesc(Long jobPostingId, Integer minScore);

    List<JobApplication> findByCandidateId(Long candidateId);

    @Query("SELECT j FROM JobApplication j WHERE j.jobPosting.recruiter.id = :recruiterId ORDER BY j.appliedAt DESC")
    List<JobApplication> findRecentApplicationsByRecruiter(@Param("recruiterId") Long recruiterId, Pageable pageable);

    // ĐÃ XÓA dòng int countByJobPostingId(...) bị thừa ở đây
}