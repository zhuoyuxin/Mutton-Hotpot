package com.tongguo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WechatMiniappService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wechat.miniapp.app-id:}")
    private String appId;

    @Value("${wechat.miniapp.app-secret:}")
    private String appSecret;

    public String exchangeCodeForOpenId(String code) {
        String normalizedCode = normalize(code);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("WeChat login code is required");
        }
        if (normalize(appId) == null || normalize(appSecret) == null) {
            throw new IllegalStateException("WeChat miniapp credentials are not configured");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", normalizedCode)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.hasNonNull("errcode") && root.path("errcode").asInt() != 0) {
                throw new IllegalArgumentException("WeChat login failed: " + root.path("errmsg").asText("unknown error"));
            }
            String openId = normalize(root.path("openid").asText(null));
            if (openId == null) {
                throw new IllegalArgumentException("WeChat login failed: openid is missing");
            }
            return openId;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call WeChat login service", e);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
