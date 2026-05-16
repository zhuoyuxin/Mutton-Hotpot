package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.CheckoutHistoryDTO;
import com.tongguo.dto.SessionDetailDTO;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/m/session")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/current/{tableId}")
    public Result<DiningSession> current(@PathVariable Integer tableId) {
        return Result.ok(sessionService.getCurrentByTableId(tableId));
    }

    @GetMapping("/detail/{id}")
    public Result<SessionDetailDTO> detail(@PathVariable Integer id) {
        return Result.ok(sessionService.getDetail(id));
    }

    @GetMapping("/history")
    public Result<List<CheckoutHistoryDTO>> history(@RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        return Result.ok(sessionService.getCheckoutHistory(startDate, endDate));
    }

    @PutMapping("/checkout/{id}")
    public Result<SessionCheckout> checkout(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> params) {
        if (params == null || !params.containsKey("actualPaid")) {
            return Result.error("Actual paid amount is required");
        }

        try {
            Integer actualPaidFen = parseActualPaidFen(params.get("actualPaid"));
            String phone = (String) params.get("phone");
            return Result.ok(sessionService.checkout(id, actualPaidFen, phone));
        } catch (NumberFormatException | ArithmeticException e) {
            return Result.error("Actual paid amount format is invalid");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer parseActualPaidFen(Object paidObj) {
        if (paidObj == null) {
            throw new IllegalArgumentException("Actual paid amount is required");
        }

        String raw = paidObj.toString().trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Actual paid amount is required");
        }

        BigDecimal amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        return amount.multiply(new BigDecimal(100)).intValueExact();
    }
}
