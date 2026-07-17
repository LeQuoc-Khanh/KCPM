package app.recruitment.service;

import app.auth.model.User;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.recruitment.dto.request.UpdateCompanyRequest;
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

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CompanyService companyService;

    @Test
    void getById_ShouldReturnCompany() {
        Company mockCompany = new Company();
        mockCompany.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        assertNotNull(companyService.getById(1L));
    }

    @Test
    void getMyCompany_ShouldReturnCompany() {
        Company mockCompany = new Company();
        when(companyRepository.findByRecruiterId(10L)).thenReturn(Optional.of(mockCompany));
        assertNotNull(companyService.getMyCompany(10L));
    }

    @Test
    void updateCompany_WhenCompanyExists_ShouldUpdateAndSave() {
        Company existingCompany = new Company();
        existingCompany.setId(1L);
        
        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName("New Tech");
        request.setLogoUrl("new_logo.png");

        when(companyRepository.findByRecruiterId(10L)).thenReturn(Optional.of(existingCompany));
        when(companyRepository.save(any(Company.class))).thenReturn(existingCompany);

        Company result = companyService.updateCompany(10L, request);

        assertEquals("New Tech", result.getName());
        assertEquals("new_logo.png", result.getLogoUrl());
        verify(companyRepository).save(existingCompany);
    }

    @Test
    void updateCompany_WhenCompanyNotExists_ShouldCreateNew() {
        User recruiter = new User();
        recruiter.setId(10L);

        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName("StartUp");

        when(companyRepository.findByRecruiterId(10L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(recruiter));
        
        Company savedCompany = new Company();
        savedCompany.setName("StartUp");
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        Company result = companyService.updateCompany(10L, request);

        assertNotNull(result);
        assertEquals("StartUp", result.getName());
    }
}