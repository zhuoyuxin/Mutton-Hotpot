package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.entity.Orders;
import com.tongguo.service.CustomerService;
import com.tongguo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/api/c/auth/login")
    public Result<Map<String, Object>> customerLogin(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            return Result.ok();
        }
        Customer customer = customerService.findOrCreateByPhone(phone);
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("phone", customer.getPhone());
        data.put("name", customer.getName());
        return Result.ok(data);
    }

    private String extractPhone(String phoneHeader) {
        if (phoneHeader == null || phoneHeader.trim().isEmpty()) return null;
        return phoneHeader.trim();
    }

    @GetMapping("/api/c/customer/info")
    public Result<Map<String, Object>> customerInfo(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) {
            return Result.error(40101, "请先登录");
        }
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) {
            return Result.error(40101, "请先登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("phone", customer.getPhone());
        data.put("name", customer.getName());
        data.put("points", customer.getPoints());
        data.put("totalSpent", customer.getTotalSpent());
        return Result.ok(data);
    }

    @GetMapping("/api/c/customer/orders")
    public Result<?> customerOrders(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) return Result.error(40101, "请先登录");
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) return Result.error(40101, "请先登录");
        return Result.ok(orderService.getCustomerOrders(customer.getId()));
    }

    @GetMapping("/api/c/customer/points")
    public Result<List<PointsRecord>> customerPoints(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) return Result.error(40101, "请先登录");
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) return Result.error(40101, "请先登录");
        return Result.ok(customerService.getPointsRecords(customer.getId()));
    }

    @GetMapping("/api/m/customer/list")
    public Result<List<Customer>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.search(keyword));
    }

    @GetMapping("/api/m/customer/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Customer customer = customerService.detail(id);
        if (customer == null) return Result.error("客户不存在");
        List<PointsRecord> records = customerService.getPointsRecords(id);
        List<Orders> orders = orderService.getCustomerOrders(id);
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        data.put("pointsRecords", records);
        data.put("orders", orders);
        return Result.ok(data);
    }

    @PostMapping("/api/m/customer/points")
    public Result<Void> manualPoints(@RequestBody Map<String, Object> params) {
        try {
            customerService.manualPoints(
                    toInteger(params.get("customerId"), "客户ID"),
                    toInteger(params.get("points"), "积分"),
                    (String) params.get("remark")
            );
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Integer toInteger(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式不正确");
        }
    }
}
