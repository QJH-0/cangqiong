package com.sky.service.impl;

import com.sky.config.RabbitMqConfiguration;
import com.sky.message.OrderTimeoutMessage;
import com.sky.service.OrderTimeoutPublisher;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderTimeoutPublisherImpl implements OrderTimeoutPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long timeoutMillis;

    public OrderTimeoutPublisherImpl(RabbitTemplate rabbitTemplate,
                                     @Value("${sky.rabbitmq.order.timeout-minutes:15}") long timeoutMinutes) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutMillis = timeoutMinutes * 60 * 1000;
    }

    @Override
    public void publish(OrderTimeoutMessage message) {
        if (message == null || message.getOrderId() == null) {
            throw new IllegalArgumentException("OrderTimeoutMessage 与 orderId 不能为空");
        }
        MessagePostProcessor ttlProcessor = amqpMessage -> {
            amqpMessage.getMessageProperties().setExpiration(String.valueOf(timeoutMillis));
            return amqpMessage;
        };
        rabbitTemplate.convertAndSend(
                RabbitMqConfiguration.ORDER_DELAY_EXCHANGE,
                RabbitMqConfiguration.ORDER_DELAY_ROUTING_KEY,
                message,
                ttlProcessor
        );
    }
}
