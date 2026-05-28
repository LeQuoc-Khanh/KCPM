package app.content.repository;

import app.content.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByIsPublishedTrueOrderByCreatedAtDesc();
    List<Article> findByStatusOrderByCreatedAtDesc(String status);

}
