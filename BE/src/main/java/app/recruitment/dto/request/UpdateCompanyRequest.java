package app.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCompanyRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty không được vượt quá 255 ký tự")
    private String name;

    private String description;
    private String website;
    private String industry;
    private String size;
    private String foundedYear;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
}