package app.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ArticleCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    // optional
    @Size(max = 255)
    private String slug;

    // optional
    @Size(max = 255)
    private String thumbnailUrl;

    // optional (mặc định false)
    private Boolean isPublished;

    // optional (khi chưa có auth, tạm truyền authorId)
    private Long authorId;
}
