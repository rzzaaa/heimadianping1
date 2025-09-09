package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private IShopTypeService typeService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryShoptype() {
        List<ShopType> typeList = new ArrayList<ShopType>();
        List<String> shopType = stringRedisTemplate.opsForList().range("shopType", 0, -1);
        if(shopType.size()!=0){
            for(String s:shopType){
                typeList.add(JSONUtil.toBean(s, ShopType.class));
            }
            System.out.println(typeList);
            return Result.ok(typeList);
        }
        typeList = typeService.query().orderByAsc("sort").list();
        for(ShopType shopType1:typeList){
            stringRedisTemplate.opsForList().rightPush("shopType",JSONUtil.toJsonStr(shopType1));
        }
        System.out.println(typeList);
        return Result.ok(typeList);
    }
}
