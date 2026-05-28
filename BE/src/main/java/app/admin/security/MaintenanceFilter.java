package app.admin.security;

import app.admin.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private final SystemSettingService settingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!settingService.isMaintenanceEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // luôn cho auth đi qua (để login/refresh)
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a ->
                    "ADMIN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority())
                );

        // ✅ admin đi qua tất cả API
        if (isAdmin) {
            chain.doFilter(request, response);
            return;
        }

        // (optional) cho admin settings đi qua kể cả chưa auth? thường không cần
        // if (path.startsWith("/api/admin/")) { chain.doFilter(...); return; }

        // ❌ non-admin bị chặn
        response.setStatus(503);
        response.setContentType("application/json;charset=UTF-8");
        String msg = settingService.maintenanceMessage();
        response.getWriter().write("{\"success\":false,\"code\":\"MAINTENANCE_MODE\",\"message\":\"" + escape(msg) + "\"}");
    }
    private String escape(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
