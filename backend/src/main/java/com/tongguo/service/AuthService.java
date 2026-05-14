package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.MerchantUser;
import com.tongguo.mapper.MerchantUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password, HttpSession session) {
        MerchantUser user = merchantUserMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        MerchantUser sessionUser = new MerchantUser();
        sessionUser.setId(user.getId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setMustChangePassword(user.getMustChangePassword());
        session.setAttribute("merchantUser", sessionUser);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("mustChangePassword", user.getMustChangePassword());
        return result;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        MerchantUser user = merchantUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(0);
        user.setUpdateTime(LocalDateTime.now());
        merchantUserMapper.updateById(user);
    }

    public Map<String, Object> getInfo(HttpSession session) {
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("mustChangePassword", user.getMustChangePassword());
        return result;
    }
}
