package com.gulimall.order;

import com.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
@Slf4j
class GulimallOrderApplicationTests {

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private OrderReturnReasonEntity reasonEntity;

    @Test
    public void sendMessage(){
        String msg = "hello-world";
        for(long i=0;i<10;i++){
            reasonEntity = new OrderReturnReasonEntity();
            reasonEntity.setId(i);
            reasonEntity.setName("质量不好");
            reasonEntity.setCreateTime(new Date());
            rabbitTemplate.convertAndSend("myExchange-1","myExchange-1", reasonEntity);
            log.info("消息发送完成:{}",reasonEntity);
        }

    }


    @Test
    void createExchange(){
        //DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        DirectExchange exchange = new DirectExchange("myExchange-1",true,false);
        rabbitAdmin.declareExchange(exchange);
        log.info("交换机：{}",exchange);
    }

    @Test
    public void createQueue(){
        //Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments)
        Queue queue = new Queue("queue-5",true,false,false,null);
        rabbitAdmin.declareQueue(queue);
        log.info("队列：{}",queue);
    }

    @Test
    public void createBinding(){
        //Binding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments)
        Binding binding = new Binding("queue-5", Binding.DestinationType.QUEUE,"myExchange-1","myExchange-1",null);
        rabbitAdmin.declareBinding(binding);
        log.info("绑定信息：{}",binding);

    }




}
