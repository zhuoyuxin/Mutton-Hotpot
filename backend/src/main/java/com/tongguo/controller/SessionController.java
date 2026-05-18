package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.CheckoutHistoryDTO;
import com.tongguo.dto.SessionDetailDTO;
import com.tongguo.dto.request.SessionCheckoutRequest;
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
                                            @RequestBody SessionCheckoutRequest request) {
        if (request == null || request.getActualPaid() == null) {
            return Result.error("Actual paid amount is required");
        }

        try {
            Integer actualPaidFen = parseAmountFen(request.getActualPaid(), "Actual paid amount is required");
            Integer selfServiceUnitPriceFen = parseOptionalAmountFen(request.getSelfServiceUnitPrice());
            Integer tablewareUnitPriceFen = parseOptionalAmountFen(request.getTablewareUnitPrice());
            return Result.ok(sessionService.checkout(
                    id,
                    actualPaidFen,
                    request.getPhone(),
                    request.getSelfServiceCount(),
                    selfServiceUnitPriceFen,
                    request.getTablewareCount(),
                    tablewareUnitPriceFen
            ));
        } catch (NumberFormatException | ArithmeticException e) {
            return Result.error("Amount format is invalid");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer parseAmountFen(BigDecimal amountValue, String missingMessage) {
        if (amountValue == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        BigDecimal amount = amountValue.setScale(2, RoundingMode.HALF_UP);
        return amount.multiply(new BigDecimal(100)).intValueExact();
    }

    private Integer parseOptionalAmountFen(BigDecimal amountValue) {
        if (amountValue == null) {
            return null;
        }
        return parseAmountFen(amountValue, "Amount is required");
    }
}
