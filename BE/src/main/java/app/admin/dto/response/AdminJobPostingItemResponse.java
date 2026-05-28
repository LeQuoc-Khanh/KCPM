// AdminJobPostingItemResponse
package app.admin.dto.response;

import java.time.LocalDateTime;

public record AdminJobPostingItemResponse(
        Long id,
        String title,
        String companyName,
        String location,
        String status,
        LocalDateTime createdAt
) {}
