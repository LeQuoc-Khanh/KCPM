package app.candidate.controller;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.candidate.model.CandidateCV;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CandidateFeature2EdgeTest extends CandidateFeature2TestBase {

    @Test
    void tc_2_16_uploadCVWithoutFile_shouldReturnBadRequest() throws Exception {
        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_17_uploadCVWrongFormat_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAndAnalyzeCV(eq(44L), any())).thenThrow(new RuntimeException("Unsupported file type"));

        MockMultipartFile txt = new MockMultipartFile("file", "cv.txt", "text/plain", "not a cv".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(txt)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_18_uploadCVOverSizeLimit_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAndAnalyzeCV(eq(44L), any())).thenThrow(new RuntimeException("File too large"));

        MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", "x".repeat(1024).getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(largeFile)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_19_uploadCVDOCXSuccess_shouldReturnOk() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(candidateService.getProfileDTO(44L)).thenReturn(sampleProfile());

        MockMultipartFile docx = new MockMultipartFile("file", "cv.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/upload-cv")
                        .file(docx)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_20_uploadCVPDFSuccess_shouldReturnOk() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
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
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAvatar(eq(44L), any())).thenThrow(new RuntimeException("Invalid image"));

        MockMultipartFile invalidAvatar = new MockMultipartFile("file", "avatar.txt", "text/plain", "bad".getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .file(invalidAvatar)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_23_uploadAvatarOverSizeLimit_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(candidateService.uploadAvatar(eq(44L), any())).thenThrow(new RuntimeException("File too large"));

        MockMultipartFile largeAvatar = new MockMultipartFile("file", "avatar.png", "image/png", "x".repeat(1024).getBytes());

        profileMvc.perform(multipart("/api/candidate/profile/avatar")
                        .file(largeAvatar)
                        .header("Authorization", bearer(VALID_TOKEN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_24_saveCVBuilderEmptyData_currentCodeAllowsEmptyPayload() throws Exception {
        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_25_saveCVBuilderFullData_shouldReturnSavedCV() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
        when(cvRepository.save(any(CandidateCV.class))).thenAnswer(invocation -> {
            CandidateCV cv = invocation.getArgument(0);
            cv.setId(3L);
            return cv;
        });

        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cvPayload("Full CV Builder Test", "classic")))
                .andExpect(status().isOk());
    }

    @Test
    void tc_2_26_saveCVBuilderMissingName_currentCodeAllowsMissingTitle() throws Exception {
        cvMvc.perform(post("/api/candidate/cv-builder/save")
                        .header("Authorization", bearer(VALID_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateType\":\"modern\",\"cvDataJson\":\"{}\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc_2_27_saveCVBuilderMultipleTimes_shouldShowBothInMyCVs() throws Exception {
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(candidate));
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
    void tc_2_28_viewCVDetailNotFound_shouldReturnNotFoundStyleError() {
        when(cvRepository.findById(999999L)).thenReturn(Optional.empty());

        assertThrows(ServletException.class, () ->
                cvMvc.perform(get("/api/candidate/cv-builder/999999").header("Authorization", bearer(VALID_TOKEN)))
        );
    }
}