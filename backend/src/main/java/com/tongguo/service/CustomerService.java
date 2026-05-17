package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.mapper.CustomerMapper;
import com.tongguo.mapper.OrdersMapper;
import com.tongguo.mapper.PointsRecordMapper;
import com.tongguo.mapper.SessionCheckoutMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private static final String DEFAULT_WECHAT_CUSTOMER_NAME = "微信顾客";

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private SessionCheckoutMapper sessionCheckoutMapper;

    @Transactional
    public Customer findOrCreateByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, normalizedPhone)
        );
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(normalizedPhone);
            customer.setName(normalizedPhone);
            customer.setPoints(0);
            customer.setTotalSpent(0);
            try {
                customerMapper.insert(customer);
            } catch (DataIntegrityViolationException e) {
                customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, normalizedPhone)
                );
                if (customer == null) {
                    throw e;
                }
            }
        }
        return customer;
    }

    @Transactional
    public Customer findOrCreateByOpenId(String openId) {
        String normalizedOpenId = normalizeRequiredText(openId, "OpenId is required");
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getOpenid, normalizedOpenId)
        );
        if (customer == null) {
            customer = new Customer();
            customer.setOpenid(normalizedOpenId);
            customer.setName(DEFAULT_WECHAT_CUSTOMER_NAME);
            customer.setPoints(0);
            customer.setTotalSpent(0);
            try {
                customerMapper.insert(customer);
            } catch (DataIntegrityViolationException e) {
                customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<Customer>().eq(Customer::getOpenid, normalizedOpenId)
                );
                if (customer == null) {
                    throw e;
                }
            }
        }
        return customer;
    }

    public Customer getByPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone == null) {
            return null;
        }
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, normalizedPhone)
        );
    }

    public Customer getByOpenId(String openId) {
        String normalizedOpenId = normalizeText(openId);
        if (normalizedOpenId == null) {
            return null;
        }
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getOpenid, normalizedOpenId)
        );
    }

    public List<Customer> search(String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Customer::getPhone, keyword).or().like(Customer::getName, keyword);
        }
        wrapper.orderByDesc(Customer::getUpdateTime);
        return customerMapper.selectList(wrapper);
    }

    public Customer detail(Integer id) {
        return customerMapper.selectById(id);
    }

    public List<PointsRecord> getPointsRecords(Integer customerId) {
        return pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getCustomerId, customerId)
                        .orderByDesc(PointsRecord::getCreateTime)
        );
    }

    @Transactional
    public void manualPoints(Integer customerId, Integer points, String remark) {
        if (customerId == null) throw new IllegalArgumentException("Customer id is required");
        if (points == null) throw new IllegalArgumentException("Points delta is required");
        if (points == 0) throw new IllegalArgumentException("Points delta cannot be zero");

        int affected = customerMapper.adjustBalances(customerId, points, 0);
        if (affected == 0) {
            Customer customer = customerMapper.selectById(customerId);
            if (customer == null) {
                throw new IllegalArgumentException("Customer does not exist");
            }
            throw new IllegalArgumentException("Customer points cannot be negative");
        }

        PointsRecord record = new PointsRecord();
        record.setCustomerId(customerId);
        record.setPoints(points);
        record.setType(points > 0 ? 1 : 2);
        record.setRemark(normalizeText(remark));
        pointsRecordMapper.insert(record);
    }

    @Transactional
    public Customer bindPhoneToWechatCustomer(Integer currentCustomerId, String phone) {
        if (currentCustomerId == null) {
            throw new IllegalArgumentException("顾客信息不存在");
        }

        String normalizedPhone = normalizeMobilePhone(phone);
        Customer currentCustomer = customerMapper.selectById(currentCustomerId);
        if (currentCustomer == null) {
            throw new IllegalArgumentException("顾客信息不存在");
        }
        if (normalizeOptionalText(currentCustomer.getOpenid()) == null) {
            throw new IllegalArgumentException("当前顾客未使用微信身份登录");
        }

        String currentPhone = normalizeOptionalText(currentCustomer.getPhone());
        if (currentPhone != null && !currentPhone.equals(normalizedPhone)) {
            throw new IllegalArgumentException("当前微信顾客已绑定其他手机号");
        }

        Customer phoneCustomer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, normalizedPhone)
        );
        if (phoneCustomer == null) {
            currentCustomer.setPhone(normalizedPhone);
            customerMapper.updateById(currentCustomer);
            return customerMapper.selectById(currentCustomerId);
        }
        if (phoneCustomer.getId().equals(currentCustomerId)) {
            return phoneCustomer;
        }

        if (normalizeOptionalText(phoneCustomer.getOpenid()) != null) {
            throw new IllegalArgumentException("该手机号已绑定其他微信顾客");
        }

        phoneCustomer.setPhone(null);
        customerMapper.updateById(phoneCustomer);

        Integer sourceCustomerId = phoneCustomer.getId();
        ordersMapper.moveCustomerOrders(sourceCustomerId, currentCustomerId);
        sessionCheckoutMapper.moveCustomerCheckouts(sourceCustomerId, currentCustomerId);
        pointsRecordMapper.moveCustomerPoints(sourceCustomerId, currentCustomerId);

        int sourcePoints = phoneCustomer.getPoints() == null ? 0 : phoneCustomer.getPoints();
        int sourceTotalSpent = phoneCustomer.getTotalSpent() == null ? 0 : phoneCustomer.getTotalSpent();
        if (sourcePoints != 0 || sourceTotalSpent != 0) {
            customerMapper.adjustBalances(currentCustomerId, sourcePoints, sourceTotalSpent);
        }

        currentCustomer = customerMapper.selectById(currentCustomerId);
        currentCustomer.setPhone(normalizedPhone);
        customerMapper.updateById(currentCustomer);
        customerMapper.deleteById(sourceCustomerId);

        return customerMapper.selectById(currentCustomerId);
    }

    public String normalizePhone(String phone) {
        return normalizeRequiredText(phone, "Phone is required");
    }

    public String normalizeMobilePhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (!normalizedPhone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("请输入正确的手机号");
        }
        return normalizedPhone;
    }

    public String normalizeRequiredText(String value, String errorMsg) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return normalized;
    }

    public String normalizeOptionalText(String value) {
        return normalizeText(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
