package org.verytogether.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.verytogether.userservice.model.User;
import org.verytogether.userservice.model.UserRole;
import org.verytogether.userservice.service.UserService;

@Component
public class RBACInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public RBACInterceptor(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractTokenFromRequest(request);

        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未提供访问令牌");
            return false;
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User currentUser = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            request.setAttribute("currentUser", currentUser);

            String path = request.getRequestURI();
            String method = request.getMethod();

            if (!hasPermission(currentUser, path, method)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                return false;
            }

            return true;
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无效的访问令牌");
            return false;
        }
    }

    private boolean hasPermission(User user, String path, String method) {
        UserRole role = user.getRole();

        if (role == UserRole.SUPER_ADMIN) {
            return true;
        }

        if (path.startsWith("/api/users")) {
            if (method.equals("GET")) {
                if (path.equals("/api/users")) {
                    return role == UserRole.SUPER_ADMIN;
                } else if (path.matches("/api/users/\\d+")) {
                    Long userId = extractUserIdFromPath(path);
                    return userId.equals(user.getId()) || role == UserRole.SUPER_ADMIN;
                }
            } else if (method.equals("PUT")) {
                Long userId = extractUserIdFromPath(path);
                return userId.equals(user.getId()) || role == UserRole.SUPER_ADMIN;
            } else if (method.equals("POST") || method.equals("DELETE")) {
                return role == UserRole.SUPER_ADMIN;
            }
        }

        return true;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Long extractUserIdFromPath(String path) {
        try {
            String[] parts = path.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }
}