package app.candidate.controller;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.candidate.dto.response.CandidateProfileResponse;
import app.candidate.model.CandidateCV;
import app.candidate.repository.CandidateCVRepository;
import app.candidate.service.CandidateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CandidateProfileCVControllerTest {

    private static final String VALID_TOKEN = "valid-candidate-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String TEST_EMAIL = "dupk0207@ut.edu.vn";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CandidateService candidateService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateCVRepository cvRepository;

    private MockMvc profileMvc;
    private MockMvc cvMvc;
    private User candidate;

    @BeforeEach
    void setUp() {
        candidate = User.builder()
                .id(44L)
                .fullName("Phan Khanh Du")
                .email(TEST_EMAIL)
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .build();

        CandidateProfileController profileController = new CandidateProfileController(candidateService, userRepository);

        CandidateCVController cvController = new CandidateCVController();
        ReflectionTestUtils.setField(cvController, "cvRepository", cvRepository);
        ReflectionTestUtils.setField(cvController, "userRepository", userRepository);

        OncePerRequestFilter fakeJwtFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                String authHeader = request.getHeader("Authorization");
                if (!authHeaderIsValid(authHeader)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                TEST_EMAIL,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                        )
                );
                filterChain.doFilter(request, response);
            }
        };

        profileMvc = MockMvcBuilders.standaloneSetup(profileController)
                .addFilters(fakeJwtFilter)
                .build();
        cvMvc = MockMvcBuilders.standaloneSetup(cvController)
                .addFilters(fakeJwtFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tc_2_1_viewProfile_shouldReturnCurrentCandidateProfile() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        profileMvc.perform(get("/api/candidate/profile/me").header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Phan Khanh Du"));
    }

    @Test
    void tc_2_2_updateProfile_shouldSaveAndReturnReloadedProfile() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePayload(List.of("Java", "Spring Boot"), "Backend developer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skills", hasSize(2)));
    }

    @Test
    void tc_2_3_uploadCVSuccess_shouldAnalyzeAndReturnUpdatedProfile() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        MockMultipartFile cv = new MockMultipartFile("file", "cv.pdf", "application/pdf", "fake-pdf".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(cv)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void tc_2_4_uploadCVInvalid_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAndAnalyzeCV(eq(44L), any())).thenThrow(new RuntimeException("Invalid file"));

        MockMultipartFile invalidFile = new MockMultipartFile("file", "cv.exe", "application/octet-stream", "invalid".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(invalidFile)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid file")));
    }

    @Test
    void tc_2_5_saveCVBuilder_shouldSaveCVToMyCVs() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class))).thenReturn(sampleCV(1L, candidate, "CV Software Developer"));

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cvPayload("CV Software Developer", "modern")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvTitle").value("CV Software Developer"));
    }

    @Test
    void tc_2_6_viewSavedCVList_shouldReturnCurrentCandidateCVs() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.findByUserId(44L)).thenReturn(List.of(sampleCV(1L, candidate, "CV 1")));

        cvMvc.perform(get("/api/candidate/cv-builder/my-cvs").header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cvTitle").value("CV 1"));
    }

    @Test
    void tc_2_7_viewCVDetail_shouldReturnCVById() throws Exception {
        when(cvRepository.findById(1L)).thenReturn(Optional.of(sampleCV(1L, candidate, "CV Detail")));

        cvMvc.perform(get("/api/candidate/cv-builder/1").header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cvTitle").value("CV Detail"));
    }

    @Test
    void tc_2_8_uploadAvatar_shouldSaveAvatarUrl() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAvatar(eq(44L), any())).thenReturn("https://cloudinary.example/avatar.png");

        MockMultipartFile avatar = new MockMultipartFile("file", "avatar.png", "image/png", "png".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .file(avatar)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("https://cloudinary.example/avatar.png"));
    }

    @Test
    void tc_2_9_updateProfileMissingRequiredField_currentCodeStillAllowsPartialUpdate() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"wrong-format\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_10_unauthorizedCandidateProfile_shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        profileMvc.perform(get("/api/candidate/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc_2_11_viewProfileWithInvalidToken_shouldReturnUnauthorized() throws Exception {
        profileMvc.perform(get("/api/candidate/profile/me").header("Authorization", bearer(INVALID_TOKEN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc_2_12_updateProfileWithInvalidToken_shouldReturnUnauthorized() throws Exception {
        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(INVALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePayload(List.of("Java"), "Invalid token test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc_2_13_updateProfileWithValidFullData_shouldReturnUpdatedProfile() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePayload(List.of("Java", "React", "MySQL"), "Full data profile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("0909123456"));
    }

    @Test
    void tc_2_14_updateProfileWithEmptySkills_shouldBeHandledByCurrentRule() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfileWithSkills(List.of()));

        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePayload(List.of(), "Empty skills")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills", hasSize(0)));
    }

    @Test
    void tc_2_15_updateProfileWithLongText_shouldNotReturnServerError() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        String longText = "Long text ".repeat(100);

        profileMvc.perform(put("/api/candidate/profile/me")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePayload(List.of("Java"), longText)))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_16_uploadCVWithoutFile_shouldReturnBadRequest() throws Exception {
        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_17_uploadCVWrongFormat_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAndAnalyzeCV(eq(44L), any())).thenThrow(new RuntimeException("Unsupported file type"));

        MockMultipartFile txt = new MockMultipartFile("file", "cv.txt", "text/plain", "not a cv".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(txt)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_18_uploadCVOverSizeLimit_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAndAnalyzeCV(eq(44L), any())).thenThrow(new RuntimeException("File too large"));

        MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", "x".repeat(1024).getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(largeFile)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_19_uploadCVDOCXSuccess_shouldReturnOk() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        MockMultipartFile docx = new MockMultipartFile("file", "cv.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(docx)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_20_uploadCVPDFSuccess_shouldReturnOk() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        MockMultipartFile pdf = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(pdf)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_21_uploadAvatarWithoutFile_shouldReturnBadRequest() throws Exception {
        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_22_uploadAvatarInvalidFormat_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAvatar(eq(44L), any())).thenThrow(new RuntimeException("Invalid image"));

        MockMultipartFile invalidAvatar = new MockMultipartFile("file", "avatar.txt", "text/plain", "bad".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .file(invalidAvatar)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_23_uploadAvatarOverSizeLimit_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAvatar(eq(44L), any())).thenThrow(new RuntimeException("File too large"));

        MockMultipartFile largeAvatar = new MockMultipartFile("file", "avatar.png", "image/png", "x".repeat(1024).getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .file(largeAvatar)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_24_saveCVBuilderEmptyData_currentCodeAllowsEmptyPayload() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class))).thenReturn(sampleCV(2L, candidate, null));

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_25_saveCVBuilderFullData_shouldReturnSavedCV() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class))).thenAnswer(invocation -> { CandidateCV cv = invocation.getArgument(0); cv.setId(3L); return cv; });

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cvPayload("Full CV Builder Test", "classic")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.cvTitle").value("Full CV Builder Test"));
    }

    @Test
    void tc_2_26_saveCVBuilderMissingName_currentCodeAllowsMissingTitle() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class))).thenReturn(sampleCV(4L, candidate, null));

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateType\":\"modern\",\"cvDataJson\":\"{}\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_27_saveCVBuilderMultipleTimes_shouldShowBothInMyCVs() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class)))
                .thenReturn(sampleCV(5L, candidate, "CV Number 1"))
                .thenReturn(sampleCV(6L, candidate, "CV Number 2"));
        when(cvRepository.findByUserId(44L)).thenReturn(List.of(
                sampleCV(5L, candidate, "CV Number 1"),
                sampleCV(6L, candidate, "CV Number 2")
        ));

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cvPayload("CV Number 1", "modern")))
                .andExpect(status().isOk());

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cvPayload("CV Number 2", "classic")))
                .andExpect(status().isOk());

        cvMvc.perform(get("/api/candidate/cv-builder/my-cvs").header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void tc_2_28_viewCVDetailNotFound_shouldReturnNotFoundStyleError() throws Exception {
        when(cvRepository.findById(999999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class, () ->
                cvMvc.perform(get("/api/candidate/cv-builder/999999").header("Authorization", bearer(VALID_TOKEN)))
        );
    }

    @Test
    void tc_2_29_viewCVDetailOfAnotherUser_currentCodeDoesNotCheckOwner() throws Exception {
        User otherUser = User.builder()
                .id(99L)
                .fullName("Other Candidate")
                .email("other@test.com")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .build();
        when(cvRepository.findById(10L)).thenReturn(Optional.of(sampleCV(10L, otherUser, "Other User CV")));

        cvMvc.perform(get("/api/candidate/cv-builder/10").header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvTitle").value("Other User CV"));
    }

    private boolean authHeaderIsValid(String authHeader) {
        return authHeader != null && authHeader.equals(bearer(VALID_TOKEN));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private CandidateProfileResponse sampleProfile() {
        return sampleProfileWithSkills(List.of("Java", "Spring Boot"));
    }

    private CandidateProfileResponse sampleProfileWithSkills(List<String> skills) {
        return CandidateProfileResponse.builder()
                .id(1L)
                .userFullName("Phan Khanh Du")
                .fullName("Phan Khanh Du")
                .email(TEST_EMAIL)
                .phoneNumber("0909123456")
                .address("Ho Chi Minh City")
                .aboutMe("Backend developer")
                .linkedInUrl("https://linkedin.com/in/dupk0207")
                .websiteUrl("https://portfolio.example.com")
                .avatarUrl("https://cloudinary.example/avatar.png")
                .cvFilePath("https://cloudinary.example/cv.pdf")
                .skills(skills)
                .experiences(List.of())
                .build();
    }

    private CandidateCV sampleCV(Long id, User owner, String title) {
        return CandidateCV.builder()
                .id(id)
                .user(owner)
                .cvTitle(title)
                .templateType("modern")
                .cvDataJson("{\"fullName\":\"Phan Khanh Du\"}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String profilePayload(List<String> skills, String aboutMe) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userFullName", "Phan Khanh Du");
        payload.put("fullName", "Phan Khanh Du");
        payload.put("email", TEST_EMAIL);
        payload.put("aboutMe", aboutMe);
        payload.put("phoneNumber", "0909123456");
        payload.put("address", "Ho Chi Minh City");
        payload.put("linkedInUrl", "https://linkedin.com/in/dupk0207");
        payload.put("websiteUrl", "https://portfolio.example.com");
        payload.put("skills", skills);
        payload.put("experiences", List.of(Map.of(
                "companyName", "ABC Company",
                "role", "Intern Developer",
                "description", "Developed web features"
        )));
        payload.put("educations", List.of(Map.of(
                "school", "University of Transport",
                "major", "Software Engineering",
                "year", "2026"
        )));
        return objectMapper.writeValueAsString(payload);
    }

    private String cvPayload(String title, String templateType) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "cvTitle", title,
                "templateType", templateType,
                "cvDataJson", "{\"fullName\":\"Phan Khanh Du\",\"email\":\"dupk0207@ut.edu.vn\",\"skills\":[\"Java\",\"React\"]}"
        ));
    }
}