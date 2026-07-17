package app.candidate.controller;

import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateCV;
import app.candidate.repository.CandidateCVRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Feature2CandidateCVControllerTest {

    @Mock CandidateCVRepository cvRepository;
    @Mock UserRepository userRepository;

    private CandidateCVController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new CandidateCVController();
        ReflectionTestUtils.setField(controller, "cvRepository", cvRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        user = User.builder().id(44L).email("candidate@test.com").fullName("Candidate").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("candidate@test.com", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveCV_shouldCreateNewCV_whenPayloadIsValid() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cvTitle", "Backend CV");
        payload.put("templateType", "modern");
        payload.put("cvDataJson", "{\"skills\":[\"Java\"]}");

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(cvRepository.save(any(CandidateCV.class))).thenAnswer(invocation -> {
            CandidateCV cv = invocation.getArgument(0);
            cv.setId(9L);
            return cv;
        });

        ResponseEntity<?> response = controller.saveCV(payload);

        assertEquals(200, response.getStatusCode().value());
        CandidateCV body = (CandidateCV) response.getBody();
        assertEquals(9L, body.getId());
        assertEquals(user, body.getUser());
        assertEquals("Backend CV", body.getCvTitle());
    }

    @Test
    void saveCV_shouldUpdateExistingCV_whenIdExists() {
        CandidateCV existing = CandidateCV.builder().id(9L).user(user).cvTitle("Old").build();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", 9L);
        payload.put("cvTitle", "Updated");
        payload.put("templateType", "classic");
        payload.put("cvDataJson", "{}");

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(cvRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(cvRepository.save(any(CandidateCV.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateCV body = (CandidateCV) controller.saveCV(payload).getBody();

        assertEquals("Updated", body.getCvTitle());
        assertEquals("classic", body.getTemplateType());
    }

    @Test
    void saveCV_currentBehavior_allowsEmptyPayload() {
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(cvRepository.save(any(CandidateCV.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateCV body = (CandidateCV) controller.saveCV(new HashMap<>()).getBody();

        assertNull(body.getCvTitle());
        assertNull(body.getTemplateType());
        assertNull(body.getCvDataJson());
    }

    @Test
    void getMyCVs_shouldReturnCurrentUserCVs() {
        CandidateCV cv = CandidateCV.builder().id(1L).user(user).cvTitle("CV 1").build();
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(cvRepository.findByUserId(44L)).thenReturn(List.of(cv));

        ResponseEntity<List<CandidateCV>> response = controller.getMyCVs();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("CV 1", response.getBody().get(0).getCvTitle());
    }

    @Test
    void getCV_shouldReturnCV_whenIdExists() {
        CandidateCV cv = CandidateCV.builder().id(1L).user(user).cvTitle("CV 1").build();
        when(cvRepository.findById(1L)).thenReturn(Optional.of(cv));

        ResponseEntity<CandidateCV> response = controller.getCV(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CV 1", response.getBody().getCvTitle());
    }

    @Test
    void getCV_shouldThrow_whenIdDoesNotExist() {
        when(cvRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> controller.getCV(404L));
    }
}