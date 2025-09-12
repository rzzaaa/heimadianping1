package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String preTitle = "lock:";
    @Override
    public boolean tryLock(Long time) {
        long value = Thread.currentThread().getId();
        Boolean IsLock = stringRedisTemplate.opsForValue().setIfAbsent(preTitle + name, value + "", time, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(IsLock);
    }

    @Override
    public void unLock() {
        stringRedisTemplate.delete(preTitle + name);
    }
}
