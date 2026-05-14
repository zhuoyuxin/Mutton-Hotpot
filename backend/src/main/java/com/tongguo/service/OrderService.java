package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DiningSessionMapper sessionMapper;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CustomerService customerService;

    @Transactional
    public Orders createOrder(Integer tableId, Integer sessionId,
                               List<Map<String, Object>> items,
                               String phone, String remark) {
        DiningSession session;
        if (sessionId != null) {
            session = sessionMapper.selectById(sessionId);
            if (session == null || session.getStatus() != 0) {
                throw new IllegalArgumentException("会话不存在或已结束");
            }
        } else if (tableId != null) {
            session = sessionService.getOrCreateSession(tableId);
        } else {
            session = sessionService.createWalkInSession();
        }

        String orderNo = generateOrderNo();

        int totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Integer dishId = (Integer) item.get("dishId");
            Integer quantity = ((Number) item.get("quantity")).intValue();
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) continue;

            OrderItem oi = new OrderItem();
            oi.setDishId(dishId);
            oi.setDishName(dish.getName());
            oi.setDishPrice(dish.getPrice());
            oi.setQuantity(quantity);
            oi.setStatus(0);
            orderItems.add(oi);

            totalAmount += dish.getPrice() * quantity;
        }

        Integer customerId = null;
        if (phone != null && !phone.trim().isEmpty()) {
            Customer customer = customerService.findOrCreateByPhone(phone);
            customerId = customer.getId();
        }

        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setSessionId(session.getId());
        order.setTableId(tableId);
        order.setCustomerId(customerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setRemark(remark);
        ordersMapper.insert(order);

        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        return order;
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return date + random;
    }

    public List<Orders> listOrders(Integer status, Integer tableId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Orders::getStatus, status);
        if (tableId != null) wrapper.eq(Orders::getTableId, tableId);
        wrapper.orderByDesc(Orders::getCreateTime);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        if (orders.isEmpty()) return orders;

        List<Integer> orderIds = new ArrayList<>();
        for (Orders o : orders) { orderIds.add(o.getId()); }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );
        Map<Integer, List<OrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), Collections.emptyList()));
        }
        return orders;
    }

    public List<Orders> getTableCurrentOrders(Integer tableId) {
        DiningSession session = sessionService.getCurrentByTableId(tableId);
        if (session == null) return Collections.emptyList();
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, session.getId())
                        .notIn(Orders::getStatus, 4, 5)
                        .orderByDesc(Orders::getCreateTime)
        );
        if (orders.isEmpty()) return orders;

        List<Integer> orderIds = new ArrayList<>();
        for (Orders o : orders) { orderIds.add(o.getId()); }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
        );
        Map<Integer, List<OrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), Collections.emptyList()));
        }
        return orders;
    }

    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );
    }

    public List<Orders> getCustomerOrders(Integer customerId) {
        return ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getCustomerId, customerId)
                        .orderByDesc(Orders::getCreateTime)
        );
    }

    @Transactional
    public Orders confirmOrder(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if (order.getStatus() != 0) throw new IllegalArgumentException("订单状态不允许确认");

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        for (OrderItem item : items) {
            if (item.getStatus() != 0) continue;
            int affected = dishMapper.deductStock(item.getDishId(), item.getQuantity());
            if (affected > 0) {
                item.setStatus(1);
            } else {
                item.setStatus(3);
            }
            orderItemMapper.updateById(item);
        }

        refreshOrderStatus(orderId);
        return ordersMapper.selectById(orderId);
    }

    public OrderItem serveItem(Integer itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) throw new IllegalArgumentException("订单项不存在");
        if (item.getStatus() != 1) throw new IllegalArgumentException("只能上待上菜的菜品");
        item.setStatus(2);
        orderItemMapper.updateById(item);

        refreshOrderStatus(item.getOrderId());
        return item;
    }

    public OrderItem cancelItem(Integer itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) throw new IllegalArgumentException("订单项不存在");

        if (item.getStatus() == 0) {
            item.setStatus(4);
        } else if (item.getStatus() == 1) {
            dishMapper.addStock(item.getDishId(), item.getQuantity());
            item.setStatus(4);
        } else {
            throw new IllegalArgumentException("该状态不允许退菜");
        }
        orderItemMapper.updateById(item);

        refreshOrderStatus(item.getOrderId());
        return item;
    }

    @Transactional
    public Orders cancelOrder(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在");

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        boolean hasServed = items.stream().anyMatch(i -> i.getStatus() == 2);
        if (hasServed) throw new IllegalArgumentException("订单中有已上菜菜品，无法整单取消");

        for (OrderItem item : items) {
            if (item.getStatus() == 1) {
                dishMapper.addStock(item.getDishId(), item.getQuantity());
                item.setStatus(4);
            } else if (item.getStatus() == 0) {
                item.setStatus(4);
            }
            if (item.getStatus() != 3) {
                orderItemMapper.updateById(item);
            }
        }

        refreshOrderStatus(orderId);
        return ordersMapper.selectById(orderId);
    }

    private void refreshOrderStatus(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        if (items.isEmpty()) {
            order.setStatus(5);
            ordersMapper.updateById(order);
            return;
        }

        long status0 = items.stream().filter(i -> i.getStatus() == 0).count();
        long status1 = items.stream().filter(i -> i.getStatus() == 1).count();
        long status2 = items.stream().filter(i -> i.getStatus() == 2).count();

        if (status2 == 0 && (status0 + status1) == 0) {
            order.setStatus(5);
        } else if (status0 > 0) {
            order.setStatus(0);
        } else if (status2 > 0 && status1 > 0) {
            order.setStatus(2);
        } else if (status2 > 0 && status1 == 0) {
            order.setStatus(3);
        } else if (status1 > 0) {
            order.setStatus(1);
        } else {
            order.setStatus(5);
        }

        order.setUpdateTime(LocalDateTime.now());
        ordersMapper.updateById(order);
    }
}
