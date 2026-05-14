package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.mapper.CustomerMapper;
import com.tongguo.mapper.PointsRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)
        );
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setName(phone);
            customer.setPoints(0);
            customer.setTotalSpent(0);
            customerMapper.insert(customer);
        }
        return customer;
    }

    public Customer getByPhone(String phone) {
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)
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
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("客户不存在");
        customer.setPoints(customer.getPoints() + points);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

        PointsRecord record = new PointsRecord();
        record.setCustomerId(customerId);
        record.setPoints(points);
        record.setType(points > 0 ? 1 : 2);
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }
}
