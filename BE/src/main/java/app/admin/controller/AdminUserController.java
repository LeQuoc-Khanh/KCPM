package app.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.dto.request.UpdateUserRoleRequest;
import app.admin.dto.response.AdminUserResponse;
import app.admin.dto.response.CreateAdminUserResponse;
import app.admin.service.AdminUserService;
import app.auth.model.enums.UserRole;
import jakarta.validation.Valid;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<AdminUserResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            Pageable pageable
    ) {
        return adminUserService.getAllUsers(keyword, role, pageable);
    }

    @PutMapping("/{id}/lock")
    public void lockUser(@PathVariable Long id) {
        adminUserService.lockUser(id);
    }

    @PutMapping("/{id}/unlock")
    public void unlockUser(@PathVariable Long id) {
        adminUserService.unlockUser(id);
    }

    @PostMapping
    public CreateAdminUserResponse createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        System.out.println("✅ HIT POST /api/admin/users");
        return adminUserService.createUser(request);
    }

    @Bean
    CommandLineRunner printMappings(RequestMappingHandlerMapping mapping) {
        return args -> mapping.getHandlerMethods().forEach((k, v) -> {
            if (k.toString().contains("/api/admin/users")) {
                System.out.println("MAPPING: " + k);
            }
        });
    }

    @PutMapping("/{id}/role")
    public AdminUserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return adminUserService.updateUserRole(id, request);
    }
}
