package com.sky.service;

import com.sky.message.OrderTimeoutMessage;

public interface OrderTimeoutPublisher {

    void publish(OrderTimeoutMessage message);
}
