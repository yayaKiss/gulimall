package com.gulimall.order.web;

import com.gulimall.order.entity.OrderEntity;
import com.gulimall.order.service.OrderService;
import com.gulimall.order.vo.OrderConfirmVo;
import com.gulimall.order.vo.OrderSubmitVo;
import com.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/rabbit/send")
    @ResponseBody
    public String rabbitSend(){
        OrderEntity order = new OrderEntity();
        order.setOrderSn(UUID.randomUUID().toString());
        order.setCreateTime(new Date());

        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order);
        return "ok";
    }

    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo =  orderService.confirmOrder();
        model.addAttribute("confirmOrderData",orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo, Model model,RedirectAttributes redirectAttributes){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(submitVo);
        if(responseVo.getCode() == 0){
            //δΈεζε
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else{
            String msg = "δΈεε€±θ΄₯";
            if(responseVo.getCode() == 1) msg += "θ?’εδΏ‘ζ―ε·²θΏζ,θ―·ιθ―!";
            else if(responseVo.getCode() == 2) msg += "θ?’εδ»·ζ ΌεηεεοΌθ―·ιζ°ε―Ήζ―εζδΊ€!";
            else msg += "εεεΊε­ιε?ε€±θ΄₯οΌεεεΊε­δΈθΆ³!";
            redirectAttributes.addAttribute("msg",msg);

            return "redirect:http://order.gulimall.com/toTrade";
        }

    }

}
