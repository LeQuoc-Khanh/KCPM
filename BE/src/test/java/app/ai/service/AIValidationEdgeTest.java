package app.ai.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.extractortext.CVTextExtractor;
import app.ai.service.cv.gemini.GeminiApiClient;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.auth.repository.UserRepository;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;

@ExtendWith(MockitoExtension.class)
class AIValidationEdgeTest {

    @Mock private CVTextExtractor textExtractor;
    @Mock private GeminiService geminiService;
    @Mock private GeminiApiClient geminiApiClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private JobApplicationRepository applicationRepository;
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private CVAnalysisResultRepository analysisRepository;

    @InjectMocks private CVAnalysisService cvAnalysisService;
    @InjectMocks private JobFastMatchingService jobFastMatchingService;
    @InjectMocks private JobMatchingService jobMatchingService;

    @Test
    @DisplayName("TC_5.27 CV analysis failed when CV file is empty")
    void shouldFailWhenCVFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        when(textExtractor.extractText(file)).thenReturn("");
        when(geminiService.parseCV("")).thenThrow(new RuntimeException("AI Error: empty CV"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cvAnalysisService.analyzeCV(file));

        assertTrue(ex.getMessage().contains("empty CV"));
        verify(geminiService).parseCV("");
    }

    @Test
    @DisplayName("TC_5.28 CV analysis failed when uploaded file format is invalid")
    void shouldFailWhenCVFileFormatIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.exe", "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8));
        when(textExtractor.extractText(file)).thenThrow(new RuntimeException("Unsupported file type"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cvAnalysisService.analyzeCV(file));

        assertTrue(ex.getMessage().contains("Unsupported file type"));
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("TC_5.29 Candidate job matching failed when candidate profile is incomplete")
    void shouldReturnZeroScoreWhenCandidateSkillsAreMissing() {
        Map<Long, FastMatchResult> result = jobFastMatchingService.calculateBatchCompatibility(null, List.of(1L, 2L));

        assertEquals(0, result.get(1L).getMatchScore());
        assertTrue(result.get(1L).getMatchedSkills().isEmpty());
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    @DisplayName("TC_5.30 Recruiter AI screening failed when job does not exist")
    void shouldFailScreeningWhenJobDoesNotExist() {
        when(applicationRepository.findByJobPostingId(999L)).thenReturn(List.of(mock(app.recruitment.entity.JobApplication.class)));
        when(jobPostingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> jobMatchingService.screenApplications(999L));
    }

    @Test
    @DisplayName("TC_5.31 Batch candidate scoring failed when candidate list is empty")
    void shouldReturnZeroScoresWhenCandidateSkillListIsEmpty() {
        Map<Long, FastMatchResult> result = jobFastMatchingService.calculateBatchCompatibility(List.of(), List.of(10L));

        assertEquals(0, result.get(10L).getMatchScore());
        assertTrue(result.get(10L).getMissingSkills().isEmpty());
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    @DisplayName("TC_5.32 AI feature handles invalid request parameters correctly")
    void shouldThrowWhenApplicationIdIsInvalid() {
        when(applicationRepository.findById(-1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> jobMatchingService.getApplicationAnalysis(-1L));

        assertTrue(ex.getMessage().contains("Application not found"));
    }

    @Test
    @DisplayName("TC_5.39 Extremely long AI prompt is handled correctly")
    void shouldHandleExtremelyLongPrompt() {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        String longPrompt = "a".repeat(10_000);
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenReturn("limited response");

        String result = realGeminiService.chatWithAI(longPrompt);

        assertEquals("limited response", result);
        verify(geminiApiClient).generateContent(contains(longPrompt), eq(0.5f));
    }

    @Test
    @DisplayName("TC_5.40 Empty AI response handled correctly")
    void shouldHandleEmptyAIResponseWhenAnalyzeCV() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "CV".getBytes(StandardCharsets.UTF_8));
        when(textExtractor.extractText(file)).thenReturn("CV text");
        when(geminiService.parseCV("CV text")).thenReturn(null);

        assertNull(cvAnalysisService.analyzeCV(file));
    }

    @Test
    @DisplayName("TC_5.41 Malformed AI response handled correctly")
    void shouldReturnEmptyListWhenExtractSkillsResponseInvalid() throws Exception {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        when(geminiApiClient.generateContent(anyString(), eq(0.0f))).thenReturn("not-json");
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("invalid json"));

        List<String> result = realGeminiService.extractSkillsFromJob("Backend", "Java");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC_5.42 CV analysis failed when uploaded file is too large")
    void shouldFailWhenCVFileIsTooLarge() {
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[10_000]);
        when(textExtractor.extractText(file)).thenThrow(new RuntimeException("File too large"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cvAnalysisService.analyzeCV(file));

        assertTrue(ex.getMessage().contains("File too large"));
    }

    @Test
    @DisplayName("TC_5.43 Career chat failed when question is empty")
    void shouldHandleEmptyCareerChatQuestion() {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenReturn("Vui lòng nhập câu hỏi cụ thể hơn.");

        String result = realGeminiService.chatWithAI("");

        assertTrue(result.contains("câu hỏi"));
    }

    @Test
    @DisplayName("TC_5.44 Gemini API response parsing failed safely")
    void shouldThrowRuntimeExceptionWhenGeminiParsingFails() throws Exception {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        when(geminiApiClient.generateContent(anyString(), eq(0.2f))).thenReturn("invalid-json");
        when(objectMapper.readValue(anyString(), eq(MatchResult.class))).thenThrow(new RuntimeException("parse error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> realGeminiService.matchCVWithJob("CV", "JD", "REQ"));

        assertTrue(ex.getMessage().contains("AI Error"));
    }

    @Test
    @DisplayName("TC_5.45 AI feature handles missing external service configuration")
    void shouldHandleMissingExternalServiceConfiguration() {
        GeminiService realGeminiService = new GeminiService(objectMapper, geminiApiClient);
        when(geminiApiClient.generateContent(anyString(), eq(0.5f)))
                .thenThrow(new RuntimeException("missing API key"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> realGeminiService.callAiChat("hello"));

        assertTrue(ex.getMessage().contains("missing API key"));
    }
}
