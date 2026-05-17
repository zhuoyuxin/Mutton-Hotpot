package com.tongguo.service;

import com.tongguo.dto.CustomerAuthDTO;
import com.tongguo.dto.CustomerInfoDTO;
import com.tongguo.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class CustomerAuthService {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private WechatMiniappService wechatMiniappService;

    @Value("${wechat.miniapp.token-secret}")
    private String tokenSecret;

    @Value("${wechat.miniapp.token-expire-days:30}")
    private int tokenExpireDays;

    public CustomerAuthDTO loginWithWechatCode(String code) {
        String openId = wechatMiniappService.exchangeCodeForOpenId(code);
        Customer customer = customerService.findOrCreateByOpenId(openId);
        return buildAuthDTO(customer, "wechat_miniapp");
    }

    public Customer resolveCustomer(String customerToken, String phoneHeader) {
        Customer customer = resolveByToken(customerToken);
        if (customer != null) {
            return customer;
        }
        String phone = customerService.normalizeOptionalText(phoneHeader);
        if (phone == null) {
            return null;
        }
        return customerService.getByPhone(phone);
    }

    public Customer resolveByToken(String customerToken) {
        ParsedCustomerToken parsedToken = parseToken(customerToken);
        if (parsedToken == null) {
            return null;
        }
        Customer customer = customerService.detail(parsedToken.customerId);
        if (customer == null) {
            return null;
        }
        if (customer.getOpenid() == null || !customer.getOpenid().equals(parsedToken.openId)) {
            return null;
        }
        return customer;
    }

    public CustomerInfoDTO toCustomerInfo(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerInfoDTO dto = new CustomerInfoDTO();
        dto.setId(customer.getId());
        dto.setPhone(customer.getPhone());
        dto.setName(customer.getName());
        dto.setPoints(customer.getPoints());
        dto.setTotalSpent(customer.getTotalSpent());
        dto.setPhoneBound(customer.getPhone() != null && !customer.getPhone().trim().isEmpty());
        dto.setOpenidBound(customer.getOpenid() != null && !customer.getOpenid().trim().isEmpty());
        return dto;
    }

    private CustomerAuthDTO buildAuthDTO(Customer customer, String loginType) {
        long expiresAt = Instant.now().plusSeconds(Math.max(1, tokenExpireDays) * 24L * 60L * 60L).getEpochSecond();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = customer.getId() + ":" + customer.getOpenid() + ":" + expiresAt + ":" + nonce;
        String signature = sign(payload);
        String token = base64UrlEncode(payload) + "." + base64UrlEncode(signature);

        CustomerAuthDTO dto = new CustomerAuthDTO();
        dto.setToken(token);
        dto.setExpiresAt(expiresAt);
        dto.setLoginType(loginType);
        dto.setCustomer(toCustomerInfo(customer));
        return dto;
    }

    private ParsedCustomerToken parseToken(String customerToken) {
        String normalizedToken = customerService.normalizeOptionalText(customerToken);
        if (normalizedToken == null) {
            return null;
        }

        String[] parts = normalizedToken.split("\\.");
        if (parts.length != 2) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String signature = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            String[] payloadParts = payload.split(":", 4);
            if (payloadParts.length != 4) {
                return null;
            }

            long expiresAt = Long.parseLong(payloadParts[2]);
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return null;
            }

            ParsedCustomerToken token = new ParsedCustomerToken();
            token.customerId = Integer.valueOf(payloadParts[0]);
            token.openId = payloadParts[1];
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign customer token", e);
        }
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class ParsedCustomerToken {
        private Integer customerId;
        private String openId;
    }
}
