package app.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ArticleUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 255)
    private String slug;

    @Size(max = 255)
    private String thumbnailUrl;

    // optional
    private Boolean isPublished;

    // optional
    private Long authorId;
}
