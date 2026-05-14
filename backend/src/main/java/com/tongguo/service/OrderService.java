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
        validateCreateOrderItems(items);

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
            Integer dishId = toRequiredInt(item.get("dishId"), "菜品ID");
            Integer quantity = toRequiredInt(item.get("quantity"), "菜品数量");
            if (quantity <= 0) {
                throw new IllegalArgumentException("菜品数量必须大于0");
            }
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) {
                throw new IllegalArgumentException("存在已失效的菜品，请刷新后重试");
            }

            OrderItem oi = new OrderItem();
            oi.setDishId(dishId);
            oi.setDishName(dish.getName());
            oi.setDishPrice(dish.getPrice());
            oi.setQuantity(quantity);
            oi.setStatus(0);
            orderItems.add(oi);

            totalAmount += dish.getPrice() * quantity;
        }

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("订单至少需要一个有效菜品");
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

        order.setItems(orderItems);
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
        populateItems(orders);
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
        populateItems(orders);
        return orders;
    }

    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );
    }

    public List<Orders> getCustomerOrders(Integer customerId) {
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getCustomerId, customerId)
                        .orderByDesc(Orders::getCreateTime)
        );
        populateItems(orders);
        return orders;
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
        return populateItems(ordersMapper.selectById(orderId));
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
        return populateItems(ordersMapper.selectById(orderId));
    }

    private void validateCreateOrderItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("订单至少需要一个菜品");
        }
    }

    private Integer toRequiredInt(Object value, String fieldName) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return ((Number) value).intValue();
    }

    private Orders populateItems(Orders order) {
        if (order == null) {
            return null;
        }
        populateItems(Collections.singletonList(order));
        return order;
    }

    private void populateItems(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<Integer> orderIds = orders.stream()
                .map(Orders::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return;
        }

        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );
        Map<Integer, List<OrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), Collections.emptyList()));
        }
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
