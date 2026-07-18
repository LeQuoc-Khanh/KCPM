package app.ai.service.cv.gemini;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.ai.service.cv.gemini.dto.GeminiResponse;
import app.ai.service.cv.gemini.dto.MatchResult;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GeminiApiClient geminiApiClient;

    @InjectMocks
    private GeminiService geminiService;

    @Test
    @DisplayName("TC_5.10 AI generates CV JSON successfully")
    void shouldParseCVSuccessfully() throws Exception {
        GeminiResponse expected = new GeminiResponse();
        expected.setAboutMe("Backend developer");
        when(geminiApiClient.generateContent(anyString(), eq(0.0f))).thenReturn("{\"aboutMe\":\"Backend developer\"}");
        when(objectMapper.readValue(anyString(), eq(GeminiResponse.class))).thenReturn(expected);

        GeminiResponse result = geminiService.parseCV("CV raw text");

        assertEquals("Backend developer", result.getAboutMe());
    }

    @Test
    @DisplayName("TC_5.11 AI evaluates candidate answer successfully")
    void shouldCallAIChatForInterviewEvaluation() {
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenReturn("AI reply");

        String result = geminiService.callAiChat("Evaluate this answer");

        assertEquals("AI reply", result);
    }

    @Test
    @DisplayName("TC_5.15 Career chat response generated successfully")
    void shouldGenerateCareerChatResponseSuccessfully() {
        when(geminiApiClient.generateContent(contains("CÂU HỎI CỦA NGƯỜI DÙNG"), eq(0.5f))).thenReturn("Bạn nên học Java và Spring Boot.");

        String result = geminiService.chatWithAI("Tôi nên học gì để làm backend?");

        assertTrue(result.contains("Java"));
    }

    @Test
    @DisplayName("TC_5.17 AI recommendation returns matching score successfully")
    void shouldReturnMatchingScoreSuccessfully() throws Exception {
        MatchResult expected = MatchResult.builder().matchPercentage(85).build();
        when(geminiApiClient.generateContent(anyString(), eq(0.2f))).thenReturn("{\"matchPercentage\":85}");
        when(objectMapper.readValue(anyString(), eq(MatchResult.class))).thenReturn(expected);

        MatchResult result = geminiService.matchCVWithJob("Java CV", "Backend JD", "Java, Spring");

        assertEquals(85, result.getMatchPercentage());
    }

    @Test
    @DisplayName("TC_5.18 AI recommendation includes matching reasons")
    void shouldReturnMatchingReasonsSuccessfully() throws Exception {
        MatchResult expected = MatchResult.builder()
                .matchPercentage(80)
                .evaluation("Ứng viên phù hợp vì có Java và Spring Boot")
                .matchedSkillsList(List.of("Java", "Spring Boot"))
                .missingSkillsList(List.of("Docker"))
                .build();
        when(geminiApiClient.generateContent(anyString(), eq(0.2f))).thenReturn("json");
        when(objectMapper.readValue(anyString(), eq(MatchResult.class))).thenReturn(expected);

        MatchResult result = geminiService.matchCVWithJob("CV", "JD", "REQ");

        assertTrue(result.getEvaluation().contains("phù hợp"));
        assertEquals(List.of("Docker"), result.getMissingSkillsList());
    }

    @Test
    @DisplayName("TC_5.19 AI feature response contains required output fields")
    void shouldContainRequiredOutputFields() throws Exception {
        MatchResult expected = MatchResult.builder()
                .matchPercentage(90)
                .matchedSkillsCount(2)
                .missingSkillsCount(1)
                .evaluation("Good match")
                .learningPath("Learn Docker")
                .careerAdvice("Apply now")
                .build();
        when(geminiApiClient.generateContent(anyString(), eq(0.2f))).thenReturn("json");
        when(objectMapper.readValue(anyString(), eq(MatchResult.class))).thenReturn(expected);

        MatchResult result = geminiService.matchCVWithJob("CV", "JD", "REQ");

        assertAll(
                () -> assertEquals(90, result.getMatchPercentage()),
                () -> assertNotNull(result.getEvaluation()),
                () -> assertNotNull(result.getLearningPath()),
                () -> assertNotNull(result.getCareerAdvice())
        );
    }

    @Test
    @DisplayName("TC_5.27 AI service timeout handled correctly")
    void shouldThrowRuntimeExceptionWhenAIServiceTimeout() {
        when(geminiApiClient.generateContent(anyString(), eq(0.2f))).thenThrow(new RuntimeException("timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> geminiService.matchCVWithJob("CV", "JD", "REQ"));

        assertTrue(ex.getMessage().contains("AI Error"));
    }

    @Test
    @DisplayName("TC_5.28 AI provider returned internal server error")
    void shouldThrowRuntimeExceptionWhenAIProviderReturnsServerError() {
        when(geminiApiClient.generateContent(anyString(), eq(0.0f))).thenThrow(new RuntimeException("500 Internal Server Error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> geminiService.parseCV("CV text"));

        assertTrue(ex.getMessage().contains("AI Error"));
    }

    @Test
    @DisplayName("TC_5.29 AI provider rate limit exceeded")
    void shouldThrowRuntimeExceptionWhenRateLimitExceeded() {
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenThrow(new RuntimeException("429 rate limit"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> geminiService.chatWithAI("hello"));

        assertTrue(ex.getMessage().contains("429") || ex.getMessage().contains("rate limit"));
    }

    @Test
    @DisplayName("TC_5.30 AI provider unavailable")
    void shouldThrowRuntimeExceptionWhenProviderUnavailable() {
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenThrow(new RuntimeException("service unavailable"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> geminiService.callAiChat("prompt"));

        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    @DisplayName("TC_5.31 Extract skills from job returns empty list when AI response is invalid")
    void shouldReturnEmptyListWhenExtractSkillsResponseInvalid() throws Exception {
        when(geminiApiClient.generateContent(anyString(), eq(0.0f))).thenReturn("not-json");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("invalid json"));

        List<String> result = geminiService.extractSkillsFromJob("Backend", "Java");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC_5.34 Extremely long AI prompt is sent through controlled chat method")
    void shouldHandleExtremelyLongPrompt() {
        String longPrompt = "a".repeat(10_000);
        when(geminiApiClient.generateContent(anyString(), eq(0.5f))).thenReturn("limited response");

        String result = geminiService.chatWithAI(longPrompt);

        assertEquals("limited response", result);
        verify(geminiApiClient).generateContent(contains(longPrompt), eq(0.5f));
    }

    @Test
    @DisplayName("TC_5.40 Retry mechanism works after temporary AI service failure")
    void shouldReturnSuccessfulResponseAfterTemporaryFailureWhenCallerRetries() {
        when(geminiApiClient.generateContent(anyString(), eq(0.5f)))
                .thenThrow(new RuntimeException("temporary error"))
                .thenReturn("success after retry");

        assertThrows(RuntimeException.class, () -> geminiService.callAiChat("prompt"));
        String result = geminiService.callAiChat("prompt");

        assertEquals("success after retry", result);
        verify(geminiApiClient, times(2)).generateContent(anyString(), eq(0.5f));
    }
}
