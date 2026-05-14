package com.tongguo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongguo.entity.MerchantUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("merchantUser") == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "请先登录");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        if (user != null
                && user.getMustChangePassword() != null
                && user.getMustChangePassword() == 1
                && !isPasswordChangeAllowed(request.getRequestURI())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 40301);
            result.put("message", "请先修改初始密码");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        return true;
    }

    private boolean isPasswordChangeAllowed(String uri) {
        return "/api/m/auth/info".equals(uri)
                || "/api/m/auth/password".equals(uri)
                || "/api/m/auth/logout".equals(uri);
    }
}
