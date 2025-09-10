package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.Event;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    //设置带有超时时间的cache
    public void set(String key, Object value, Long expireTime, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),expireTime,timeUnit);
    }
    //设置带有逻辑超时时间的cache
    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }
    //缓存穿透
    public <T,ID> T queryWithchuangtou(
            String preTitle, ID id, Class<T> type, Function<ID,T> function,Long expireTime, TimeUnit timeUnit){
        String key = preTitle+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }
        T t = function.apply(id);
        if(t==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        this.set(key,t,expireTime,timeUnit);
        return t;
    }

    private static final ExecutorService CACHE_EXECUTOR = Executors.newFixedThreadPool(10);

    public <T,ID> T queryWithLogical(
            String preTitle,String preTitle2, ID id, Class<T> type, Function<ID,T> function,Long expireTime, TimeUnit timeUnit){
        T t = null;
        String key = preTitle+id;
        String lock = preTitle2+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        LocalDateTime expireTime1 = redisData.getExpireTime();
        JSONObject data = (JSONObject) redisData.getData();
        t = JSONUtil.toBean(data, type);
        //判断是否过期
        if (expireTime1.isAfter(LocalDateTime.now())) {
            return t;
        }
        //过期
        if (tryLock(lock)) {
            //单独开启线程
            CACHE_EXECUTOR.submit(()->{
                try {
                    T value = function.apply(id);
                    this.setWithLogicalExpire(key,value,expireTime,timeUnit);
                }catch (Exception exception){

                }finally {
                    unlock(lock);//草，我是傻逼吗？？？？？
                }
            });
        }
        return t;
    }
    public boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
