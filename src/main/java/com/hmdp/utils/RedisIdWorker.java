package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final long START_TIME = 1756684800L;
    public Long nextId(String keyPreFix){
        try{
            LocalDateTime now = LocalDateTime.now();
            long currentTime = now.toEpochSecond(ZoneOffset.UTC);
            long timeKey = currentTime - START_TIME;
            //获取自增序列号
            String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
            long count = stringRedisTemplate.opsForValue().increment("irc:"+keyPreFix+":"+date);
            return timeKey << 32 | count;
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        //获取时间戳
        return 0L;
    }

}
