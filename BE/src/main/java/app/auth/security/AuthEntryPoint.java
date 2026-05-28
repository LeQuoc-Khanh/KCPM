package app.auth.security;

import app.auth.dto.response.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.error("Unauthorized error: {}", authException.getMessage());
        response.setContentType("application/json;charset=UTF-8");

        // ✅ Nếu bị khóa (BANNED)
        if (authException instanceof DisabledException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403

            // Nếu bạn muốn trả dạng custom code cho FE bắt:
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "code", "ACCOUNT_BANNED",
                    "message", authException.getMessage()
            )));
            return;
        }

        // ✅ Mặc định: chưa đăng nhập / token sai / token hết hạn...
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

        // Bạn có thể dùng MessageResponse nếu muốn format đồng nhất:
        MessageResponse errorResponse = MessageResponse.error(
                "Unauthorized: " + authException.getMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
