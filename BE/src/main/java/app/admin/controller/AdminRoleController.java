package app.admin.controller;

import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final UserRepository userRepository;

    // GET /api/admin/roles/count-users
    @GetMapping("/count-users")
    public Map<String, Long> countUsersByRole() {
        Map<String, Long> res = new HashMap<>();

        for (Object[] row : userRepository.countUsersByRole()) {
            UserRole role = (UserRole) row[0];
            Long count = (Long) row[1];
            res.put(role.name(), count);
        }

        // đảm bảo luôn có đủ 5 role key
        for (UserRole r : UserRole.values()) {
            res.putIfAbsent(r.name(), 0L);
        }
        return res;
    }
}
