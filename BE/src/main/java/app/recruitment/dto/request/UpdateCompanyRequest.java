package app.recruitment.dto.request;

import lombok.Data;

@Data
public class UpdateCompanyRequest {
    private String name;
    private String description;
    private String website;
    
    // Các trường bổ sung khớp với FE
    private String industry;
    private String size;
    private String foundedYear;
    private String address;
    private String phone;
    private String email;
    
    // Logo và CoverImage FE xử lý upload riêng và gửi chuỗi URL, 
    // hoặc bạn có thể nhận MultipartFile nếu muốn upload trực tiếp tại đây.
    // Ở đây giả định FE gửi link ảnh sau khi upload xong.
    private String logoUrl;
    private String coverImageUrl;
}