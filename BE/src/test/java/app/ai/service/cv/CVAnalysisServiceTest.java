package app.ai.service.cv;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import app.ai.service.cv.extractortext.CVTextExtractor;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.cv.gemini.dto.GeminiResponse;

@ExtendWith(MockitoExtension.class)
class CVAnalysisServiceTest {

    @Mock
    private CVTextExtractor textExtractor;

    @Mock
    private GeminiService geminiService;

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
    @DisplayName("TC_5.2 CV analysis failed when CV file is empty")
    void shouldFailWhenCVFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        when(textExtractor.extractText(file)).thenReturn("");
        when(geminiService.parseCV("")).thenThrow(new RuntimeException("AI Error: empty CV"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cvAnalysisService.analyzeCV(file));

        assertTrue(ex.getMessage().contains("empty CV"));
        verify(geminiService).parseCV("");
    }

    @Test
    @DisplayName("TC_5.3 CV analysis failed when uploaded file format is invalid")
    void shouldFailWhenCVFileFormatIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.exe", "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8));
        when(textExtractor.extractText(file)).thenThrow(new RuntimeException("Unsupported file type"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cvAnalysisService.analyzeCV(file));

        assertTrue(ex.getMessage().contains("Unsupported file type"));
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("TC_5.12 CV analysis returns skills correctly")
    void shouldReturnSkillsFromCVAnalysis() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "Java Spring".getBytes(StandardCharsets.UTF_8));
        GeminiResponse response = new GeminiResponse();
        response.setSkills(java.util.List.of("Java", "Spring Boot"));

        when(textExtractor.extractText(file)).thenReturn("Java Spring Boot");
        when(geminiService.parseCV("Java Spring Boot")).thenReturn(response);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertEquals(java.util.List.of("Java", "Spring Boot"), result.getSkills());
    }

    @Test
    @DisplayName("TC_5.13 CV analysis returns experience correctly")
    void shouldReturnExperienceFromCVAnalysis() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "Experience".getBytes(StandardCharsets.UTF_8));
        GeminiResponse response = new GeminiResponse();
        response.setExperiences(java.util.List.of(app.ai.service.cv.gemini.dto.ExperienceDTO.builder()
                .company("ABC")
                .role("Backend Developer")
                .build()));

        when(textExtractor.extractText(file)).thenReturn("Backend Developer at ABC");
        when(geminiService.parseCV("Backend Developer at ABC")).thenReturn(response);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertEquals(1, result.getExperiences().size());
        assertEquals("ABC", result.getExperiences().get(0).getCompany());
    }

    @Test
    @DisplayName("TC_5.35 Empty AI response handled correctly")
    void shouldHandleEmptyAIResponseWhenAnalyzeCV() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "CV".getBytes(StandardCharsets.UTF_8));
        when(textExtractor.extractText(file)).thenReturn("CV text");
        when(geminiService.parseCV("CV text")).thenReturn(null);

        GeminiResponse result = cvAnalysisService.analyzeCV(file);

        assertNull(result);
    }

    @Test
    @DisplayName("TC_5.36 CV URL extraction returns empty text when download fails")
    void shouldReturnEmptyTextWhenUrlExtractionFails() throws Exception {
        String result = cvAnalysisService.getTextFromUrl("http://invalid.localhost/not-found.pdf");

        assertEquals("", result);
        verify(textExtractor, never()).extractText(any(File.class));
    }
}
