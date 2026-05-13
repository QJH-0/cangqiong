package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 *处理超时订单
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟执行一次
    public void processTimeoutOrder(){
        log.info("处理超时订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //所有timeout未支付订单
       List<Orders> ordersList=  orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
       if(ordersList != null && ordersList.size() > 0){
           for (Orders orders : ordersList) {
               orders.setStatus(Orders.CANCELLED);
               orders.setCancelReason("订单超时，自动取消");
               orders.setCancelTime(LocalDateTime.now());
               orderMapper.update(orders);
           }
       }
    }
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨一点，处理派送中的订单
    public void processDeliveryOrder(){
        log.info("处理派送订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
