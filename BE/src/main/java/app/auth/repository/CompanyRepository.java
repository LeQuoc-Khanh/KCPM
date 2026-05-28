package app.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import app.content.model.Company;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // 1. Tìm công ty do một User (Recruiter) quản lý
    // Dùng khi Recruiter đăng nhập vào Dashboard để load thông tin công ty của họ
    Optional<Company> findByRecruiterId(Long recruiterId);

    // 2. Tìm kiếm công ty theo tên (cho ứng viên search công ty)
    List<Company> findByNameContainingIgnoreCase(String name);

    // 3. Kiểm tra xem tên công ty đã tồn tại chưa (tránh trùng lặp khi đăng ký)
    boolean existsByName(String name);
}