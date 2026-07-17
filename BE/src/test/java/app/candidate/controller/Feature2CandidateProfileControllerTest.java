package app.candidate.controller;

import app.auth.dto.response.MessageResponse;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.dto.request.CandidateProfileUpdateRequest;
import app.candidate.dto.response.CandidateProfileResponse;
import app.candidate.service.CandidateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Feature2CandidateProfileControllerTest {

    @Mock CandidateService candidateService;
    @Mock UserRepository userRepository;

    private CandidateProfileController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new CandidateProfileController(candidateService, userRepository);
        user = User.builder().id(44L).email("candidate@test.com").fullName("Candidate").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("candidate@test.com", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfile_shouldReturnProfile_whenProfileExists() {
        CandidateProfileResponse profile = CandidateProfileResponse.builder().id(1L).fullName("Candidate").build();
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.getProfileDTO(44L)).thenReturn(profile);

        ResponseEntity<?> response = controller.getMyProfile();

        assertEquals(200, response.getStatusCode().value());
        MessageResponse body = (MessageResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertSame(profile, body.getData());
    }

    @Test
    void getMyProfile_shouldReturnNullData_whenProfileMissing() {
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.getProfileDTO(44L)).thenThrow(new RuntimeException("missing"));

        ResponseEntity<?> response = controller.getMyProfile();

        assertEquals(200, response.getStatusCode().value());
        MessageResponse body = (MessageResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertNull(body.getData());
    }

    @Test
    void updateMyProfile_shouldReturnUpdatedProfile_whenServiceSucceeds() {
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        CandidateProfileResponse updated = CandidateProfileResponse.builder().id(1L).fullName("Updated").build();
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.getProfileDTO(44L)).thenReturn(updated);

        ResponseEntity<?> response = controller.updateMyProfile(request);

        assertEquals(200, response.getStatusCode().value());
        verify(candidateService).updateProfile(44L, request);
        MessageResponse body = (MessageResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertSame(updated, body.getData());
    }

    @Test
    void updateMyProfile_shouldReturnBadRequest_whenServiceFails() {
        CandidateProfileUpdateRequest request = new CandidateProfileUpdateRequest();
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.updateProfile(44L, request)).thenThrow(new RuntimeException("invalid"));

        ResponseEntity<?> response = controller.updateMyProfile(request);

        assertEquals(400, response.getStatusCode().value());
        MessageResponse body = (MessageResponse) response.getBody();
        assertFalse(body.getSuccess());
    }

    @Test
    void uploadCV_shouldAnalyzeAndReturnUpdatedProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "demo".getBytes());
        CandidateProfileResponse profile = CandidateProfileResponse.builder().id(1L).cvFilePath("https://cloud/cv.pdf").build();
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.getProfileDTO(44L)).thenReturn(profile);

        ResponseEntity<?> response = controller.uploadCV(file);

        assertEquals(200, response.getStatusCode().value());
        verify(candidateService).uploadAndAnalyzeCV(44L, file);
        MessageResponse body = (MessageResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertSame(profile, body.getData());
    }

    @Test
    void uploadAvatar_shouldReturnAvatarUrl_whenServiceSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "image".getBytes());
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(candidateService.uploadAvatar(44L, file)).thenReturn("https://cloud/avatar.png");

        ResponseEntity<?> response = controller.uploadAvatar(file);

        assertEquals(200, response.getStatusCode().value());
        MessageResponse body = (MessageResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertEquals("https://cloud/avatar.png", body.getData());
    }
}