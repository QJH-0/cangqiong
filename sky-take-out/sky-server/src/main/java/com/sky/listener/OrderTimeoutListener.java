package com.sky.listener;

import com.sky.config.RabbitMqConfiguration;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.message.OrderTimeoutMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTimeoutListener {

    private final OrderMapper orderMapper;

    public OrderTimeoutListener(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @RabbitListener(queues = RabbitMqConfiguration.ORDER_DEAD_LETTER_QUEUE)
    public void handle(OrderTimeoutMessage message) {
        if (message == null || message.getOrderId() == null) {
            log.warn("ignore invalid order timeout message, payload={}", message);
            return;
        }
        Orders order = orderMapper.getById(message.getOrderId());
        if (order == null || !Orders.PENDING_PAYMENT.equals(order.getStatus())) {
            return;
        }

        Orders updateOrder = new Orders();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Orders.CANCELLED);
        updateOrder.setCancelReason("订单超时，自动取消");
        updateOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(updateOrder);
        log.info("cancel timeout order by mq, orderId={}, orderNumber={}", message.getOrderId(), message.getOrderNumber());
    }
}
