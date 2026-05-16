package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tongguo.dto.TableOverviewDTO;
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

    public TableInfo getById(Integer id) {
        if (id == null) {
            return null;
        }
        return tableInfoMapper.selectById(id);
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

    public List<TableOverviewDTO> getTableOverview() {
        List<TableInfo> tables = tableInfoMapper.selectList(null);
        if (tables.isEmpty()) return Collections.emptyList();

        // 批量查出所有活跃 session
        List<DiningSession> activeSessions = diningSessionMapper.selectList(
                new LambdaQueryWrapper<DiningSession>().eq(DiningSession::getStatus, 0)
        );
        Map<Integer, DiningSession> sessionByTableId = activeSessions.stream()
                .filter(s -> s.getTableId() != null)
                .collect(Collectors.toMap(DiningSession::getTableId, s -> s, (a, b) -> a));

        // 批量查出所有活跃 session 的订单
        List<Integer> sessionIds = activeSessions.stream().map(DiningSession::getId).collect(Collectors.toList());
        Map<Integer, List<Orders>> ordersBySessionId = new HashMap<>();
        List<OrderItem> allActiveItems = Collections.emptyList();
        if (!sessionIds.isEmpty()) {
            List<Orders> allOrders = ordersMapper.selectList(
                    new LambdaQueryWrapper<Orders>()
                            .in(Orders::getSessionId, sessionIds)
                            .notIn(Orders::getStatus, 4, 5)
                            .orderByDesc(Orders::getCreateTime)
            );
            ordersBySessionId = allOrders.stream()
                    .collect(Collectors.groupingBy(Orders::getSessionId));

            // 批量查出所有订单项
            List<Integer> orderIds = allOrders.stream().map(Orders::getId).collect(Collectors.toList());
            if (!orderIds.isEmpty()) {
                allActiveItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
                );
            }
        }

        // 按订单分组订单项
        Map<Integer, List<OrderItem>> itemsByOrderId = allActiveItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<TableOverviewDTO> result = new ArrayList<>();
        for (TableInfo table : tables) {
            TableOverviewDTO dto = new TableOverviewDTO();
            dto.setId(table.getId());
            dto.setName(table.getName());
            dto.setArea(table.getArea());
            dto.setStatus(table.getStatus());

            DiningSession session = sessionByTableId.get(table.getId());
            if (session != null) {
                List<Orders> orders = ordersBySessionId.getOrDefault(session.getId(), Collections.emptyList());
                for (Orders order : orders) {
                    order.setItems(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()));
                }

                List<OrderItem> tableItems = orders.stream()
                        .flatMap(o -> itemsByOrderId.getOrDefault(o.getId(), Collections.emptyList()).stream())
                        .collect(Collectors.toList());

                long served = tableItems.stream().filter(i -> i.getStatus() != null && i.getStatus() == 2).count();
                long pending = tableItems.stream().filter(i -> i.getStatus() != null && (i.getStatus() == 0 || i.getStatus() == 1)).count();

                dto.setOrders(orders);
                dto.setTotalItems(tableItems.size());
                dto.setServedItems(served);
                dto.setPendingItems(pending);
            } else {
                dto.setOrders(Collections.emptyList());
                dto.setTotalItems(0);
                dto.setServedItems(0L);
                dto.setPendingItems(0L);
            }
            result.add(dto);
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
