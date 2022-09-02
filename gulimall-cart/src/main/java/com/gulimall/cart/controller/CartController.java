package com.gulimall.cart.controller;

import com.gulimall.cart.intercept.CartInterceptor;
import com.gulimall.cart.service.CartService;
import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;
import com.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;


@Controller
public class CartController {

    @Autowired
    CartService cartService;
    /**
     * 浏览器存放一个cookie（use-key）来标识临时用户，时长为一个月，
     * 设置拦截器，在访问该url时，判断用户是否已经登录
     * @param
     * @return
     */
    @GetMapping("/cart.html")
    public String cartPageList(Model model) throws ExecutionException, InterruptedException {
        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart",cartVo);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId")Long skuId, @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {

         CartItemVo cartItemVo = cartService.addToCart(skuId,num);

        redirectAttributes.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addCartSuccess";
    }

    @GetMapping("/addCartSuccess")
    public String addCartSuccess(@RequestParam("skuId")String skuId, Model model){
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItemVo);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,@RequestParam("checked") Integer checked){
        cartService.checkItem(skuId,checked);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,@RequestParam("num") Integer num){
        cartService.countItem(skuId,num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") String skuId){
        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

}
