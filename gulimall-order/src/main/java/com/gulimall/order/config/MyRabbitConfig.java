package com.gulimall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @PostConstruct
    public void initRabbitTemplate(){
        //当消息从发送者送达broker时的回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData   当前消息关联的唯一消息（看做消息的唯一id）
             * @param ack    消息是否成功失败
             * @param s      失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String s) {
                System.out.println("CorrelationData["+correlationData+"]====>ack["+ack+"]===>s["+s+"]");
            }
        });

        //当消息从exchange发送到queue时的回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                System.out.println("返回的数据：" + returnedMessage);
            }
        });

    }

}
