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

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {


    // ✅ thêm EntityGraph ở đây
    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findByStatus(JobStatus status, Pageable pageable);

    // ✅ thêm EntityGraph cho findAll paging (admin ALL đang gọi findAll(pageable))
    @Override
    @EntityGraph(attributePaths = {"company"})
    Page<JobPosting> findAll(Pageable pageable);
  
    // Các hàm tìm kiếm cơ bản
    List<JobPosting> findByRecruiterId(Long recruiterId);
    List<JobPosting> findByTitleContainingIgnoreCase(String keyword);
    List<JobPosting> findByStatus(JobStatus status);

    // 1. Hàm đếm cho Dashboard (Sử dụng cho RecruiterDashboardService)
    long countByRecruiterIdAndStatus(Long recruiterId, JobStatus status);

    // Hàm lấy danh sách mới nhất
    List<JobPosting> findTop10ByStatusOrderByCreatedAtDesc(JobStatus status);

    // 2. Hàm tìm kiếm nâng cao (Đã sửa 'OPEN' thành 'PUBLISHED')
    @Query("SELECT j FROM JobPosting j WHERE " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND j.status = 'PUBLISHED'") // Sửa thành PUBLISHED để khớp với logic Service
    List<JobPosting> searchJobs(@Param("keyword") String keyword);

    // 3. Hàm lấy jobs kèm skills 
    @Query("SELECT DISTINCT j FROM JobPosting j LEFT JOIN FETCH j.extractedSkills WHERE j.id IN :ids")
    List<JobPosting> findAllByIdsWhithSkills(@Param("ids") List<Long> ids);
    List<JobPosting> findByRecruiterIdAndStatusNot(Long recruiterId, JobStatus status);
}
