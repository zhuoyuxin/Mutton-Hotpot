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

import java.time.LocalDateTime;
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
        if (customerId == null) throw new IllegalArgumentException("客户ID不能为空");
        if (points == null) throw new IllegalArgumentException("积分变动不能为空");
        if (points == 0) throw new IllegalArgumentException("积分变动不能为0");
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("客户不存在");
        int nextPoints = customer.getPoints() + points;
        if (nextPoints < 0) {
            throw new IllegalArgumentException("客户积分不能小于0");
        }
        customer.setPoints(nextPoints);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

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
            throw new IllegalArgumentException("手机号不能为空");
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
