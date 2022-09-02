package com.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.gulimall.cart.feign.ProductServiceFeign;
import com.gulimall.cart.intercept.CartInterceptor;
import com.gulimall.cart.service.CartService;
import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;
import com.gulimall.cart.vo.SkuInfoVo;
import com.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final static String CART_PREFIX = "gulimall:cart:";

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductServiceFeign productServiceFeign;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo;
        if(StringUtils.isEmpty(res)){
            //购物车中无此商品
            cartItemVo = new CartItemVo();

            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //远程调用商品信息
                SkuInfoVo skuInfo = productServiceFeign.skuInfo(skuId).getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItemVo.setSkuId(skuInfo.getSkuId());
                cartItemVo.setCount(num);
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setCheck(true);
            }, executor);

            CompletableFuture<Void> getSaleAttrValueTask = CompletableFuture.runAsync(() -> {
                //远程调用商品属性信息
                List<String> attrValues = productServiceFeign.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttrValues(attrValues);
            }, executor);

            //阻塞等待，等待cartItem属性查找完全并封装好
            CompletableFuture.allOf(getSkuInfoTask,getSaleAttrValueTask).get();

            cartOps.put(skuId.toString(),JSON.toJSONString(cartItemVo));

        }else {
            //存在此商品，修改num即可
            cartItemVo = JSON.parseObject(res, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);

            cartOps.put(skuId.toString(),JSON.toJSONString(cartItemVo));
        }

        return cartItemVo;
    }

    @Override
    public CartItemVo getCartItem(String skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s  = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(s,CartItemVo.class);
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        CartVo cart = new CartVo();
        if(userInfoTo.getUserId() != null){
            //用户登录
            String useKey  = CART_PREFIX + userInfoTo.getUserId();
            //临时登录的购物车内容
            List<CartItemVo> tempCartItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
            //临时购物车有，需要进行合并,添加购物车中做好相同内容只添加数量
            if(tempCartItems != null && tempCartItems.size() > 0){
                for (CartItemVo cartItem : tempCartItems) {
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
                //合并后，删除临时购物车内容(删除键)
                stringRedisTemplate.delete(CART_PREFIX + userInfoTo.getUserKey());
            }
            //合并完成后，再次查询进行设置
            List<CartItemVo> cartItems = getCartItems(useKey);
            cart.setItems(cartItems);
        }else{
            //临时用户
            String userKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> cartItems = getCartItems(userKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId.toString());
        cartItem.setCheck(checked == 1);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

    }

    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId.toString());
        cartItem.setCount(num);
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(String skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    private List<CartItemVo> getCartItems(String cartKey ) {
        BoundHashOperations<String, Object, Object> boundHashOps =
                stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = boundHashOps.values();
        if(values.size() > 0 && values != null){
            List<CartItemVo> collect = values.stream().map(value -> {
                CartItemVo cartItem = JSON.parseObject((String) value, CartItemVo.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * redis中存放的购物车位置
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if(userInfoTo.getUserId() != null){
            //用户登录
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else{
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOperation = stringRedisTemplate.boundHashOps(cartKey);

        return hashOperation;
    }
}
