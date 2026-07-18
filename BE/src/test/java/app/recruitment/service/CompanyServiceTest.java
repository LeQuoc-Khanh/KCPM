package app.recruitment.service;

import app.auth.model.User;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.recruitment.dto.request.UpdateCompanyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private static final long RECRUITER_ID = 46L;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("UT-COMP-01 - getById tìm thấy công ty")
    void getById_returnsCompanyWhenFound() {
        Company company = Company.builder().id(15L).name("CareerMate").build();
        when(companyRepository.findById(15L)).thenReturn(Optional.of(company));
        assertSame(company, companyService.getById(15L));
    }

    @Test
    @DisplayName("UT-COMP-02 - getById không tìm thấy trả null")
    void getById_returnsNullWhenMissing() {
        when(companyRepository.findById(404L)).thenReturn(Optional.empty());
        assertNull(companyService.getById(404L));
    }

    @Test
    @DisplayName("UT-COMP-03 - getMyCompany tìm theo recruiter")
    void getMyCompany_returnsRecruiterCompany() {
        Company company = Company.builder().id(15L).name("CareerMate").build();
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.of(company));
        assertSame(company, companyService.getMyCompany(RECRUITER_ID));
    }

    @Test
    @DisplayName("UT-COMP-04 - Recruiter chưa có công ty trả null")
    void getMyCompany_returnsNullWhenRecruiterHasNoCompany() {
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.empty());
        assertNull(companyService.getMyCompany(RECRUITER_ID));
    }

    @Test
    @DisplayName("UT-COMP-05 - Cập nhật toàn bộ dữ liệu và URL")
    void updateCompany_updatesExistingCompanyAndImages() {
        Company company = Company.builder()
                .id(15L).name("Old").logoUrl("old-logo").coverImageUrl("old-cover").build();
        UpdateCompanyRequest request = nominalRequest();
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);

        Company result = companyService.updateCompany(RECRUITER_ID, request);

        assertSame(company, result);
        assertEquals("CareerMate", result.getName());
        assertEquals("Company description", result.getDescription());
        assertEquals("https://careermate.vn", result.getWebsite());
        assertEquals("Information Technology", result.getIndustry());
        assertEquals("51-200", result.getSize());
        assertEquals("2020", result.getFoundedYear());
        assertEquals("Ho Chi Minh City", result.getAddress());
        assertEquals("0901234567", result.getPhone());
        assertEquals("contact@careermate.vn", result.getEmail());
        assertEquals("new-logo", result.getLogoUrl());
        assertEquals("new-cover", result.getCoverImageUrl());
        verifyNoInteractions(userRepository);
        verify(companyRepository).save(company);
    }

    @Test
    @DisplayName("UT-COMP-06 - Không gửi URL thì giữ ảnh cũ")
    void updateCompany_preservesImagesWhenUrlsAreNull() {
        Company company = Company.builder()
                .id(15L).name("Old").logoUrl("old-logo").coverImageUrl("old-cover").build();
        UpdateCompanyRequest request = nominalRequest();
        request.setLogoUrl(null);
        request.setCoverImageUrl(null);
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);

        Company result = companyService.updateCompany(RECRUITER_ID, request);

        assertEquals("old-logo", result.getLogoUrl());
        assertEquals("old-cover", result.getCoverImageUrl());
    }

    @Test
    @DisplayName("UT-COMP-07 - Tạo công ty khi recruiter chưa có company")
    void updateCompany_createsCompanyWhenRecruiterExists() {
        User recruiter = User.builder().id(RECRUITER_ID).fullName("Recruiter").build();
        UpdateCompanyRequest request = nominalRequest();
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(RECRUITER_ID)).thenReturn(Optional.of(recruiter));
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Company result = companyService.updateCompany(RECRUITER_ID, request);

        assertSame(recruiter, result.getRecruiter());
        assertEquals("CareerMate", result.getName());
        verify(companyRepository).save(result);
    }

    @Test
    @DisplayName("UT-COMP-08 - Không tìm thấy recruiter thì ném exception")
    void updateCompany_throwsWhenRecruiterDoesNotExist() {
        when(companyRepository.findByRecruiterId(RECRUITER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(RECRUITER_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> companyService.updateCompany(RECRUITER_ID, nominalRequest())
        );

        assertTrue(exception.getMessage().contains("Recruiter không tồn tại"));
        verify(companyRepository, never()).save(any());
    }

    private UpdateCompanyRequest nominalRequest() {
        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName("CareerMate");
        request.setDescription("Company description");
        request.setWebsite("https://careermate.vn");
        request.setIndustry("Information Technology");
        request.setSize("51-200");
        request.setFoundedYear("2020");
        request.setAddress("Ho Chi Minh City");
        request.setPhone("0901234567");
        request.setEmail("contact@careermate.vn");
        request.setLogoUrl("new-logo");
        request.setCoverImageUrl("new-cover");
        return request;
    }
}
