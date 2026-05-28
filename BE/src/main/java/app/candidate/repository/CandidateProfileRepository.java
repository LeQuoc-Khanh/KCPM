package app.candidate.repository;

import app.candidate.model.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    // Tìm hồ sơ dựa trên userId
    Optional<CandidateProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<CandidateProfile> findByUserEmail(String email);
    @Query("SELECT p FROM CandidateProfile p LEFT JOIN FETCH p.skills WHERE p.user.id = :userId")
    Optional<CandidateProfile> findByUserIdWithSkills(@Param("userId") Long userId);

}