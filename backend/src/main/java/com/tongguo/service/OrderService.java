package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.dto.request.OrderItemRequest;
import com.tongguo.entity.Customer;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.Dish;
import com.tongguo.entity.OrderItem;
import com.tongguo.entity.Orders;
import com.tongguo.entity.TableInfo;
import com.tongguo.mapper.DiningSessionMapper;
import com.tongguo.mapper.DishMapper;
import com.tongguo.mapper.OrderItemMapper;
import com.tongguo.mapper.OrdersMapper;
import com.tongguo.mapper.TableInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final int ORDER_STATUS_PROCESSING_CONFIRM = -1;
    private static final int ORDER_STATUS_PROCESSING_CANCEL = -2;

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

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Transactional
    public Orders createOrder(Integer tableId, Integer sessionId,
                              List<OrderItemRequest> items,
                              String phone, String remark) {
        validateCreateOrderItems(items);

        DiningSession session;
        if (sessionId != null) {
            session = sessionMapper.selectById(sessionId);
            if (session == null || session.getStatus() != 0) {
                throw new IllegalArgumentException("Session does not exist or is already closed");
            }
        } else if (tableId != null) {
            session = sessionService.getOrCreateSession(tableId);
        } else {
            session = sessionService.createWalkInSession();
        }

        Integer resolvedTableId = session.getTableId();
        if (sessionId != null && tableId != null && !Objects.equals(tableId, resolvedTableId)) {
            throw new IllegalArgumentException("Table does not match the session");
        }

        List<PendingOrderItem> pendingItems = new ArrayList<>();
        Map<Integer, Integer> requestedQtyByDishId = new LinkedHashMap<>();
        int totalAmount = 0;
        for (OrderItemRequest item : items) {
            Integer dishId = toRequiredInt(item == null ? null : item.getDishId(), "Dish id");
            Integer quantity = toRequiredInt(item == null ? null : item.getQuantity(), "Dish quantity");
            if (quantity <= 0) {
                throw new IllegalArgumentException("Dish quantity must be greater than 0");
            }

            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) {
                throw new IllegalArgumentException("Dish does not exist anymore, please refresh and retry");
            }
            if (!Objects.equals(dish.getStatus(), 1)) {
                throw new IllegalArgumentException("Dish is no longer on sale, please refresh and retry");
            }

            requestedQtyByDishId.merge(dishId, quantity, Integer::sum);
            pendingItems.add(new PendingOrderItem(dish, quantity));
            totalAmount += dish.getPrice() * quantity;
        }

        validateSellableStock(requestedQtyByDishId, pendingItems);

        Integer customerId = null;
        if (phone != null && !phone.trim().isEmpty()) {
            Customer customer = customerService.findOrCreateByPhone(phone);
            customerId = customer.getId();
        }

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setSessionId(session.getId());
        order.setTableId(resolvedTableId);
        order.setCustomerId(customerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setRemark(remark);
        ordersMapper.insert(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (PendingOrderItem pendingItem : pendingItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setDishId(pendingItem.dish.getId());
            orderItem.setDishName(pendingItem.dish.getName());
            orderItem.setDishPrice(pendingItem.dish.getPrice());
            orderItem.setQuantity(pendingItem.quantity);
            orderItem.setStatus(0);
            orderItemMapper.insert(orderItem);
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        return order;
    }

    private void validateSellableStock(Map<Integer, Integer> requestedQtyByDishId, List<PendingOrderItem> pendingItems) {
        Map<Integer, Dish> dishById = pendingItems.stream()
                .collect(Collectors.toMap(item -> item.dish.getId(), item -> item.dish, (left, right) -> left));

        for (Map.Entry<Integer, Integer> entry : requestedQtyByDishId.entrySet()) {
            Dish dish = dishById.get(entry.getKey());
            int currentStock = dish.getStock() == null ? 0 : dish.getStock();
            Integer pendingQtyRaw = orderItemMapper.sumPendingQuantityByDishId(entry.getKey());
            int pendingQty = pendingQtyRaw == null ? 0 : pendingQtyRaw;
            int availableToOrder = currentStock - pendingQty;
            if (entry.getValue() > availableToOrder) {
                throw new IllegalArgumentException("Insufficient stock for dish: " + dish.getName());
            }
        }
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return date + random;
    }

    public List<Orders> listOrders(Integer status, Integer tableId,
                                   List<Integer> statuses, String startDate, String endDate) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Orders::getStatus, status);
        if (statuses != null && !statuses.isEmpty()) wrapper.in(Orders::getStatus, statuses);
        if (tableId != null) wrapper.eq(Orders::getTableId, tableId);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Orders::getCreateTime, parseDateOrThrow(startDate, "Invalid start date").atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Orders::getCreateTime, parseDateOrThrow(endDate, "Invalid end date").atTime(23, 59, 59));
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        populateItems(orders);
        populateTableInfo(orders);
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
        populateTableInfo(orders);
        return orders;
    }

    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
    }

    public List<Orders> getCustomerOrders(Integer customerId) {
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getCustomerId, customerId)
                        .orderByDesc(Orders::getCreateTime)
        );
        populateItems(orders);
        populateTableInfo(orders);
        return orders;
    }

    @Transactional
    public Orders confirmOrder(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("Order does not exist");
        if (isBusyStatus(order.getStatus())) {
            throw new IllegalArgumentException("Order is being processed, please retry later");
        }
        if (!Objects.equals(order.getStatus(), 0)) {
            if (Objects.equals(order.getStatus(), 4)) {
                throw new IllegalArgumentException("Settled orders cannot be confirmed");
            }
            return populateItems(order);
        }

        int claimed = ordersMapper.updateStatusIfCurrent(orderId, 0, ORDER_STATUS_PROCESSING_CONFIRM);
        if (claimed == 0) {
            Orders latest = ordersMapper.selectById(orderId);
            if (latest == null) throw new IllegalArgumentException("Order does not exist");
            if (isBusyStatus(latest.getStatus())) {
                throw new IllegalArgumentException("Order is being processed, please retry later");
            }
            return populateItems(latest);
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        for (OrderItem item : items) {
            if (!Objects.equals(item.getStatus(), 0)) {
                continue;
            }
            int affected = dishMapper.deductStock(item.getDishId(), item.getQuantity());
            orderItemMapper.updateStatusIfCurrent(item.getId(), 0, affected > 0 ? 1 : 3);
        }

        refreshOrderStatus(orderId);
        return populateItems(ordersMapper.selectById(orderId));
    }

    @Transactional
    public OrderItem serveItem(Integer itemId, Integer serveQuantity, Integer expectedQuantity) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) throw new IllegalArgumentException("Order item does not exist");

        ensureOrderMutable(item.getOrderId());
        if (Objects.equals(item.getStatus(), 2)) {
            return item;
        }
        if (!Objects.equals(item.getStatus(), 1)) {
            throw new IllegalArgumentException("Only waiting items can be served");
        }

        int currentQuantity = requirePositiveQuantity(item.getQuantity(), "Order item quantity is invalid");
        int normalizedServeQuantity = serveQuantity == null ? currentQuantity : serveQuantity;
        if (normalizedServeQuantity <= 0) {
            throw new IllegalArgumentException("Serve quantity must be greater than 0");
        }
        if (normalizedServeQuantity > currentQuantity) {
            throw new IllegalArgumentException("Serve quantity cannot exceed waiting quantity");
        }

        OrderItem result;
        if (normalizedServeQuantity == currentQuantity) {
            int affected = expectedQuantity == null
                    ? orderItemMapper.updateStatusIfCurrent(itemId, 1, 2)
                    : orderItemMapper.updateStatusIfCurrentAndQuantity(itemId, 1, 2, expectedQuantity);
            if (affected == 0) {
                result = resolveServeItemRace(itemId, expectedQuantity);
            } else {
                result = orderItemMapper.selectById(itemId);
            }
        } else {
            result = servePartialItem(item, normalizedServeQuantity, expectedQuantity);
        }

        refreshOrderStatus(item.getOrderId());
        return result;
    }

    @Transactional
    public OrderItem cancelItem(Integer itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) throw new IllegalArgumentException("Order item does not exist");

        ensureOrderMutable(item.getOrderId());
        if (Objects.equals(item.getStatus(), 4)) {
            return item;
        }

        if (Objects.equals(item.getStatus(), 0)) {
            int affected = orderItemMapper.updateStatusIfCurrent(itemId, 0, 4);
            if (affected == 0) {
                return resolveCancelItemRace(itemId);
            }
        } else if (Objects.equals(item.getStatus(), 1)) {
            int affected = orderItemMapper.updateStatusIfCurrent(itemId, 1, 4);
            if (affected == 0) {
                return resolveCancelItemRace(itemId);
            }
            dishMapper.addStock(item.getDishId(), item.getQuantity());
        } else {
            throw new IllegalArgumentException("Current item status cannot be cancelled");
        }

        refreshOrderStatus(item.getOrderId());
        return orderItemMapper.selectById(itemId);
    }

    @Transactional
    public Orders cancelOrder(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new IllegalArgumentException("Order does not exist");
        if (Objects.equals(order.getStatus(), 5)) {
            return populateItems(order);
        }
        ensureOrderMutable(order);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        boolean hasServed = items.stream().anyMatch(item -> Objects.equals(item.getStatus(), 2));
        if (hasServed) {
            throw new IllegalArgumentException("Orders with served items cannot be cancelled");
        }

        int claimed = ordersMapper.updateStatusIfCurrent(orderId, order.getStatus(), ORDER_STATUS_PROCESSING_CANCEL);
        if (claimed == 0) {
            Orders latest = ordersMapper.selectById(orderId);
            if (latest == null) throw new IllegalArgumentException("Order does not exist");
            if (Objects.equals(latest.getStatus(), 5)) {
                return populateItems(latest);
            }
            ensureOrderMutable(latest);
            return populateItems(latest);
        }

        for (OrderItem item : items) {
            if (Objects.equals(item.getStatus(), 1)) {
                int affected = orderItemMapper.updateStatusIfCurrent(item.getId(), 1, 4);
                if (affected > 0) {
                    dishMapper.addStock(item.getDishId(), item.getQuantity());
                }
            } else if (Objects.equals(item.getStatus(), 0)) {
                orderItemMapper.updateStatusIfCurrent(item.getId(), 0, 4);
            }
        }

        refreshOrderStatus(orderId);
        return populateItems(ordersMapper.selectById(orderId));
    }

    private OrderItem resolveCancelItemRace(Integer itemId) {
        OrderItem latest = orderItemMapper.selectById(itemId);
        if (latest == null) throw new IllegalArgumentException("Order item does not exist");
        if (Objects.equals(latest.getStatus(), 4)) {
            return latest;
        }
        throw new IllegalArgumentException("Current item status cannot be cancelled");
    }

    private OrderItem servePartialItem(OrderItem item, int serveQuantity, Integer expectedQuantity) {
        int compareQuantity = expectedQuantity != null ? expectedQuantity
                : requirePositiveQuantity(item.getQuantity(), "Order item quantity is invalid");
        int affected = orderItemMapper.deductWaitingQuantity(item.getId(), compareQuantity, serveQuantity);
        if (affected == 0) {
            return resolveServeItemRace(item.getId(), compareQuantity);
        }

        OrderItem servedItem = new OrderItem();
        servedItem.setOrderId(item.getOrderId());
        servedItem.setDishId(item.getDishId());
        servedItem.setDishName(item.getDishName());
        servedItem.setDishPrice(item.getDishPrice());
        servedItem.setQuantity(serveQuantity);
        servedItem.setStatus(2);
        orderItemMapper.insert(servedItem);
        return servedItem;
    }

    private OrderItem resolveServeItemRace(Integer itemId, Integer expectedQuantity) {
        OrderItem latest = orderItemMapper.selectById(itemId);
        if (latest == null) {
            throw new IllegalArgumentException("Order item does not exist");
        }
        if (Objects.equals(latest.getStatus(), 2)) {
            return latest;
        }
        if (Objects.equals(latest.getStatus(), 1) && expectedQuantity != null
                && !Objects.equals(latest.getQuantity(), expectedQuantity)) {
            throw new IllegalArgumentException("Order item quantity has changed, please refresh and retry");
        }
        throw new IllegalArgumentException("Only waiting items can be served");
    }

    private void validateCreateOrderItems(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one dish");
        }
    }

    private Integer toRequiredInt(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private int requirePositiveQuantity(Integer quantity, String errorMsg) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(errorMsg);
        }
        return quantity;
    }

    private LocalDate parseDateOrThrow(String dateStr, String errorMsg) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(errorMsg);
        }
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

    private void populateTableInfo(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<Integer> tableIds = orders.stream()
                .map(Orders::getTableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tableIds.isEmpty()) {
            return;
        }
        Map<Integer, TableInfo> tableMap = tableInfoMapper.selectList(
                new LambdaQueryWrapper<TableInfo>().in(TableInfo::getId, tableIds)
        ).stream().collect(Collectors.toMap(TableInfo::getId, table -> table));
        for (Orders order : orders) {
            TableInfo table = tableMap.get(order.getTableId());
            if (table != null) {
                order.setTableName(table.getName());
                order.setTableArea(table.getArea());
            }
        }
    }

    private Orders ensureOrderMutable(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order does not exist");
        }
        ensureOrderMutable(order);
        return order;
    }

    private void ensureOrderMutable(Orders order) {
        if (isBusyStatus(order.getStatus())) {
            throw new IllegalArgumentException("Order is being processed, please retry later");
        }
        if (Objects.equals(order.getStatus(), 4)) {
            throw new IllegalArgumentException("Settled orders cannot be changed");
        }
        if (Objects.equals(order.getStatus(), 5)) {
            throw new IllegalArgumentException("Cancelled orders cannot be changed");
        }
    }

    private boolean isBusyStatus(Integer status) {
        return status != null && status < 0;
    }

    private void refreshOrderStatus(Integer orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || Objects.equals(order.getStatus(), 4)) {
            return;
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        if (items.isEmpty()) {
            order.setStatus(5);
            order.setUpdateTime(LocalDateTime.now());
            ordersMapper.updateById(order);
            return;
        }

        long status0 = items.stream().filter(item -> Objects.equals(item.getStatus(), 0)).count();
        long status1 = items.stream().filter(item -> Objects.equals(item.getStatus(), 1)).count();
        long status2 = items.stream().filter(item -> Objects.equals(item.getStatus(), 2)).count();

        if (status2 == 0 && (status0 + status1) == 0) {
            order.setStatus(5);
        } else if (status0 > 0) {
            order.setStatus(0);
        } else if (status2 > 0 && status1 > 0) {
            order.setStatus(2);
        } else if (status2 > 0) {
            order.setStatus(3);
        } else if (status1 > 0) {
            order.setStatus(1);
        } else {
            order.setStatus(5);
        }

        order.setUpdateTime(LocalDateTime.now());
        ordersMapper.updateById(order);
    }

    private static final class PendingOrderItem {
        private final Dish dish;
        private final int quantity;

        private PendingOrderItem(Dish dish, int quantity) {
            this.dish = dish;
            this.quantity = quantity;
        }
    }
}
