package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.OrderItem;
import com.tongguo.entity.Orders;
import com.tongguo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/api/c/order/create")
    public Result<Orders> customerCreate(@RequestBody Map<String, Object> params,
                                         @RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        try {
            String phoneFromHeader = extractPhone(phoneHeader);
            String phoneFromBody = (String) params.get("phone");
            String phone = phoneFromHeader != null ? phoneFromHeader : phoneFromBody;
            Orders order = orderService.createOrder(
                    toInt(params.get("tableId")),
                    null,
                    (List<Map<String, Object>>) params.get("items"),
                    phone,
                    (String) params.get("remark")
            );
            return Result.ok(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/c/order/table/{tableId}")
    public Result<List<Orders>> tableOrders(@PathVariable Integer tableId) {
        return Result.ok(orderService.getTableCurrentOrders(tableId));
    }

    @GetMapping("/api/m/order/list")
    public Result<List<Orders>> list(@RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) Integer tableId,
                                     @RequestParam(required = false) List<Integer> statuses,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        return Result.ok(orderService.listOrders(status, tableId, statuses, startDate, endDate));
    }

    @PostMapping("/api/m/order/create")
    public Result<Orders> merchantCreate(@RequestBody Map<String, Object> params) {
        try {
            Orders order = orderService.createOrder(
                    toInt(params.get("tableId")),
                    toInt(params.get("sessionId")),
                    (List<Map<String, Object>>) params.get("items"),
                    (String) params.get("phone"),
                    (String) params.get("remark")
            );
            return Result.ok(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/m/order/items/{orderId}")
    public Result<List<OrderItem>> items(@PathVariable Integer orderId) {
        return Result.ok(orderService.getOrderItems(orderId));
    }

    @PutMapping("/api/m/order/confirm/{id}")
    public Result<Orders> confirm(@PathVariable Integer id) {
        try {
            return Result.ok(orderService.confirmOrder(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/order/serve-item/{id}")
    public Result<OrderItem> serveItem(@PathVariable Integer id) {
        try {
            return Result.ok(orderService.serveItem(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/order/cancel-item/{id}")
    public Result<OrderItem> cancelItem(@PathVariable Integer id) {
        try {
            return Result.ok(orderService.cancelItem(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/order/cancel/{id}")
    public Result<Orders> cancel(@PathVariable Integer id) {
        try {
            return Result.ok(orderService.cancelOrder(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        return ((Number) obj).intValue();
    }

    private String extractPhone(String phoneHeader) {
        if (phoneHeader == null || phoneHeader.trim().isEmpty()) {
            return null;
        }
        return phoneHeader.trim();
    }
}
