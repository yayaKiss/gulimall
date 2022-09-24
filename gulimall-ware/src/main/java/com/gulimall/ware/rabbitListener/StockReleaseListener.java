package com.gulimall.ware.rabbitListener;

import com.alibaba.fastjson.TypeReference;
import com.gulimall.common.to.mq.OrderTo;
import com.gulimall.common.to.mq.StockLockedTo;
import com.gulimall.common.to.mq.StockTaskDetailTo;
import com.gulimall.common.utils.R;
import com.gulimall.ware.dao.WareSkuDao;
import com.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.gulimall.ware.entity.WareOrderTaskEntity;
import com.gulimall.ware.feign.OrderServiceFeign;
import com.gulimall.ware.service.WareOrderTaskDetailService;
import com.gulimall.ware.service.WareOrderTaskService;
import com.gulimall.ware.service.WareSkuService;
import com.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = {"stock.release.stock.queue"})
public class StockReleaseListener {
    @Autowired
    WareSkuService wareSkuService;


    /**
     * 库存解锁：
     *     1、库存锁定成功，但是订单模块因为后续业务出现问题而回滚，需要进行库存解锁
     *     2、库存自身锁库存失败
     * @param stockLockedTo
     * @param message
     */
    @RabbitHandler
    public void handlerStockLockedRelease(StockLockedTo stockLockedTo, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的信息");
        try {
            wareSkuService.unLockStock(stockLockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handlerStockLockedRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的信息");
        try {
            wareSkuService.unLockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }


}
