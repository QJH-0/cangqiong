package com.sky.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfiguration {

    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.routing";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "order.dlx.exchange";
    public static final String ORDER_DEAD_LETTER_QUEUE = "order.dlx.queue";
    public static final String ORDER_DEAD_LETTER_ROUTING_KEY = "order.dlx.routing";

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(ORDER_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", ORDER_DEAD_LETTER_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", ORDER_DEAD_LETTER_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return new Queue(ORDER_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderDeadLetterBinding() {
        return BindingBuilder.bind(orderDeadLetterQueue()).to(orderDeadLetterExchange()).with(ORDER_DEAD_LETTER_ROUTING_KEY);
    }
}
