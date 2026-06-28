package app.recruitment.service;

import app.auth.model.User;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.recruitment.dto.request.UpdateCompanyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyService companyService;

    private User mockRecruiter;
    private Company mockCompany;
    private UpdateCompanyRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Giả lập Recruiter
        mockRecruiter = new User();
        mockRecruiter.setId(1L);

        // Giả lập Profile Công ty hiện tại
        mockCompany = new Company();
        mockCompany.setId(1L);
        mockCompany.setRecruiter(mockRecruiter);
        mockCompany.setName("Old Company Name");

        // Giả lập Request gửi lên từ Client
        mockRequest = new UpdateCompanyRequest();
        mockRequest.setName("Tech CareerMate Inc.");
        mockRequest.setDescription("Nền tảng AI tuyển dụng");
        mockRequest.setWebsite("https://careermate.com");
        mockRequest.setSize("100-500 nhân viên"); 

    }

    @Test
    void testGetMyCompany_Success() {
        when(companyRepository.findByRecruiterId(1L)).thenReturn(Optional.of(mockCompany));

        Company result = companyService.getMyCompany(1L);

        assertNotNull(result);
        assertEquals("Old Company Name", result.getName());
        verify(companyRepository, times(1)).findByRecruiterId(1L);
    }

    @Test
    void testUpdateCompany_ExistingCompany_Success() {
        // Giả lập DB tìm thấy công ty và lưu thành công
        when(companyRepository.findByRecruiterId(1L)).thenReturn(Optional.of(mockCompany));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArguments()[0]);

        Company updatedCompany = companyService.updateCompany(1L, mockRequest);

        // Kiểm tra xem dữ liệu có được cập nhật đúng từ request không
        assertNotNull(updatedCompany);
        assertEquals("Tech CareerMate Inc.", updatedCompany.getName());
        assertEquals("Nền tảng AI tuyển dụng", updatedCompany.getDescription());
        assertEquals("100-500 nhân viên", updatedCompany.getSize());
        
        verify(companyRepository, times(1)).save(any(Company.class));
    }
}