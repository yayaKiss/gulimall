package com.gulimall.order.interceptor;

import com.gulimall.common.constant.AuthServerConstant;
import com.gulimall.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //feign服务之间的调用，不需要判断是否登录，进行路径匹配
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/getOrder/**", uri);
        if(match){
            return true;
        }

        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute != null){
            threadLocal.set(attribute);
            return true;
        }else{
            request.getSession().setAttribute("msg","请先登录!");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
