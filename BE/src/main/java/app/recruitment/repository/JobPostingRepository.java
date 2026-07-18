package app.recruitment.repository;

import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findByStatus(JobStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findAll(Pageable pageable);

    @Query("""
            SELECT j FROM JobPosting j
            JOIN FETCH j.recruiter r
            LEFT JOIN FETCH r.company
            LEFT JOIN FETCH j.company
            WHERE j.id = :id
            """)
    Optional<JobPosting> findByIdWithRecruiterAndCompany(@Param("id") Long id);

    List<JobPosting> findByRecruiterId(Long recruiterId);

    List<JobPosting> findByTitleContainingIgnoreCase(String keyword);

    List<JobPosting> findByStatus(JobStatus status);

    long countByRecruiterIdAndStatus(Long recruiterId, JobStatus status);

    List<JobPosting> findTop10ByStatusOrderByCreatedAtDesc(JobStatus status);

    @Query("SELECT j FROM JobPosting j WHERE " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND j.status = 'PUBLISHED'")
    List<JobPosting> searchJobs(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT j FROM JobPosting j LEFT JOIN FETCH j.extractedSkills WHERE j.id IN :ids")
    List<JobPosting> findAllByIdsWhithSkills(@Param("ids") List<Long> ids);

    List<JobPosting> findByRecruiterIdAndStatusNot(Long recruiterId, JobStatus status);
}

