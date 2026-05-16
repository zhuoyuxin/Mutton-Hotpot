package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.mapper.CustomerMapper;
import com.tongguo.mapper.PointsRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

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

    public Customer getByPhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone == null) {
            return null;
        }
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, normalizedPhone)
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

    public String normalizePhone(String phone) {
        String normalizedPhone = normalizeText(phone);
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Phone is required");
        }
        return normalizedPhone;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
