package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TableService {

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private DiningSessionMapper diningSessionMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public List<TableInfo> list() {
        return tableInfoMapper.selectList(null);
    }

    public void add(TableInfo tableInfo) {
        tableInfoMapper.insert(tableInfo);
    }

    public void update(TableInfo tableInfo) {
        if (tableInfo == null || tableInfo.getId() == null) {
            throw new IllegalArgumentException("桌台ID不能为空");
        }
        TableInfo existing = tableInfoMapper.selectById(tableInfo.getId());
        if (existing == null) {
            throw new IllegalArgumentException("桌台不存在");
        }
        if (tableInfo.getName() != null) {
            existing.setName(tableInfo.getName());
        }
        if (tableInfo.getArea() != null) {
            existing.setArea(tableInfo.getArea());
        }
        existing.setStatus(hasActiveSession(existing.getId()) ? 1 : 0);
        tableInfoMapper.updateById(existing);
    }

    public void delete(Integer id) {
        Long count = diningSessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, id)
                        .eq(DiningSession::getStatus, 0)
        );
        if (count > 0) {
            throw new IllegalArgumentException("该桌台存在进行中的就餐会话，无法删除");
        }
        tableInfoMapper.deleteById(id);
    }

    public String generateQRCode(Integer id, String baseUrl) throws WriterException, IOException {
        TableInfo table = tableInfoMapper.selectById(id);
        if (table == null) throw new IllegalArgumentException("桌台不存在");

        String content = baseUrl + "/c/login/" + id;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public List<Map<String, Object>> getTableOverview() {
        List<TableInfo> tables = tableInfoMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (TableInfo table : tables) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", table.getId());
            row.put("name", table.getName());
            row.put("area", table.getArea());
            row.put("status", table.getStatus());

            DiningSession activeSession = diningSessionMapper.selectOne(
                    new LambdaQueryWrapper<DiningSession>()
                            .eq(DiningSession::getTableId, table.getId())
                            .eq(DiningSession::getStatus, 0)
            );

            if (activeSession != null) {
                List<Orders> orders = ordersMapper.selectList(
                        new LambdaQueryWrapper<Orders>()
                                .eq(Orders::getSessionId, activeSession.getId())
                                .notIn(Orders::getStatus, 4, 5)
                                .orderByDesc(Orders::getCreateTime)
                );

                List<Integer> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
                List<OrderItem> allItems = orderIds.isEmpty() ? Collections.emptyList() :
                        orderItemMapper.selectList(
                                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
                        );

                long served = allItems.stream().filter(i -> i.getStatus() == 2).count();
                long pending = allItems.stream().filter(i -> i.getStatus() == 0 || i.getStatus() == 1).count();

                Map<Integer, List<OrderItem>> itemsByOrder = allItems.stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));
                for (Orders order : orders) {
                    order.setItems(itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()));
                }

                row.put("orders", orders);
                row.put("totalItems", allItems.size());
                row.put("servedItems", served);
                row.put("pendingItems", pending);
            } else {
                row.put("orders", Collections.emptyList());
                row.put("totalItems", 0);
                row.put("servedItems", 0L);
                row.put("pendingItems", 0L);
            }
            result.add(row);
        }
        return result;
    }

    private boolean hasActiveSession(Integer tableId) {
        return diningSessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        ) > 0;
    }
}
