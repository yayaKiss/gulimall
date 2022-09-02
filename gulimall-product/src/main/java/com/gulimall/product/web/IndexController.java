package com.gulimall.product.web;

import com.gulimall.product.entity.CategoryEntity;
import com.gulimall.product.service.CategoryService;
import com.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController{

    @Autowired
    private CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        //查出所有一级分类
        List<CategoryEntity> list = categoryService.getFirstCategory();
        model.addAttribute("categories",list);
        return "index";
    }

    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();

        return catalogJson;

    }

    @GetMapping("/hello")
    @ResponseBody
    public String hello(){
        return "hello";
    }

    /**
     * 读写锁测试
     */
    @GetMapping("/write")
    @ResponseBody
    public String write(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.writeLock();
        String s = "";
        //上锁
        rLock.lock();
        try{
            //进行写业务
            System.out.println("写锁加锁成功....." + Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(15000);
            stringRedisTemplate.opsForValue().set("writeValue",s);
        } catch (Exception e){
             e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("写锁释放" + Thread.currentThread().getId());
        }
        return  s;

    }


    @GetMapping("/read")
    @ResponseBody
    public String read(){
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = readWriteLock.readLock();
        String s = "";
        //上锁
        rLock.lock();
        try{
            //进行读业务
            System.out.println("读锁加锁成功....." + Thread.currentThread().getId());
            s = stringRedisTemplate.opsForValue().get("writeValue");
            Thread.sleep(15000);
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放" + Thread.currentThread().getId());
        }
        return  s;

    }

}
