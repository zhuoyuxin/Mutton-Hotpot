package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
            Object paidObj = params.get("actualPaid");
            int actualPaidFen = 0;
            if (paidObj != null) {
                BigDecimal bd = new BigDecimal(paidObj.toString())
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                actualPaidFen = bd.multiply(new BigDecimal(100)).intValueExact();
            }
            String phone = (String) params.get("phone");
            SessionCheckout checkout = sessionService.checkout(id, actualPaidFen, phone);
            return Result.ok(checkout);
        } catch (NumberFormatException | ArithmeticException e) {
            return Result.error("实收金额格式不正确");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
