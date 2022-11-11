package com.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.gulimall.common.to.mq.OrderTo;
import com.gulimall.common.utils.R;
import com.gulimall.common.vo.MemberRespVo;
import com.gulimall.order.constant.OrderConstant;
import com.gulimall.order.entity.OrderItemEntity;
import com.gulimall.order.entity.OrderReturnReasonEntity;
import com.gulimall.order.enume.OrderStatusEnum;
import com.gulimall.order.feign.*;

import com.gulimall.order.interceptor.LoginInterceptor;
import com.gulimall.order.service.OrderItemService;
import com.gulimall.order.to.OrderCreateTo;
import com.gulimall.order.vo.*;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.order.dao.OrderDao;
import com.gulimall.order.entity.OrderEntity;
import com.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;


@Service("orderService")
@RabbitListener(queues = {"queue-5"})
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    private ThreadLocal<OrderSubmitVo>  orderSubmitLocal = new ThreadLocal<>();

    @Autowired
    MemberServiceFeign memberServiceFeign;
    @Autowired
    CartServiceFeign cartServiceFeign;

    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    ProductServiceFeign productServiceFeign;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        //拦截器获取登录信息
        MemberRespVo memberRespVo = LoginInterceptor.threadLocal.get();
        //异步远程调用设置会员地址信息
        /**
         * 由于线程不一样，即使设置了RequestIntercept也是无效（底层是ThreadLocal写的），openfeign还是会丢失请求头
         * 所以提前给当前线程中添加
         */
        //TODO :获取当前线程请求头信息(解决Feign异步调用丢失请求头问题)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程调用用户服务
            List<MemberAddressVo> memberAddresses = memberServiceFeign.getMemberAddress(memberRespVo.getId());

            orderConfirmVo.setMemberAddressVos(memberAddresses);
        }, executor);

        //异步远程调用设置购物项信息
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> userCartItems = cartServiceFeign.getCurrentUserCartItems();
            orderConfirmVo.setItems(userCartItems);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> skuIds = items.stream().map(item -> {
                return item.getSkuId();
            }).collect(Collectors.toList());
            //远程调用库存服务查询商品是否有库存
            R skuHasStock = wmsFeignService.getSkuHasStock(skuIds);
            List<OrderHasStockVo> hasStockList = skuHasStock.getData("data",new TypeReference<List<OrderHasStockVo>>() {
            });
            if(hasStockList != null){
                Map<Long, Boolean> map = hasStockList.stream().collect(Collectors.toMap(OrderHasStockVo::getSkuId, OrderHasStockVo::getHasStock));
                orderConfirmVo.setStocks(map);
            }
        },executor);

        //用户积分
        orderConfirmVo.setIntegration(memberRespVo.getIntegration());

        //防重令牌
        String token = UUID.randomUUID().toString().replace("-","");
        orderConfirmVo.setOrderToken(token);
        //将令牌也存一分到redis
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(),token);

        CompletableFuture.allOf(memberFuture,cartFuture).get();

        return orderConfirmVo;
    }

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        orderSubmitLocal.set(submitVo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        MemberRespVo loginUser = LoginInterceptor.threadLocal.get();
        //1、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = submitVo.getOrderToken();

        //通过lua脚本原子验证令牌和删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getId()),
                orderToken);

        if(result == 0L){
            //验证失败
            responseVo.setCode(1);
        }else{
            //验证成功
            //下单，创建订单、收集组装数据
            OrderCreateTo orderTo = createOrderTo();
            //验价  ===》 生成的订单总金额（查询了购物车被选中项） PK  前端页面传过来的价格
            BigDecimal payAmount = orderTo.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            //验价成功
            if(Math.abs(payAmount.subtract(payPrice).intValue()) <= 0.01){
                //保存订单及订单项到数据库
                saveOrder(orderTo);
                //锁定库存
                WareSkuLockVo lockVo = new WareSkuLockVo();
                //锁定的订单号
                lockVo.setOrderSn(orderTo.getOrder().getOrderSn());
                //锁定的订单项（只有skuId，count两个信息即可）
                List<OrderItemVo> collect = orderTo.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(collect);

                //TODO 远程调用锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode() == 0){
                    //锁定成功
                    responseVo.setOrder(orderTo.getOrder());
                }else{
                    //锁定失败
                    responseVo.setCode(3);
                }

                // TODO 模拟远程调用优惠服务失败
//                int a = 10 / 0;

                //订单创建完成，发送到消息中间件
                rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",orderTo.getOrder());
            }else{
                //金额对比，出现很大差别
                responseVo.setCode(2);
            }
        }
        return responseVo;
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity order) {
        if(order.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            OrderEntity entity = new OrderEntity();
            entity.setId(order.getId());
            entity.setStatus(OrderStatusEnum.CANCLED.getCode());
            boolean update = this.updateById(entity);
            if(!update){
                throw new RuntimeException("订单取消失败!");
            }

            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order,orderTo);

            //订单关闭完成，主动发消息给解锁库存
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other.#",orderTo);
        }

    }

    private void saveOrder(OrderCreateTo orderTo) {
        OrderEntity order = orderTo.getOrder();
        order.setCreateTime(new Date());
        order.setModifyTime(new Date());
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        this.save(order);

        List<OrderItemEntity> orderItems = orderTo.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 构建订单传输VO：包括订单信息和订单项信息
     * @return
     */
    private OrderCreateTo createOrderTo() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //生成订单号
        String orderSn = IdWorker.getTimeId();
        //生成订单
        OrderEntity order = createOrder(orderSn);
        orderCreateTo.setOrder(order);
        //生成订单项
        List<OrderItemEntity> orderItems = createOrderItems(orderSn);
        orderCreateTo.setOrderItems(orderItems);
        //计算价格
        compute(order,orderItems);

        return orderCreateTo;
    }

    private void compute(OrderEntity order, List<OrderItemEntity> orderItems) {
        BigDecimal total = new BigDecimal("0");
        //优惠券抵扣金额
        BigDecimal coupon = new BigDecimal("0");
        //促销优化金额
        BigDecimal promotion = new BigDecimal("0");
        //积分抵扣金额
        BigDecimal integrationAmount = new BigDecimal("0");

        //赠送积分
        BigDecimal integration = new BigDecimal("0");
        //赠送成长值
        BigDecimal growth = new BigDecimal("0");
        for(OrderItemEntity orderItem : orderItems){
            total = total.add(orderItem.getRealAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            integrationAmount.add(orderItem.getIntegrationAmount());
            coupon = coupon.add(orderItem.getCouponAmount());
            integration = integration.add(new BigDecimal(orderItem.getGiftIntegration()));
            growth = growth.add(new BigDecimal(orderItem.getGiftGrowth()));
        }
        //订单项的总金额（已经构建每个订单项时减掉了）
        order.setTotalAmount(total);
        //订最终要支付的金额(需要 + 运费)
        order.setPayAmount(total.add(order.getFreightAmount()));
        //整个订单的总的优惠价格
        order.setPromotionAmount(promotion);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(coupon);
        //订单的一些积分
        order.setIntegration(integration.intValue());
        order.setGrowth(growth.intValue());

    }

    /**
     * 构建所有已选择订单项
     * @return
     */
    private List<OrderItemEntity> createOrderItems(String orderSn) {
        //远程调用获取现在购物车的购物项（购物车价格再次进行查询，确实是最新的）
        List<OrderItemVo> userCartItems = cartServiceFeign.getCurrentUserCartItems();
        List<OrderItemEntity> orderItems = null;
        if(userCartItems != null && userCartItems.size() >0){
            orderItems = userCartItems.stream()
                    //购物车勾选的购物项，才会将其生成订单项
                    .filter(cartItem -> cartItem.getCheck())
                    .map(cartItem -> {
                //对每一个购物车的购物项将其转为订单项
                OrderItemEntity orderItem = createOrderItem(cartItem);
                orderItem.setOrderSn(orderSn);
                return orderItem;
            }).collect(Collectors.toList());
        }
        return orderItems;
    }

    private OrderItemEntity createOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItem = new OrderItemEntity();
        //订单项信息：SPU信息  SKU信息   优惠信息  积分信息
        //sku信息
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setSkuName(cartItem.getTitle());
        orderItem.setSkuPrice(cartItem.getPrice());
        orderItem.setSkuPic(cartItem.getImage());
        orderItem.setSkuAttrsVals(StringUtils.collectionToDelimitedString(cartItem.getSkuAttrValues(),""));
        orderItem.setSkuQuantity(cartItem.getCount());

        //spu信息
        R r = productServiceFeign.getSpuInfoBySkuId(cartItem.getSkuId());
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>(){});
        orderItem.setSpuId(spuInfoVo.getId());
        orderItem.setSpuBrand(spuInfoVo.getBrandId().toString());
        orderItem.setSpuName(spuInfoVo.getSpuName());
        orderItem.setCategoryId(spuInfoVo.getCatalogId());

        //优惠信息【不做】
        //积分信息
        orderItem.setGiftGrowth(cartItem.getPrice().intValue());
        orderItem.setGiftIntegration(cartItem.getPrice().intValue());
        //订单项的价格信息
        orderItem.setCouponAmount(new BigDecimal("0"));
        orderItem.setIntegrationAmount(new BigDecimal("0"));
        orderItem.setPromotionAmount(new BigDecimal("0"));

        //订单项的金额
        BigDecimal origin = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity()));
        //每个订单项 - 优惠的金额
        BigDecimal realPrice = origin.subtract(orderItem.getCouponAmount())
                .subtract(orderItem.getIntegrationAmount())
                .subtract(orderItem.getPromotionAmount());
        orderItem.setRealAmount(realPrice);

        return orderItem;

    }

    /**
     * 构建订单
     * @return
     */
    private OrderEntity createOrder( String orderSn) {
        MemberRespVo loginUser = LoginInterceptor.threadLocal.get();
        OrderEntity order = new OrderEntity();
        //设置订单号
        order.setOrderSn(orderSn);
        order.setMemberId(loginUser.getId());
        //远程调用获取收货地址和费用
        Long addrId = orderSubmitLocal.get().getAddrId();
        FareVo fareVo = wmsFeignService.getFare(addrId).getData(new TypeReference<FareVo>() {
        });
        //获取运费
        BigDecimal fare = fareVo.getFare();
        order.setFreightAmount(fare);
        //获取用户地址
        MemberAddressVo address = fareVo.getMemberAddressVo();
        //设置收货人信息
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverRegion(address.getRegion());
        order.setReceiverDetailAddress(address.getDetailAddress());

        return order;
    }


    @RabbitHandler
    public void receiveMessage1(Message message, OrderReturnReasonEntity reasonEntity, Channel channel) {
        System.out.println("接收到的消息：" + reasonEntity);
        byte[] body = message.getBody();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicAck(deliveryTag,false);
//            channel.basicNack(deliveryTag,false,true);
//            channel.basicReject(deliveryTag,true);
            System.out.println("消息处理完成=》" + reasonEntity.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void receiveMessage1(OrderEntity entity) {
        System.out.println("接收到的消息：" + entity);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}