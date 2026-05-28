package app.candidate.repository;

import app.candidate.model.CandidateCV;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CandidateCVRepository extends JpaRepository<CandidateCV, Long> {
    List<CandidateCV> findByUserId(Long userId);
}