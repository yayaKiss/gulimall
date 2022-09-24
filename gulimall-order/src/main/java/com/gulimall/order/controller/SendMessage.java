package com.gulimall.order.controller;

import com.gulimall.order.entity.OrderEntity;
import com.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@Slf4j
public class SendMessage {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private OrderReturnReasonEntity reasonEntity;

    @GetMapping("/send")
    public String sendMessage(){
        for(long i=0;i<10;i++){
            reasonEntity = new OrderReturnReasonEntity();
            reasonEntity.setId(i);
            reasonEntity.setName("小米");
            reasonEntity.setCreateTime(new Date());
            rabbitTemplate.convertAndSend("myExchange-1","myExchange-1", reasonEntity);

            log.info("消息发送完成:{}",reasonEntity);

        }
        return "ok";
    }
}
