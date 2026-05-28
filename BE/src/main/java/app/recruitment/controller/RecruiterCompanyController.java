package app.recruitment.controller;

import app.content.model.Company;
import app.recruitment.dto.request.UpdateCompanyRequest;
import app.recruitment.service.CompanyService;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import app.service.CloudinaryService; 
import java.util.Map;

@RestController
@RequestMapping("/api/recruiter/company")
@RequiredArgsConstructor
public class RecruiterCompanyController {

    private final CompanyService companyService;
    private final SecurityUtils securityUtils;
    private final CloudinaryService cloudinaryService;

    // 1. API lấy thông tin công ty hiện tại
    // GET: /api/recruiter/company/me
    @GetMapping("/me")
    public ResponseEntity<Company> getMyCompany() {
        // Lấy ID của recruiter đang đăng nhập từ token
        Long recruiterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(companyService.getMyCompany(recruiterId));
    }

    // 2. API cập nhật thông tin công ty
    // PUT: /api/recruiter/company/me
    @PutMapping("/me")
    public ResponseEntity<Company> updateMyCompany(@RequestBody UpdateCompanyRequest request) {
        // Lấy ID của recruiter đang đăng nhập
        Long recruiterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(companyService.updateCompany(recruiterId, request));
    }

    // 3. API Upload ảnh (Logo hoặc Cover)
    // POST: /api/recruiter/company/upload-image
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadCompanyImage(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.uploadCompanyImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}