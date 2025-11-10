package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private SeckillVoucherServiceImpl seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime().isAfter(now)) {
            return Result.fail("秒杀活动未开始!");
        }
        if (seckillVoucher.getEndTime().isBefore(now)) {
            return Result.fail("秒杀活动已结束!");
        }
        if (seckillVoucher.getStock()<1) {
            return Result.fail("库存不足!");
        }
        Long id = UserHolder.getUser().getId();
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order"+id,stringRedisTemplate);
        boolean isLock = simpleRedisLock.tryLock(1200L);
        if(!isLock){
            Result.fail("失败了");
        }
        try{
            IVoucherOrderService proxy  = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createNewOrder(voucherId);
        }catch (Exception exception){

        }finally {
            simpleRedisLock.unLock();
        }
        return Result.fail("失败");
    }

    @Transactional
    public Result createNewOrder(Long voucherId){
        //一个人只能下一单
        Integer count = query().eq("user_id", UserHolder.getUser().getId())
                .eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("该用户已经购买，无法重复下单");
        }

        boolean flag = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)
                .update();
        if(!flag){
            return Result.fail("更新失败");
        }

        VoucherOrder voucherOrder = new VoucherOrder();

        Long order_id = redisIdWorker.nextId("order");
        voucherOrder.setId(order_id);
        voucherOrder.setVoucherId(voucherId);
        Long id = UserHolder.getUser().getId();
        voucherOrder.setUserId(id);
        save(voucherOrder);
        return Result.ok(order_id);
    }

}
