package app.recruitment.controller;

import app.content.model.Company;
import app.exception.GlobalExceptionHandler;
import app.recruitment.dto.request.UpdateCompanyRequest;
import app.recruitment.service.CompanyService;
import app.service.CloudinaryService;
import app.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecruiterCompanyControllerTest {

    private static final long RECRUITER_ID = 46L;

    @Mock
    private CompanyService companyService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CloudinaryService cloudinaryService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RecruiterCompanyController controller = new RecruiterCompanyController(
                companyService,
                securityUtils,
                cloudinaryService
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET-COMP-01 - Recruiter lấy công ty thành công")
    void getMyCompany_returnsCompany() throws Exception {
        Company company = Company.builder().id(15L).name("CareerMate").build();
        when(securityUtils.getCurrentUserId()).thenReturn(RECRUITER_ID);
        when(companyService.getMyCompany(RECRUITER_ID)).thenReturn(company);

        mockMvc.perform(get("/api/recruiter/company/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.name").value("CareerMate"));

        verify(companyService).getMyCompany(RECRUITER_ID);
    }

    @Test
    @DisplayName("GET company chưa tồn tại trả HTTP 200 với body rỗng theo source")
    void getMyCompany_returnsEmptyBodyWhenCompanyDoesNotExist() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(RECRUITER_ID);
        when(companyService.getMyCompany(RECRUITER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/recruiter/company/me"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @ParameterizedTest(name = "BVA hợp lệ: name dài {0} ký tự")
    @ValueSource(ints = {1, 2, 128, 254, 255})
    void updateMyCompany_acceptsValidBoundaryLengths(int length) throws Exception {
        String name = "A".repeat(length);
        when(securityUtils.getCurrentUserId()).thenReturn(RECRUITER_ID);
        when(companyService.updateCompany(eq(RECRUITER_ID), any(UpdateCompanyRequest.class)))
                .thenAnswer(invocation -> {
                    UpdateCompanyRequest request = invocation.getArgument(1);
                    return Company.builder().id(15L).name(request.getName()).build();
                });

        mockMvc.perform(put("/api/recruiter/company/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        verify(companyService).updateCompany(eq(RECRUITER_ID), any(UpdateCompanyRequest.class));
    }

    @ParameterizedTest(name = "BVA không hợp lệ: name dài {0} ký tự")
    @ValueSource(ints = {0, 256})
    void updateMyCompany_rejectsInvalidBoundaryLengths(int length) throws Exception {
        mockMvc.perform(put("/api/recruiter/company/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest("A".repeat(length))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(companyService);
    }

    @Test
    @DisplayName("PUT-COMP-02 - name null trả HTTP 400")
    void updateMyCompany_rejectsNullName() throws Exception {
        mockMvc.perform(put("/api/recruiter/company/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(companyService);
    }

    @Test
    @DisplayName("UPLOAD-01 - Upload ảnh công ty trả URL HTTPS")
    void uploadCompanyImage_returnsSecureUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "company.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3}
        );
        when(cloudinaryService.uploadCompanyImage(any()))
                .thenReturn("https://cdn.example/company.png");

        mockMvc.perform(multipart("/api/recruiter/company/upload-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://cdn.example/company.png"));
    }

    @Test
    @DisplayName("Upload ảnh lỗi được GlobalExceptionHandler chuyển thành HTTP 500")
    void uploadCompanyImage_returnsInternalServerErrorWhenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "company.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
        );
        when(cloudinaryService.uploadCompanyImage(any()))
                .thenThrow(new RuntimeException("Cloudinary unavailable"));

        mockMvc.perform(multipart("/api/recruiter/company/upload-image").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String jsonRequest(String name) throws Exception {
        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName(name);
        request.setDescription("BVA company test");
        request.setWebsite("https://careermate.vn");
        request.setIndustry("Information Technology");
        request.setSize("51-200");
        request.setFoundedYear("2020");
        request.setAddress("Ho Chi Minh City");
        request.setPhone("0901234567");
        request.setEmail("contact@careermate.vn");
        return objectMapper.writeValueAsString(request);
    }
}
