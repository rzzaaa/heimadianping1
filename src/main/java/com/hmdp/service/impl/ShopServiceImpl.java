package com.hmdp.service.impl;

import ch.qos.logback.core.util.TimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    public IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
        //缓存
        Shop shop;
        //缓存穿透
        //shop = queryWithchuangtou(id);
        //缓存击穿
        //shop = queryWithMeyux(id);
        //逻辑超时
        //shop = queryWithLogical(id);
        //shop = cacheClient.queryWithchuangtou(CACHE_SHOP_KEY,id,Shop.class,(id2)->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        shop =cacheClient.queryWithLogical(CACHE_SHOP_KEY,LOCK_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.SECONDS);
        if(shop==null){
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }
//    private static final ExecutorService CACHE_EXECUTOR = Executors.newFixedThreadPool(10);
//    public Shop queryWithLogical(Long id){
//        Shop shop = null;
//        String key = CACHE_SHOP_KEY+id;
//        String lock = LOCK_SHOP_KEY+id;
//        String s = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(s)) {
//            return null;
//        }
//        RedisData redisData = JSONUtil.toBean(s, RedisData.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        JSONObject data = (JSONObject) redisData.getData();
//        shop = JSONUtil.toBean(data, Shop.class);
//        //判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;
//        }
//        //过期
//        if (tryLock(lock)) {
//            //单独开启线程
//            CACHE_EXECUTOR.submit(()->{
//                try {
//                    saveRedisToData(id,CACHE_SHOP_TTL);
//                }catch (Exception exception){
//
//                }finally {
//                    unlock(key);
//                }
//            });
//        }
//        return shop;
//    }
//    public Shop queryWithMeyux(Long id){
//        Shop shop;
//        String key = CACHE_SHOP_KEY+id;
//        String lock = LOCK_SHOP_KEY+id;
//        String s = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(s)) {
//            return JSONUtil.toBean(s,Shop.class);
//        }
//        try {
//            boolean isLock = tryLock(lock);
//            if (!isLock) {
//                Thread.sleep(50);
//                return queryWithMeyux(id);
//            }
//            shop = getById(id);
//            if(shop==null){
//                return null;
//            }
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        }catch (Exception e){
//            throw new RuntimeException(e.getMessage());
//        }finally {
//            unlock(lock);
//        }
//        return shop;
//    }

//    public Shop queryWithchuangtou(Long id){
//        String key = CACHE_SHOP_KEY+id;
//        String s = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(s)) {
//            return JSONUtil.toBean(s,Shop.class);
//        }
//        if(s!=null){
//            return null;
//        }
//        Shop shop = getById(id);
//        if(shop==null){
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }

//    public boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    public void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }
    //重新建立缓存
//    public void saveRedisToData(Long id, Long expireSecond) throws InterruptedException {
//        Shop shop = getById(id);
//        Thread.sleep(500);
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSecond));
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
//    }

    @Override
    public Result upadte(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        //先更新数据库
        shopService.updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
