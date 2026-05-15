package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.AuthInfoDTO;
import com.tongguo.entity.MerchantUser;
import com.tongguo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/m/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<AuthInfoDTO> login(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            AuthInfoDTO data = authService.login(params.get("username"), params.get("password"), session);
            return Result.ok(data);
        } catch (IllegalArgumentException e) {
            return Result.error(4001, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout(session);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params, HttpSession session) {
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        try {
            authService.changePassword(user.getId(), params.get("oldPassword"), params.get("newPassword"));
            user.setMustChangePassword(0);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<AuthInfoDTO> info(HttpSession session) {
        return Result.ok(authService.getInfo(session));
    }
}
