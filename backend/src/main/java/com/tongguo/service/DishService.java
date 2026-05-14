package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Dish;
import com.tongguo.mapper.DishMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DishService {

    @Autowired
    private DishMapper dishMapper;

    public List<Dish> listAll() {
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getSortOrder)
        );
    }

    public List<Dish> listPublished() {
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .orderByAsc(Dish::getSortOrder)
        );
    }

    public void add(Map<String, Object> params) {
        Dish dish = new Dish();
        dish.setCategoryId((Integer) params.get("categoryId"));
        dish.setName((String) params.get("name"));
        dish.setPrice(yuanToFen(params.get("price")));
        dish.setImage((String) params.get("image"));
        dish.setDescription((String) params.get("description"));
        dish.setStatus(1);
        dish.setStock(params.get("stock") != null ? (Integer) params.get("stock") : 0);
        dish.setSortOrder(params.get("sortOrder") != null ? (Integer) params.get("sortOrder") : 0);
        dishMapper.insert(dish);
    }

    public void update(Map<String, Object> params) {
        Dish dish = dishMapper.selectById((Integer) params.get("id"));
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        if (params.containsKey("categoryId")) dish.setCategoryId((Integer) params.get("categoryId"));
        if (params.containsKey("name")) dish.setName((String) params.get("name"));
        if (params.containsKey("price")) dish.setPrice(yuanToFen(params.get("price")));
        if (params.containsKey("image")) dish.setImage((String) params.get("image"));
        if (params.containsKey("description")) dish.setDescription((String) params.get("description"));
        if (params.containsKey("stock")) dish.setStock((Integer) params.get("stock"));
        if (params.containsKey("sortOrder")) dish.setSortOrder((Integer) params.get("sortOrder"));
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void toggle(Integer id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new IllegalArgumentException("菜品不存在");
        dish.setStatus(dish.getStatus() == 1 ? 0 : 1);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void updateStock(Integer id, Integer stock) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new IllegalArgumentException("菜品不存在");
        dish.setStock(stock);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    private Integer yuanToFen(Object yuan) {
        if (yuan == null) return 0;
        BigDecimal bd = new BigDecimal(yuan.toString());
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.multiply(new BigDecimal(100)).intValue();
    }
}
