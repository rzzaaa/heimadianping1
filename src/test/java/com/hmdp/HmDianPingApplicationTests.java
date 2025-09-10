package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private CacheClient cacheClient;
    @Resource
    private IShopService shopService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Test
    public void test(){
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY+1,shop,RedisConstants.CACHE_SHOP_TTL, TimeUnit.SECONDS);
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(500);
    @Test
    public void testWithId() throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(10);
        Runnable runnable = ()->{
            for (int i = 0; i < 100; i++) {
                System.out.println(redisIdWorker.nextId("worker_id"));
            }
            latch.countDown();
        };
        for (int i = 0; i < 10; i++) {
            executorService.submit(runnable);
        }
        latch.await();
    }
}
