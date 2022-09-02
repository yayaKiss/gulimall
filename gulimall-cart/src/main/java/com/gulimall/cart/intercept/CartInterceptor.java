package com.gulimall.cart.intercept;

import com.gulimall.cart.vo.UserInfoTo;
import com.gulimall.common.constant.AuthServerConstant;
import com.gulimall.common.constant.CartConstant;
import com.gulimall.common.vo.MemberRespVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法前，判断用户登录状态
 */
@Controller
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();
    /**
     * 业务方法执行之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        MemberRespVo loginUser = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(loginUser != null){
            //用户已登录
            userInfoTo.setUserId(loginUser.getId());
        }
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            for (Cookie cookie : cookies) {
                //cookie :  user-key
                String name = cookie.getName();
                if(CartConstant.TEMP_USER_COOKIE_NAME.equals(name)){
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setIsTempUser(true);
                }
            }
        }

        //如果用户没登录，分配一个临时用户
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            String s = UUID.randomUUID().toString();
            userInfoTo.setUserKey(s);
        }
        //放行之前，把数据放到ThreadLocal中
        threadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 业务执行之后,设置cookie：user-key ，时间为一个月
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.getIsTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
