package app.recruitment.service;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateSearchServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CandidateSearchServiceImpl candidateSearchService;

    @Test
    void searchCandidates_WithName_ShouldFilterByNameAndRole() {
        User user1 = User.builder().userRole(UserRole.CANDIDATE).fullName("Nguyen Van A").build();
        User user2 = User.builder().userRole(UserRole.CANDIDATE).fullName("Tran Thi B").build();
        User user3 = User.builder().userRole(UserRole.RECRUITER).fullName("Nguyen Van C").build(); // Khác role

        when(userRepository.findByStatus(null)).thenReturn(List.of(user1, user2, user3));

        // Test tìm kiếm có truyền param "Nguyen", có param skill/gpa để coverage luôn phần log.warn
        List<User> result = candidateSearchService.searchCandidates("Java", 3.2, "Nguyen");

        assertEquals(1, result.size()); // Chỉ lấy user1 vì user3 là Recruiter
        assertEquals("Nguyen Van A", result.get(0).getFullName());
    }

    @Test
    void searchCandidates_WithoutName_ShouldReturnAllCandidates() {
        User user1 = User.builder().userRole(UserRole.CANDIDATE).fullName("Candidate 1").build();
        User user2 = User.builder().userRole(UserRole.RECRUITER).fullName("Recruiter 1").build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<User> result = candidateSearchService.searchCandidates(null, null, null);

        assertEquals(1, result.size());
        assertEquals("Candidate 1", result.get(0).getFullName());
    }

    @Test
    void searchByJobDescription_ShouldReturnEmptyList() {
        assertTrue(candidateSearchService.searchByJobDescription("JD").isEmpty());
    }
}