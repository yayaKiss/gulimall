package com.gulimall.order.to;

import com.gulimall.order.entity.OrderEntity;
import com.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateTo {
    private OrderEntity order;
    private List<OrderItemEntity> orderItems;
}
