package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.CustomerDetailDTO;
import com.tongguo.dto.CustomerAuthDTO;
import com.tongguo.dto.CustomerInfoDTO;
import com.tongguo.dto.request.BindCustomerPhoneRequest;
import com.tongguo.dto.request.CustomerLoginRequest;
import com.tongguo.dto.request.ManualPointsRequest;
import com.tongguo.dto.request.WechatLoginRequest;
import com.tongguo.entity.Customer;
import com.tongguo.entity.Orders;
import com.tongguo.entity.PointsRecord;
import com.tongguo.service.CustomerAuthService;
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
    private CustomerAuthService customerAuthService;

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
        return Result.ok(customerAuthService.toCustomerInfo(customer));
    }

    @PostMapping("/api/c/auth/wechat-login")
    public Result<CustomerAuthDTO> wechatLogin(@RequestBody WechatLoginRequest request) {
        try {
            return Result.ok(customerAuthService.loginWithWechatCode(request == null ? null : request.getCode()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    public String extractPhone(String phoneHeader) {
        if (phoneHeader == null || phoneHeader.trim().isEmpty()) {
            return null;
        }
        return phoneHeader.trim();
    }

    @GetMapping("/api/c/customer/info")
    public Result<CustomerInfoDTO> customerInfo(@RequestHeader(value = "X-Customer-Token", required = false) String customerToken,
                                                @RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        Customer customer = customerAuthService.resolveCustomer(customerToken, phoneHeader);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        return Result.ok(customerAuthService.toCustomerInfo(customer));
    }

    @GetMapping("/api/c/customer/orders")
    public Result<List<Orders>> customerOrders(@RequestHeader(value = "X-Customer-Token", required = false) String customerToken,
                                               @RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        Customer customer = customerAuthService.resolveCustomer(customerToken, phoneHeader);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        return Result.ok(orderService.getCustomerOrders(customer.getId()));
    }

    @GetMapping("/api/c/customer/points")
    public Result<List<PointsRecord>> customerPoints(@RequestHeader(value = "X-Customer-Token", required = false) String customerToken,
                                                     @RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        Customer customer = customerAuthService.resolveCustomer(customerToken, phoneHeader);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        return Result.ok(customerService.getPointsRecords(customer.getId()));
    }

    @PostMapping("/api/c/customer/bind-phone")
    public Result<CustomerInfoDTO> bindCustomerPhone(@RequestHeader(value = "X-Customer-Token", required = false) String customerToken,
                                                     @RequestBody BindCustomerPhoneRequest request) {
        Customer customer = customerAuthService.resolveByToken(customerToken);
        if (customer == null) {
            return Result.error(40101, "Please login first");
        }
        try {
            Customer mergedCustomer = customerService.bindPhoneToWechatCustomer(
                    customer.getId(),
                    request == null ? null : request.getPhone()
            );
            return Result.ok(customerAuthService.toCustomerInfo(mergedCustomer));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
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
