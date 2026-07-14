package app.ai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.ai.dto.InterviewChatRequest;
import app.ai.models.InterviewSession;
import app.ai.repository.IInterviewSessionRepository;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.prompt.InterviewPromptBuilder;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.JobPostingRepository;

@ExtendWith(MockitoExtension.class)
class AIInterviewCoreTest {

    @Mock private IInterviewSessionRepository sessionRepository;
    @Mock private JobPostingRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private GeminiService geminiService;
    @Mock private InterviewPromptBuilder promptBuilder;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InterviewService interviewService;

    @Test
    @DisplayName("TC_5.8 AI interview session starts successfully")
    void shouldStartInterviewSessionSuccessfully() {
        User user = mock(User.class);
        JobPosting job = mock(JobPosting.class);
        InterviewSession saved = InterviewSession.builder().id(1L).user(user).jobPosting(job).status("ONGOING").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(sessionRepository.save(any(InterviewSession.class))).thenReturn(saved);

        InterviewSession result = interviewService.startInterview(10L, 20L);

        assertEquals("ONGOING", result.getStatus());
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("TC_5.9 AI interview question generated successfully")
    void shouldGenerateInterviewQuestionSuccessfully() {
        JobPosting job = mock(JobPosting.class);
        InterviewSession session = InterviewSession.builder().id(1L).jobPosting(job).status("ONGOING").build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(promptBuilder.buildChatPrompt(eq(job), anyList())).thenReturn("chat prompt");
        when(geminiService.callAiChat("chat prompt")).thenReturn("Please explain OOP concepts.");

        String result = interviewService.chat(1L, "Start", List.of());

        assertTrue(result.contains("OOP"));
    }

    @Test
    @DisplayName("TC_5.10 AI interview answer evaluation success")
    void shouldEvaluateInterviewChatSuccessfully() {
        JobPosting job = mock(JobPosting.class);
        InterviewSession session = InterviewSession.builder().id(1L).jobPosting(job).status("ONGOING").build();
        InterviewChatRequest.MessageItem oldMsg = new InterviewChatRequest.MessageItem();
        oldMsg.setSender("USER");
        oldMsg.setContent("Hello");
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(promptBuilder.buildChatPrompt(eq(job), anyList())).thenReturn("chat prompt");
        when(geminiService.callAiChat("chat prompt")).thenReturn("Next question");

        String result = interviewService.chat(1L, "I know Java", List.of(oldMsg));

        assertEquals("Next question", result);
    }

    @Test
    @DisplayName("TC_5.11 AI interview history retrieved successfully")
    void shouldGetCompletedInterviewHistorySuccessfully() {
        InterviewSession session = InterviewSession.builder().id(1L).status("COMPLETED").build();
        when(sessionRepository.findByUserIdAndJobPostingIdAndStatusOrderByCreatedAtDesc(10L, 20L, "COMPLETED"))
                .thenReturn(List.of(session));

        List<InterviewSession> result = interviewService.getCompletedHistory(20L, 10L);

        assertEquals(1, result.size());
        assertEquals("COMPLETED", result.get(0).getStatus());
    }

    @Test
    @DisplayName("TC_5.12 AI interview session ended successfully")
    void shouldEndInterviewSessionSuccessfully() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        InterviewSession session = InterviewSession.builder().id(1L).user(user).status("ONGOING").build();
        InterviewChatRequest.MessageItem msg = new InterviewChatRequest.MessageItem();
        msg.setSender("USER");
        msg.setContent("I have 2 years Java experience");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(promptBuilder.buildGradingPrompt(anyList())).thenReturn("grading prompt");
        when(geminiService.callAiChat("grading prompt")).thenReturn("{\"score\":88,\"feedback\":\"Good\"}");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of("score", 88, "feedback", "Good"));
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> inv.getArgument(0));

        InterviewSession result = interviewService.endInterview(1L, List.of(msg));

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(88, result.getFinalScore());
        assertEquals("Good", result.getFeedback());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("TC_5.15 Career chat response generated successfully")
    void shouldGenerateCareerChatResponseSuccessfully() {
        when(geminiService.chatWithAI("Tôi nên học gì để làm backend?")).thenReturn("Bạn nên học Java và Spring Boot.");

        String result = geminiService.chatWithAI("Tôi nên học gì để làm backend?");

        assertTrue(result.contains("Java"));
    }

    @Test
    @DisplayName("TC_5.21 AI interview greeting generated successfully")
    void shouldGenerateInterviewGreetingSuccessfully() {
        JobPosting job = mock(JobPosting.class);
        CandidateProfile profile = mock(CandidateProfile.class);
        when(job.getTitle()).thenReturn("Backend Developer");
        when(job.getCompany()).thenReturn(null);
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(profile.getFullName()).thenReturn("Nguyen Van A");
        when(promptBuilder.buildStartPrompt("Công ty", "Backend Developer", "Nguyen Van A")).thenReturn("start prompt");
        when(geminiService.callAiChat("start prompt")).thenReturn("Xin chào Nguyen Van A");

        String result = interviewService.getInitialGreeting(10L, 20L);

        assertTrue(result.contains("Nguyen Van A"));
    }
}
