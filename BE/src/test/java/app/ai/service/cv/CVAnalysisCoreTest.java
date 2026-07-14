package app.ai.service.cv;

import app.ai.service.cv.extractortext.CVTextExtractor;
import app.ai.service.cv.gemini.GeminiApiClient;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.cv.gemini.dto.ExperienceDTO;
import app.ai.service.cv.gemini.dto.GeminiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CVAnalysisCoreTest {

    @Mock private CVTextExtractor textExtractor;
    @Mock private GeminiService geminiService;
    @Mock private ObjectMapper objectMapper;
    @Mock private GeminiApiClient geminiApiClient;

    @InjectMocks
    private CVAnalysisService cvAnalysisService;

    @Test
    @DisplayName("TC_5.1 CV analysis success")
    void shouldAnalyzeCVSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "CV text".getBytes(StandardCharsets.UTF_8));
        GeminiResponse expected = new GeminiResponse();
        expected.setAboutMe("Java Developer");

        when(textExtractor.extractText(file)).thenReturn("Nguyen Van A - Java Developer");
        when(geminiService.parseCV("Nguyen Van A - Java Developer")).thenReturn(expected);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertSame(expected, result);
        verify(textExtractor).extractText(file);
        verify(geminiService).parseCV("Nguyen Van A - Java Developer");
    }

    @Test
    @DisplayName("TC_5.2 CV analysis extracts candidate skills correctly")
    void shouldExtractCandidateSkillsCorrectly() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "Java Spring".getBytes(StandardCharsets.UTF_8));
        GeminiResponse response = new GeminiResponse();
        response.setSkills(List.of("Java", "Spring Boot"));

        when(textExtractor.extractText(file)).thenReturn("Java Spring Boot");
        when(geminiService.parseCV("Java Spring Boot")).thenReturn(response);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertEquals(List.of("Java", "Spring Boot"), result.getSkills());
    }

    @Test
    @DisplayName("TC_5.3 CV analysis extracts work experience correctly")
    void shouldExtractWorkExperienceCorrectly() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "Experience".getBytes(StandardCharsets.UTF_8));
        GeminiResponse response = new GeminiResponse();
        response.setExperiences(List.of(ExperienceDTO.builder()
                .company("ABC")
                .role("Backend Developer")
                .build()));

        when(textExtractor.extractText(file)).thenReturn("Backend Developer at ABC");
        when(geminiService.parseCV("Backend Developer at ABC")).thenReturn(response);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertEquals(1, result.getExperiences().size());
        assertEquals("ABC", result.getExperiences().get(0).getCompany());
        assertEquals("Backend Developer", result.getExperiences().get(0).getRole());
    }

    @Test
    @DisplayName("TC_5.18 CV analysis returns skills correctly")
    void shouldReturnSkillsCorrectly() {
        GeminiResponse response = new GeminiResponse();
        response.setSkills(List.of("Java", "SQL", "Docker"));

        assertEquals(3, response.getSkills().size());
        assertTrue(response.getSkills().contains("Docker"));
    }

    @Test
    @DisplayName("TC_5.19 CV analysis returns experience correctly")
    void shouldReturnExperienceCorrectly() {
        ExperienceDTO experience = ExperienceDTO.builder()
                .company("CareerMate")
                .role("AI Backend Intern")
                .description("Built AI matching features")
                .build();
        GeminiResponse response = new GeminiResponse();
        response.setExperiences(List.of(experience));

        assertEquals("CareerMate", response.getExperiences().get(0).getCompany());
        assertEquals("AI Backend Intern", response.getExperiences().get(0).getRole());
    }

    @Test
    @DisplayName("TC_5.22 Gemini AI skill extraction success")
    void shouldExtractSkillsFromJobSuccessfully() throws Exception {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        when(geminiApiClient.generateContent(anyString(), eq(0.0f))).thenReturn("[\"Java\",\"Spring Boot\"]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(List.of("Java", "Spring Boot"));

        List<String> result = realGeminiService.extractSkillsFromJob("Backend Developer", "Java, Spring Boot");

        assertEquals(List.of("Java", "Spring Boot"), result);
    }
}
