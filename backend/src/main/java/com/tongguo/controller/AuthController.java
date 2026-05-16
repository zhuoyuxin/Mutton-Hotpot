package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.AuthInfoDTO;
import com.tongguo.dto.request.AuthLoginRequest;
import com.tongguo.dto.request.ChangePasswordRequest;
import com.tongguo.entity.MerchantUser;
import com.tongguo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/m/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<AuthInfoDTO> login(@RequestBody AuthLoginRequest request, HttpSession session) {
        try {
            AuthInfoDTO data = authService.login(request.getUsername(), request.getPassword(), session);
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
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request, HttpSession session) {
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        try {
            authService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
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
