package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@RestController
@RequestMapping("/api/m/session")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/current/{tableId}")
    public Result<DiningSession> current(@PathVariable Integer tableId) {
        DiningSession session = sessionService.getCurrentByTableId(tableId);
        return Result.ok(session);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        try {
            return Result.ok(sessionService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/checkout/{id}")
    public Result<SessionCheckout> checkout(@PathVariable Integer id,
                                             @RequestBody Map<String, Object> params) {
        try {
            if (params == null || !params.containsKey("actualPaid")) {
                return Result.error("实收金额不能为空");
            }
            Integer actualPaidFen = parseActualPaidFen(params.get("actualPaid"));
            String phone = (String) params.get("phone");
            SessionCheckout checkout = sessionService.checkout(id, actualPaidFen, phone);
            return Result.ok(checkout);
        } catch (NumberFormatException | ArithmeticException e) {
            return Result.error("实收金额格式不正确");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer parseActualPaidFen(Object paidObj) {
        if (paidObj == null) {
            throw new IllegalArgumentException("实收金额不能为空");
        }
        String raw = paidObj.toString().trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("实收金额不能为空");
        }
        BigDecimal bd = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        return bd.multiply(new BigDecimal(100)).intValueExact();
    }
}
