package app.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import app.content.model.Article;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 1. Tìm bài viết theo Slug (để hiển thị bài chi tiết qua URL đẹp)
    // Ví dụ: careermate.com/blog/cach-viet-cv-chuan
    Optional<Article> findBySlug(String slug);

    // 2. Lấy danh sách bài viết ĐÃ xuất bản (cho trang News/Blog của ứng viên)
    // Hỗ trợ phân trang (Pageable) để không load hết 1000 bài một lúc
    Page<Article> findByIsPublishedTrue(Pageable pageable);

    // 3. Tìm bài viết theo từ khóa trong Tiêu đề (dùng cho thanh tìm kiếm)
    Page<Article> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    // 4. Lấy danh sách bài viết của một tác giả cụ thể (dùng cho Admin quản lý bài của mình)
    Page<Article> findByAuthorId(Long authorId, Pageable pageable);
}