package com.tongguo.dto;

import com.tongguo.entity.Customer;
import com.tongguo.entity.Orders;
import com.tongguo.entity.PointsRecord;
import lombok.Data;

import java.util.List;

@Data
public class CustomerDetailDTO {
    private Customer customer;
    private List<PointsRecord> pointsRecords;
    private List<Orders> orders;
    private List<CustomerConsumptionDTO> consumptionRecords;
}
