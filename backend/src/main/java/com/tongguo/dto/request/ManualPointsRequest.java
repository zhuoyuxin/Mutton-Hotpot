package com.tongguo.dto.request;

import lombok.Data;

@Data
public class ManualPointsRequest {
    private Integer customerId;
    private Integer points;
    private String remark;
}
