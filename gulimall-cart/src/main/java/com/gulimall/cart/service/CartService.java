package com.gulimall.cart.service;

import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;

import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 添加某个购物项到购物车
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中的某个购物项
     * @param skuId
     * @return
     */
    CartItemVo getCartItem(String skuId);

    /**
     * 获取整个购物车
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    CartVo getCart() throws ExecutionException, InterruptedException;

    /**
     * 勾选购物项
     * @param skuId
     * @param checked
     */
    void checkItem(Long skuId, Integer checked);

    /**
     * 修改购物项数量
     * @param skuId
     * @param num
     */
    void countItem(Long skuId, Integer num);

    /**
     * 删除购物车中某一项
     * @param skuId
     */
    void deleteItem(String skuId);
}
