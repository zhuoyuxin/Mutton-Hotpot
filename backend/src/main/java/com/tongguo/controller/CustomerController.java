package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.CustomerDetailDTO;
import com.tongguo.dto.CustomerInfoDTO;
import com.tongguo.dto.request.CustomerLoginRequest;
import com.tongguo.dto.request.ManualPointsRequest;
import com.tongguo.entity.Customer;
import com.tongguo.entity.Orders;
import com.tongguo.entity.PointsRecord;
import com.tongguo.service.CustomerService;
import com.tongguo.service.OrderService;
import com.tongguo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/api/c/auth/login")
    public Result<CustomerInfoDTO> customerLogin(@RequestBody CustomerLoginRequest request) {
        String phone = request.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            return Result.ok();
        }
        Customer customer = customerService.findOrCreateByPhone(phone);
        CustomerInfoDTO dto = new CustomerInfoDTO();
        dto.setId(customer.getId());
        dto.setPhone(customer.getPhone());
        dto.setName(customer.getName());
        return Result.ok(dto);
    }

    public String extractPhone(String phoneHeader) {
        if (phoneHeader == null || phoneHeader.trim().isEmpty()) {
            return null;
        }
        return phoneHeader.trim();
    }

    @GetMapping("/api/c/customer/info")
    public Result<CustomerInfoDTO> customerInfo(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) {
            return Result.error(40101, "Please login first");
        }
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        CustomerInfoDTO dto = new CustomerInfoDTO();
        dto.setId(customer.getId());
        dto.setPhone(customer.getPhone());
        dto.setName(customer.getName());
        dto.setPoints(customer.getPoints());
        dto.setTotalSpent(customer.getTotalSpent());
        return Result.ok(dto);
    }

    @GetMapping("/api/c/customer/orders")
    public Result<List<Orders>> customerOrders(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) {
            return Result.error(40101, "Please login first");
        }
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        return Result.ok(orderService.getCustomerOrders(customer.getId()));
    }

    @GetMapping("/api/c/customer/points")
    public Result<List<PointsRecord>> customerPoints(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) {
            return Result.error(40101, "Please login first");
        }
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        return Result.ok(customerService.getPointsRecords(customer.getId()));
    }

    @GetMapping("/api/m/customer/list")
    public Result<List<Customer>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.search(keyword));
    }

    @GetMapping("/api/m/customer/detail/{id}")
    public Result<CustomerDetailDTO> detail(@PathVariable Integer id) {
        Customer customer = customerService.detail(id);
        if (customer == null) {
            return Result.error("Customer does not exist");
        }
        List<PointsRecord> records = customerService.getPointsRecords(id);
        List<Orders> orders = orderService.getCustomerOrders(id);
        CustomerDetailDTO dto = new CustomerDetailDTO();
        dto.setCustomer(customer);
        dto.setPointsRecords(records);
        dto.setOrders(orders);
        dto.setConsumptionRecords(sessionService.getCustomerConsumptionRecords(id));
        return Result.ok(dto);
    }

    @PostMapping("/api/m/customer/points")
    public Result<Void> manualPoints(@RequestBody ManualPointsRequest request) {
        try {
            customerService.manualPoints(request.getCustomerId(), request.getPoints(), request.getRemark());
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
