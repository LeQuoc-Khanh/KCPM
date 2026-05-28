package app.auth.security;

import app.admin.service.SystemSettingService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemSettingService settingService;

    public MaintenanceModeFilter(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // nếu không bật bảo trì => cho qua
        if (!settingService.isMaintenanceEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Cho phép admin settings + swagger (tuỳ bạn)
        if (path.startsWith("/api/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        // Auth login/refresh vẫn đi qua, nhưng login sẽ bị chặn ở AuthService nếu non-admin
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated() && hasRole(auth, "ADMIN");

        if (isAdmin) {
            chain.doFilter(request, response);
            return;
        }

        // Non-admin => 503
        response.setStatus(503);
        response.setContentType("application/json;charset=UTF-8");
        String msg = settingService.maintenanceMessage();
        response.getWriter().write("{\"success\":false,\"code\":\"MAINTENANCE_MODE\",\"message\":\"" + escape(msg) + "\"}");
    }

    private boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (role.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
