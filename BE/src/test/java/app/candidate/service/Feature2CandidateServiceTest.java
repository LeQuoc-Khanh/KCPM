package app.candidate.service;

import app.ai.models.Experience;
import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.dto.ContactDTO;
import app.ai.service.cv.gemini.dto.ExperienceDTO;
import app.ai.service.cv.gemini.dto.GeminiResponse;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.dto.request.CandidateProfileUpdateRequest;
import app.candidate.dto.response.CandidateProfileResponse;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Feature2CandidateServiceTest {

    @Mock CandidateProfileRepository candidateProfileRepository;
    @Mock UserRepository userRepository;
    @Mock CVAnalysisService cvAnalysisService;
    @Mock CloudinaryService cloudinaryService;
    @Mock CVAnalysisResultRepository cvAnalysisResultRepository;
    @Mock ObjectMapper objectMapper;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CandidateService candidateService;

    private User user;
    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        user = User.builder().id(44L).fullName("Phan Khanh Du").email("candidate@test.com").profileImageUrl("old-avatar.png").build();
        Experience experience = new Experience();
        experience.setCompany("Old Company");
        experience.setRole("Developer");
        profile = CandidateProfile.builder()
                .id(100L)
                .user(user)
                .fullName("Old Profile")
                .email("old@test.com")
                .skills(new ArrayList<>(List.of("Java")))
                .experiences(new ArrayList<>(List.of(experience)))
                .build();
        experience.setCandidateProfile(profile);
    }

    @Test
    void getProfileDTO_shouldMapEntityToResponse_whenProfileExists() {
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));

        CandidateProfileResponse result = candidateService.getProfileDTO(44L);

        assertEquals(100L, result.getId());
        assertEquals("Phan Khanh Du", result.getUserFullName());
        assertEquals("Old Profile", result.getFullName());
        assertEquals(List.of("Java"), result.getSkills());
        assertEquals(1, result.getExperiences().size());
        assertEquals("Developer", result.getExperiences().get(0).getRole());
    }

    @Test
    void getProfileDTO_shouldThrow_whenProfileDoesNotExist() {
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> candidateService.getProfileDTO(44L));
    }

    @Test
    void updateProfile_shouldCreateProfileAndSaveFields_whenNoProfileExists() {
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        request.setUserFullName("New User Name");
        request.setFullName("New Profile Name");
        request.setSkills(List.of("Spring", "SQL"));

        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.empty());
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateProfile saved = candidateService.updateProfile(44L, request);

        assertEquals("New User Name", user.getFullName());
        assertEquals("New Profile Name", saved.getFullName());
        assertEquals(List.of("Spring", "SQL"), saved.getSkills());
        verify(userRepository).save(user);
        verify(cvAnalysisResultRepository).deleteByUserId(44L);
    }

    @Test
    void updateProfile_shouldReplaceExperiences_whenExperiencesProvided() {
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        request.setExperiences(List.of(Map.of(
                "companyName", "New Company",
                "role", "QA",
                "description", "Test Feature 2",
                "startDate", "2025-01",
                "endDate", "Present"
        )));

        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateProfile saved = candidateService.updateProfile(44L, request);

        assertEquals(1, saved.getExperiences().size());
        assertEquals("New Company", saved.getExperiences().get(0).getCompany());
        assertSame(saved, saved.getExperiences().get(0).getCandidateProfile());
    }

    @Test
    void updateProfile_shouldSerializeEducations_whenEducationsProvided() throws Exception {
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        request.setEducations(List.of(Map.of("school", "UT")));

        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));
        when(objectMapper.writeValueAsString(request.getEducations())).thenReturn("[{\"school\":\"UT\"}]");
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateProfile saved = candidateService.updateProfile(44L, request);

        assertEquals("[{\"school\":\"UT\"}]", saved.getEducationJson());
    }

    @Test
    void uploadAndAnalyzeCV_shouldUploadAnalyzeMapAndPublishEvent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "demo".getBytes());
        GeminiResponse ai = new GeminiResponse();
        ai.setContact(ContactDTO.builder().name("AI Name").email("ai@test.com").phoneNumber("0909").address("HCM").linkedIn("linkedin").build());
        ai.setSkills(List.of("Java", "React"));
        ai.setExperiences(List.of(ExperienceDTO.builder().company("AI Co").role("Backend").description("Build API").build()));
        ai.setAboutMe("AI summary");

        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadFile(file)).thenReturn("https://cloud/cv.pdf");
        when(cvAnalysisService.analyzeCV(file)).thenReturn(ai);
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.empty());
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(invocation -> {
            CandidateProfile saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        CandidateProfile saved = candidateService.uploadAndAnalyzeCV(44L, file);

        assertEquals("AI Name", saved.getFullName());
        assertEquals("https://cloud/cv.pdf", saved.getCvFilePath());
        assertEquals(List.of("Java", "React"), saved.getSkills());
        assertEquals(1, saved.getExperiences().size());
        verify(cvAnalysisResultRepository).deleteByUserId(44L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void uploadAndAnalyzeCV_shouldNotFail_whenPointEventFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "demo".getBytes());
        GeminiResponse ai = new GeminiResponse();
        ai.setSkills(List.of("Java"));

        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadFile(file)).thenReturn("https://cloud/cv.pdf");
        when(cvAnalysisService.analyzeCV(file)).thenReturn(ai);
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenReturn(profile);
        doThrow(new RuntimeException("event failed")).when(eventPublisher).publishEvent(any());

        CandidateProfile saved = candidateService.uploadAndAnalyzeCV(44L, file);

        assertSame(profile, saved);
    }

    @Test
    void uploadAvatar_shouldUpdateUserImageUrl_whenProfileExists() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image".getBytes());
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));
        when(cloudinaryService.uploadFile(file)).thenReturn("https://cloud/avatar.png");

        String result = candidateService.uploadAvatar(44L, file);

        assertEquals("https://cloud/avatar.png", result);
        assertEquals("https://cloud/avatar.png", user.getProfileImageUrl());
        verify(userRepository).save(user);
        verify(candidateProfileRepository).save(profile);
    }

    @Test
    void uploadAvatar_shouldThrow_whenProfileMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image".getBytes());
        when(candidateProfileRepository.findByUserId(44L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> candidateService.uploadAvatar(44L, file));
        verify(cloudinaryService, never()).uploadFile(any());
    }
}