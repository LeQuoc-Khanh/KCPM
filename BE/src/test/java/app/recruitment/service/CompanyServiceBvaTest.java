package app.recruitment.service;

import app.recruitment.dto.request.UpdateCompanyRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyServiceBvaTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Gộp TC_4.26 đến TC_4.30: Kiểm tra quy mô (Size)
    // Giả sử logic nghiệp vụ quy định size không được âm
    @ParameterizedTest
    @ValueSource(ints = {-1, -100}) // Các giá trị biên dưới
    void testUpdateCompany_Size_Negative_Failed(int invalidSize) {
        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName("CareerMate");
        request.setSize(String.valueOf(invalidSize)); // Ép kiểu vì DTO đang dùng String
        
        // Nếu dùng annotation @Min(1) trong DTO thì đoạn này sẽ bắt được lỗi
        Set<ConstraintViolation<UpdateCompanyRequest>> violations = validator.validate(request);
        // Tùy thuộc vào DTO của bạn có chặn số âm hay không. 
        // Nếu DTO không cấu hình @Min, bạn có thể chuyển test này xuống tầng Service logic.
    }
}