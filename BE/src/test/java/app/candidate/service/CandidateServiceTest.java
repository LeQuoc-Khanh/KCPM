package app.candidate.service;

import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.dto.ExperienceDTO;
import app.ai.service.cv.gemini.dto.GeminiResponse;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.dto.request.CandidateProfileUpdateRequest;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import app.gamification.event.PointEvent; // Đảm bảo import Event

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    // 1. Khai báo HÀNG GIẢ (Mocks)
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private CVAnalysisService cvAnalysisService;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private CVAnalysisResultRepository analysisResultRepo;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    // 2. Khai báo HÀNG THẬT (Service cần test)
    @InjectMocks
    private CandidateService candidateService;

    // =========================================================================
    // HÀM UPLOAD VÀ PHÂN TÍCH CV (uploadAndAnalyzeCV)
    // =========================================================================

    /**
     * Test Case UT-F4-CAN-001: Upload CV thành công (Profile chưa tồn tại, tạo mới)
     */
    @Test
    void uploadAndAnalyzeCV_WhenUserExists_ShouldCreateProfileAndPublishEvent() throws Exception {
        // Arrange: Chuẩn bị dữ liệu
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setFullName("Nguyen Van A");

        MockMultipartFile mockFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "dummy content".getBytes());
        String expectedUrl = "https://cloudinary.com/cv.pdf";

        // Giả lập AI trả về dữ liệu bằng Mockito
        GeminiResponse mockAiResult = mock(GeminiResponse.class);
        
        // Không cần mock Contact nữa. Mockito sẽ tự trả về null.
        // Code Service có "if (result.getContact() != null)" nên sẽ an toàn lướt qua.

        
        // Cài đặt hành vi cho các Mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cloudinaryService.uploadFile(any(MultipartFile.class))).thenReturn(expectedUrl);
        // ... (Giữ nguyên các đoạn code phía dưới)
        
        // Khi save, trả về một Profile có ID
        CandidateProfile savedProfile = new CandidateProfile();
        savedProfile.setId(100L);
        savedProfile.setCvFilePath(expectedUrl);
        savedProfile.setSkills(List.of("Java", "Spring Boot"));
        when(profileRepository.save(any(CandidateProfile.class))).thenReturn(savedProfile);

        // Act: Thực thi hàm
        CandidateProfile result = candidateService.uploadAndAnalyzeCV(userId, mockFile);

        // Assert: Kiểm chứng
        assertNotNull(result);
        assertEquals(expectedUrl, result.getCvFilePath());
        assertTrue(result.getSkills().contains("Java"));
        
        // Kiểm chứng Repository đã gọi hàm xóa cache
        verify(analysisResultRepo, times(1)).deleteByUserId(userId);
        
        // Kiểm chứng Event cộng điểm UPLOAD_CV đã được bắn ra 1 lần
        verify(eventPublisher, times(1)).publishEvent(any(PointEvent.class));
    }

    /**
     * Test Case lỗi cơ bản: Ném lỗi nếu User không tồn tại
     */
    @Test
    void uploadAndAnalyzeCV_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        MockMultipartFile mockFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            candidateService.uploadAndAnalyzeCV(99L, mockFile);
        });
        assertEquals("User not found: 99", exception.getMessage());
        
        // Xác nhận file không bị đẩy lên Cloud nếu user không tồn tại
        verifyNoInteractions(cloudinaryService);
    }

    // =========================================================================
    // HÀM CẬP NHẬT HỒ SƠ (updateProfile)
    // =========================================================================

    /**
     * Test Case UT-F4-CAN-002 & 003: Partial update và clear danh sách cũ
     */
    @Test
    void updateProfile_PartialUpdate_ShouldKeepOldDataAndAddNewExperiences() {
        // Arrange
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);

        CandidateProfile existingProfile = new CandidateProfile();
        existingProfile.setAddress("Hanoi"); // Địa chỉ CŨ
        
        // Danh sách kinh nghiệm CŨ (Cần bị clear)
        List<app.ai.models.Experience> oldExps = new ArrayList<>();
        oldExps.add(new app.ai.models.Experience()); 
        existingProfile.setExperiences(oldExps);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any(CandidateProfile.class))).thenReturn(existingProfile);

        // Tạo Request MỚI (Khuyết Address, nhưng có Experience mới)
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        request.setFullName("Ten Moi"); // Thay đổi tên
        request.setAddress(null); // Không thay đổi địa chỉ
        
        // Kinh nghiệm MỚI
        List<Map<String, Object>> newExps = List.of(
            Map.of("companyName", "Tech Corp", "role", "Dev")
        );
        request.setExperiences(newExps);

        // Act
        CandidateProfile result = candidateService.updateProfile(userId, request);

        // Assert
        assertEquals("Ten Moi", result.getFullName());
        assertEquals("Hanoi", result.getAddress()); // Địa chỉ CŨ phải được giữ nguyên
        
        // Kiểm tra danh sách kinh nghiệm đã bị ghi đè thành 1 phần tử MỚI
        assertEquals(1, result.getExperiences().size());
        assertEquals("Tech Corp", result.getExperiences().get(0).getCompany());
        
        verify(analysisResultRepo, times(1)).deleteByUserId(userId);
    }
}