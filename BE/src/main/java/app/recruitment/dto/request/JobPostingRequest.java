package app.recruitment.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate; // Dùng LocalDate thay vì LocalDateTime cho input

@Data
public class JobPostingRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Yêu cầu không được để trống")
    private String requirements;

    private String salaryRange;

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    // Frontend gửi "yyyy-MM-dd", Backend cần parse đúng
    @NotNull(message = "Hạn nộp hồ sơ không được để trống")
    @FutureOrPresent(message = "Hạn nộp hồ sơ phải từ hôm nay trở đi")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expiryDate; 
    
    private String status; // Optional
}