package app.content.controller;

import app.auth.dto.response.MessageResponse;
import app.content.dto.request.ArticleCreateRequest;
import app.content.dto.request.ArticlePublishRequest;
import app.content.dto.request.ArticleUpdateRequest;
import app.content.dto.response.ArticleResponse;
import app.content.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService service;

    // =========================
    // 📌 DANH SÁCH BÀI VIẾT
    // =========================

    // ✅ GET /api/admin/articles  -> tất cả bài viết (admin)
    @GetMapping
    public ResponseEntity<MessageResponse> listAll() {
        List<ArticleResponse> data = service.listAdmin();
        return ResponseEntity.ok(
                MessageResponse.success("Danh sách bài viết (admin)", data)
        );
    }

    // ✅ GET /api/admin/articles/pending -> bài viết chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<MessageResponse> listPending() {
        List<ArticleResponse> data = service.listPendingAdmin();
        return ResponseEntity.ok(
                MessageResponse.success("Danh sách bài viết chờ duyệt", data)
        );
    }

    // =========================
    // 📌 CHI TIẾT BÀI VIẾT
    // =========================

    // ✅ GET /api/admin/articles/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getById(@PathVariable Long id) {
        ArticleResponse data = service.getAdminById(id);
        return ResponseEntity.ok(
                MessageResponse.success("Chi tiết bài viết (admin)", data)
        );
    }

    // =========================
    // ✍️ CRUD BÀI VIẾT
    // =========================

    // ✅ POST /api/admin/articles/create
    @PostMapping("/create")
    public ResponseEntity<MessageResponse> create(
            @Valid @RequestBody ArticleCreateRequest req
    ) {
        ArticleResponse data = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.success("Tạo bài viết thành công", data));
    }

    // ✅ PUT /api/admin/articles/{id}
    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest req
    ) {
        ArticleResponse data = service.update(id, req);
        return ResponseEntity.ok(
                MessageResponse.success("Cập nhật bài viết thành công", data)
        );
    }

    // =========================
    // 🚦 DUYỆT / TỪ CHỐI / PUBLISH
    // =========================

    // ✅ PATCH /api/admin/articles/{id}/approve  -> duyệt bài
    @PatchMapping("/{id}/approve")
    public ResponseEntity<MessageResponse> approve(@PathVariable Long id) {
        ArticleResponse data = service.approve(id);
        return ResponseEntity.ok(
                MessageResponse.success("Duyệt bài viết thành công", data)
        );
    }

    // ✅ PATCH /api/admin/articles/{id}/reject -> từ chối bài
    @PatchMapping("/{id}/reject")
    public ResponseEntity<MessageResponse> reject(@PathVariable Long id) {
        ArticleResponse data = service.reject(id);
        return ResponseEntity.ok(
                MessageResponse.success("Từ chối bài viết thành công", data)
        );
    }

    // ✅ PATCH /api/admin/articles/{id}/publish  (publish / unpublish)
    @PatchMapping("/{id}/publish")
    public ResponseEntity<MessageResponse> publish(
            @PathVariable Long id,
            @RequestBody ArticlePublishRequest req
    ) {
        ArticleResponse data = service.setPublish(id, req);
        return ResponseEntity.ok(
                MessageResponse.success("Cập nhật trạng thái publish", data)
        );
    }

    // =========================
    // 🗑️ XÓA
    // =========================

    // ✅ DELETE /api/admin/articles/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                MessageResponse.success("Xóa bài viết thành công")
        );
    }
}
