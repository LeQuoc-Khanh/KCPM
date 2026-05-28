package app.recruitment.service;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import app.candidate.dto.response.CandidateProfileResponse; // Import DTO mới
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LƯU Ý:
 * - Hiện tại `User` trong workspace không có fields skills hoặc gpa. 
 * Vì vậy implement hiện tại hỗ trợ tìm theo tên + role=CANDIDATE.
 * - Để search theo skills/gpa, cần mở rộng schema hoặc sử dụng CVAnalysisService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateSearchServiceImpl implements CandidateSearchService {

    private final UserRepository userRepository;

    @Override
    public List<User> searchCandidates(String skill, Double minGpa, String name) {
        // filter role = CANDIDATE
        List<User> candidates;
        
        if (name != null && !name.isBlank()) {
            // Logic cũ: tìm placeholder rồi filter stream
            candidates = userRepository.findByStatus(null) 
                    .stream()
                    .filter(u -> u.getUserRole() == UserRole.CANDIDATE)
                    .filter(u -> u.getFullName() != null && u.getFullName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            // Logic cũ: tìm tất cả rồi filter role
            candidates = userRepository.findAll().stream()
                    .filter(u -> u.getUserRole() == UserRole.CANDIDATE)
                    .collect(Collectors.toList());
        }

        // Skill / GPA filtering: chưa có trường lưu trữ -> log warning như code cũ
        if (skill != null && !skill.isBlank()) {
            log.warn("Skill filter requested ('{}') but User.skills not present in schema. Returning name-filtered results only.", skill);
        }
        if (minGpa != null) {
            log.warn("GPA filter requested ({}) but User.gpa not present in schema. Ignored.", minGpa);
        }
        return candidates;
    }

    @Override
    public List<CandidateProfileResponse> searchByJobDescription(String jobDescription) {
        // Tạm thời trả về list rỗng để fix lỗi biên dịch
        // Sau này bạn sẽ gọi AI Service ở đây để match JD với Candidate
        return Collections.emptyList();
    }
}