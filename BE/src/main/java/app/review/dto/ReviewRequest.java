package app.review.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long companyId;
    private Integer rating;
    private String comment;
}