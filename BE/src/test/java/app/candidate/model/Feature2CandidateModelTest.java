package app.candidate.model;

import app.ai.models.Experience;
import app.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Feature2CandidateModelTest {

    @Test
    void candidateCV_shouldSetTimestamps_onCreate() {
        CandidateCV cv = new CandidateCV();

        ReflectionTestUtils.invokeMethod(cv, "onCreate");

        assertNotNull(cv.getCreatedAt());
        assertNotNull(cv.getUpdatedAt());
    }

    @Test
    void candidateCV_shouldRefreshUpdatedAt_onUpdate() {
        CandidateCV cv = new CandidateCV();
        LocalDateTime oldValue = LocalDateTime.now().minusDays(1);
        cv.setUpdatedAt(oldValue);

        ReflectionTestUtils.invokeMethod(cv, "onUpdate");

        assertTrue(cv.getUpdatedAt().isAfter(oldValue));
    }

    @Test
    void candidateProfile_shouldKeepSkillsExperiencesAndEducationJson() {
        User user = User.builder().id(44L).email("candidate@test.com").fullName("Candidate").build();
        CandidateProfile profile = CandidateProfile.builder()
                .id(1L)
                .user(user)
                .skills(new ArrayList<>(List.of("Java")))
                .experiences(new ArrayList<>())
                .educationJson("[{\"school\":\"UT\"}]")
                .build();
        Experience experience = new Experience();
        experience.setCompany("ABC");
        experience.setCandidateProfile(profile);
        profile.getExperiences().add(experience);

        assertEquals(List.of("Java"), profile.getSkills());
        assertEquals(1, profile.getExperiences().size());
        assertSame(profile, profile.getExperiences().get(0).getCandidateProfile());
        assertEquals("[{\"school\":\"UT\"}]", profile.getEducationJson());
    }
}