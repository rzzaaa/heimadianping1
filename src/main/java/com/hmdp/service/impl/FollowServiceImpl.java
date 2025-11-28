package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long userId, boolean isFollow) {
        Long userId1 = UserHolder.getUser().getId();
        if(isFollow){
            //进行关注
            Follow follow = new Follow();
            follow.setUserId(userId1);
            follow.setFollowUserId(userId);
            follow.setCreateTime(LocalDateTime.now());
            this.save(follow);
        }
        else{
            QueryWrapper<Follow> queryWrapper = new QueryWrapper<Follow>()
                    .eq("user_id",userId)
                    .eq("follow_user_id",userId);
            boolean isRemove = this.remove(queryWrapper);
            System.out.println(isRemove);
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long userId) {
        Long userId1 = UserHolder.getUser().getId();
        int count = this.count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId,userId1)
                .eq(Follow::getFollowUserId,userId));
        return Result.ok(count>0);
    }
}
