package app.content.service;

import app.content.dto.request.ArticleCreateRequest;
import app.content.dto.request.ArticlePublishRequest;
import app.content.dto.request.ArticleUpdateRequest;
import app.content.dto.response.ArticleResponse;
import app.content.model.Article;
import app.content.repository.ContentArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ContentArticleRepository repo;

    // ====== Status constants (nhanh, đúng DB varchar) ======
    private static final String STATUS_PENDING  = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private ArticleResponse toResponse(Article a) {
        return ArticleResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .slug(a.getSlug())
                .thumbnailUrl(a.getThumbnailUrl())
                .isPublished(a.getIsPublished())
                .authorId(a.getAuthorId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                // ✅ thêm status để admin UI hiển thị & filter
                .status(a.getStatus())
                .build();
    }

    // ✅ Public: chỉ lấy bài đã publish
    public List<ArticleResponse> listPublic() {
        return repo.findByIsPublishedTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    // ✅ Public: lấy bài theo id nhưng chỉ nếu published
    public ArticleResponse getPublicById(Long id) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));
        if (!Boolean.TRUE.equals(a.getIsPublished())) {
            throw new RuntimeException("Bài viết chưa được xuất bản");
        }
        return toResponse(a);
    }

    // ✅ Admin: list tất cả
    public List<ArticleResponse> listAdmin() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    // ✅ Admin: list bài chờ duyệt
    public List<ArticleResponse> listPendingAdmin() {
        return repo.findByStatusOrderByCreatedAtDesc(STATUS_PENDING)
                .stream().map(this::toResponse).toList();
    }

    // ✅ Admin: get bất kỳ
    public ArticleResponse getAdminById(Long id) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));
        return toResponse(a);
    }

    // ✅ Admin: create
    // Quy ước: tạo bài xong -> PENDING + isPublished=false (chờ duyệt)
    public ArticleResponse create(ArticleCreateRequest req) {
        String slug = (req.getSlug() == null || req.getSlug().isBlank())
                ? toSlug(req.getTitle())
                : req.getSlug();

        Article a = Article.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .slug(slug)
                .thumbnailUrl(req.getThumbnailUrl())
                .authorId(req.getAuthorId())
                // ✅ moderation default
                .status(STATUS_PENDING)
                .isPublished(false)
                .build();

        return toResponse(repo.save(a));
    }

    // ✅ Admin: update
    public ArticleResponse update(Long id, ArticleUpdateRequest req) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));

        a.setTitle(req.getTitle());
        a.setContent(req.getContent());

        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            a.setSlug(req.getSlug());
        } else if (a.getSlug() == null || a.getSlug().isBlank()) {
            a.setSlug(toSlug(req.getTitle()));
        }

        a.setThumbnailUrl(req.getThumbnailUrl());

        // ❗ Với flow duyệt, không cho update isPublished trực tiếp ở đây nữa (tránh bypass duyệt)
        // Nếu bạn vẫn muốn cho admin override, có thể mở lại.
        // if (req.getIsPublished() != null) {
        //     a.setIsPublished(req.getIsPublished());
        // }

        if (req.getAuthorId() != null) {
            a.setAuthorId(req.getAuthorId());
        }

        return toResponse(repo.save(a));
    }

    // ✅ Admin: approve (duyệt bài)
    public ArticleResponse approve(Long id) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));

        a.setStatus(STATUS_APPROVED);
        a.setIsPublished(true);

        return toResponse(repo.save(a));
    }

    // ✅ Admin: reject (từ chối bài)
    public ArticleResponse reject(Long id) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));

        a.setStatus(STATUS_REJECTED);
        a.setIsPublished(false);

        return toResponse(repo.save(a));
    }

    // ✅ Admin: publish/unpublish nhanh (giữ lại theo code cũ)
    // Lưu ý: nếu bạn muốn flow duyệt nghiêm ngặt, có thể chỉ cho publish khi status=APPROVED.
    public ArticleResponse setPublish(Long id, ArticlePublishRequest req) {
        Article a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết id=" + id));

        a.setIsPublished(req.isPublished());

        // nếu publish=true mà chưa APPROVED thì tự set APPROVED cho hợp logic
        if (req.isPublished() && (a.getStatus() == null || STATUS_PENDING.equals(a.getStatus()))) {
            a.setStatus(STATUS_APPROVED);
        }

        // nếu publish=false thì không đổi status (tuỳ bạn); hoặc set REJECTED/…
        return toResponse(repo.save(a));
    }

    // ✅ Admin: delete
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Không tìm thấy bài viết id=" + id);
        }
        repo.deleteById(id);
    }

    // Helper: tạo slug từ title (không dấu, gạch ngang)
    private String toSlug(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-{2,}", "-");
        return s;
    }
}
