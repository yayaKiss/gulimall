package com.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.gulimall.common.to.SkuHasStockVo;
import com.gulimall.common.to.mq.OrderTo;
import com.gulimall.common.to.mq.StockLockedTo;
import com.gulimall.common.to.mq.StockTaskDetailTo;
import com.gulimall.common.utils.R;
import com.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.gulimall.ware.entity.WareOrderTaskEntity;
import com.gulimall.ware.ex.SkuStockException;
import com.gulimall.ware.feign.OrderServiceFeign;
import com.gulimall.ware.feign.ProductFeign;
import com.gulimall.ware.service.WareOrderTaskDetailService;
import com.gulimall.ware.service.WareOrderTaskService;
import com.gulimall.ware.vo.OrderItemVo;
import com.gulimall.ware.vo.OrderVo;
import com.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gulimall.common.utils.Query;
import com.gulimall.common.utils.PageUtils;


import com.gulimall.ware.dao.WareSkuDao;
import com.gulimall.ware.entity.WareSkuEntity;
import com.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeign productFeign;
    @Autowired
    private WareSkuDao wareSkuDao;
    @Autowired
    OrderServiceFeign orderServiceFeign;
    @Autowired
    private WareOrderTaskService orderTaskService;
    @Autowired
    WareOrderTaskDetailService orderTaskDetailService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     *  被动接触库存
     * @param stockLockedTo
     */
    @Override
    public void unLockStock(StockLockedTo stockLockedTo) {
        //消息队列中
        Long id = stockLockedTo.getId();  //库存工作单id
        StockTaskDetailTo detail = stockLockedTo.getDetail();  //库存工作单详情 ===》（skuId,wareId,num等等）
        Long detailId = detail.getId();
        /**
         * 查询一下detail信息
         *      1、如果没有，说明之前锁库存时失败，进行了本地回滚
         *      2、如果有，不一定进行解锁
         *          订单状态：已取消：解锁库存
         *          已支付：不能解锁库存
         */
        WareOrderTaskDetailEntity detailEntity = orderTaskDetailService.getById(detailId);
        //库存锁定了
        if (detailEntity != null) {
            //解锁条件 ：
            /**
             * 远程调用获取订单信息：
             *      1、若没有该订单，说明订单后续服务出现异常，需要解锁
             *      2、有订单，但被取消了，一样需要解锁
             */
            //获取工作单实体 === 》 获取订单号
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();//订单号
            R r = orderServiceFeign.getOrderByOrderSn(orderSn);
            if (r.getCode() == 0) {
                //返回订单数据
                OrderVo orderVo = r.getData("data", new TypeReference<OrderVo>() {
                });
                //订单数据为空（没有该订单） 或 订单状态为已取消   ====》 解锁
                if (orderVo == null || orderVo.getStatus() == 4) {
                    //查看状态（避免主动解锁了），只有解锁状态为1（已锁定），才进行解锁
                    if(detailEntity.getLockStatus() == 1){
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(),detail.getId());
                    }
                }
            }
            else{
                //消息拒绝以后重新放在队列里面，让别人继续消费解锁
                //远程调用服务失败
                throw new RuntimeException("远程调用服务失败");
            }
        }
    }

    @Transactional
    public void unLockStock(Long skuId, Long wareId, Integer skuNum, Long taskDetailId) {
        //解锁完成
        wareSkuDao.unLockStock(skuId,wareId,skuNum);
        //更新工作单的状态
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        //变为已解锁
        taskDetailEntity.setLockStatus(2);
        orderTaskDetailService.updateById(taskDetailEntity);
    }

    /**
     * 订单取消，主动解除库存
     * @param orderTo
     */
    @Override
    public void unLockStock(OrderTo orderTo) {
        //获取订单号
        String orderSn = orderTo.getOrderSn();
        //通过订单号获取订单任务单
        WareOrderTaskEntity taskEntity = orderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>()
                .eq("order_sn", orderSn));
        Long id = taskEntity.getId();
        //获取任务单的详情（task_id  和 lock_status == 》 由于网络导致订单延迟,可能订单被动解锁了）
        List<WareOrderTaskDetailEntity> detailEntities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id).eq("lock_status",1));

        for (WareOrderTaskDetailEntity detail : detailEntities) {
            unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(), detail.getId());
        }

    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        /**
         * skuId:
         * wareId:
         */
        String key = (String) params.get("key");
        String skuId = (String) params.get("skuId");
        String wareId = (String) params.get("wareId");
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id",wareId);
        }
        if(!StringUtils.isEmpty(key)){
                wrapper.and(w -> {
                    w.eq("id",key).or().like("sku_name",key);
                });
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>()
                .eq("sku_id", skuId).eq("ware_id", wareId));
        if(wareSkuEntities == null || wareSkuEntities.size() == 0){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //远程调用商品服务获取商品名字
            try{
                R info = productFeign.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if(info.getCode() == 0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){

            }
            this.baseMapper.insert(wareSkuEntity);
        }else{
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> vos = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            skuHasStockVo.setSkuId(skuId);
            Long count = baseMapper.selectSkuHasStock(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0);

            return skuHasStockVo;
        }).collect(Collectors.toList());

        return vos;
    }


    @Transactional
    @Override
    public boolean orderLockStock(WareSkuLockVo skuLockVo) {
        /**
         * 保存库存工作单
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(skuLockVo.getOrderSn());
        orderTaskService.save(taskEntity);

        //locks ==> List <orderItem(skuId,count)>
        List<OrderItemVo> locks = skuLockVo.getLocks();
        List<skuIdAndWaresHasStock> waresHasStock = locks.stream().map(lock -> {
            skuIdAndWaresHasStock skuIdAndWares = new skuIdAndWaresHasStock();
            skuIdAndWares.setSkuId(lock.getSkuId());
            skuIdAndWares.setCount(lock.getCount());
            //查询所有有库存的仓库id
            List<Long> wareIds = wareSkuDao.getWaresBySkuId(lock.getSkuId());
            skuIdAndWares.setWareIds(wareIds);

            return skuIdAndWares;
        }).collect(Collectors.toList());

        //锁定库存
        for(skuIdAndWaresHasStock wareHasStock : waresHasStock){
            boolean lockSuccess = true;
            Long skuId = wareHasStock.getSkuId();
            List<Long> wareIds = wareHasStock.getWareIds();
            //没有当前商品的仓库
            if(wareIds == null || wareIds.size() <= 0){
                throw new SkuStockException(skuId);
            }
            //遍历仓库寻找库存足够的
            for(Long wareId : wareIds){
                //锁库存：skuId  ————》仓库，锁几件
                Long count = wareSkuDao.lockSkuStock(skuId,wareId,wareHasStock.getCount());
                if(count == 0){
                    //当前仓库锁定失败,继续下一个仓库
                    lockSuccess = false;
                }else{
                    lockSuccess = true;
                    //保存订单任务详情
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, null, wareHasStock.getCount()
                            , taskEntity.getId(), wareId, 1);
                    orderTaskDetailService.save(taskDetailEntity);
                    // 告诉MQ库存锁定成功(将locked送到延迟队列中，包括工作单id，工作单详情内容)
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockTaskDetailTo detailTo = new StockTaskDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity,detailTo);
                    lockedTo.setDetail(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);

                    break;
                }
            }
            if(!lockSuccess){
                throw new SkuStockException(skuId);
            }

        }

        return true;
    }

    //每一个skuId对应的所有仓库（有库存）
    @Data
    class skuIdAndWaresHasStock{
        private Long skuId;
        private Integer count;
        private List<Long> wareIds;
    }

}