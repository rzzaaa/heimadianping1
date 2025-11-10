package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.annotation.Resources;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Resource
    private IUserService userService;

    @Override
    public Result isLikeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();

        String key = BLOG_LIKED_KEY + id;
        Boolean isMerber = stringRedisTemplate.opsForSet().isMember(key, userId.toString());

        if(BooleanUtil.isFalse(isMerber)){
            boolean isSuccess = update().setSql("liked = liked+1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForSet().add(key,userId.toString());
            }
        }
        else{
            boolean isSuccess = update().setSql("liked = liked-1").eq("id", id).update();
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove(key,userId.toString());
            }
        }

        return Result.ok();
    }



    @Override
    public Blog queryById(long id) {
        Blog blog = getById(id);
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        return null;
    }
}
