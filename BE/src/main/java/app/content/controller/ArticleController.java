package app.content.controller;

import app.auth.dto.response.MessageResponse; // nếu đỏ -> sửa đúng package của MessageResponse
import app.content.dto.response.ArticleResponse;
import app.content.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService service;

    // ✅ GET /api/articles (public) -> chỉ published
    @GetMapping
    public ResponseEntity<MessageResponse> list() {
        List<ArticleResponse> data = service.listPublic();
        return ResponseEntity.ok(MessageResponse.success("Danh sách bài viết (public)", data));
    }

    // ✅ GET /api/articles/{id} (public) -> chỉ nếu published
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> get(@PathVariable Long id) {
        ArticleResponse data = service.getPublicById(id);
        return ResponseEntity.ok(MessageResponse.success("Chi tiết bài viết (public)", data));
    }
}
