package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.dto.request.DishSaveRequest;
import com.tongguo.entity.Dish;
import com.tongguo.mapper.DishMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

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

    public void add(DishSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        Dish dish = new Dish();
        dish.setCategoryId(toRequiredPositiveInt(request.getCategoryId(), "分类ID"));
        dish.setName(toRequiredText(request.getName(), "菜品名称"));
        dish.setPrice(yuanToFen(request.getPrice(), true));
        dish.setImage(toNullableText(request.getImage()));
        dish.setDescription(toNullableText(request.getDescription()));
        dish.setStatus(1);
        dish.setStock(toOptionalNonNegativeInt(request.getStock(), 0, "库存"));
        dish.setSortOrder(toOptionalNonNegativeInt(request.getSortOrder(), 0, "排序"));
        dishMapper.insert(dish);
    }

    public void update(DishSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        Integer id = toRequiredPositiveInt(request.getId(), "菜品ID");
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }

        dish.setCategoryId(toRequiredPositiveInt(request.getCategoryId(), "分类ID"));
        dish.setName(toRequiredText(request.getName(), "菜品名称"));
        dish.setPrice(yuanToFen(request.getPrice(), true));
        dish.setImage(toNullableText(request.getImage()));
        dish.setDescription(toNullableText(request.getDescription()));
        dish.setStock(toOptionalNonNegativeInt(request.getStock(), 0, "库存"));
        dish.setSortOrder(toOptionalNonNegativeInt(request.getSortOrder(), 0, "排序"));
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void toggle(Integer id) {
        Integer dishId = toRequiredPositiveInt(id, "菜品ID");
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        dish.setStatus(dish.getStatus() == 1 ? 0 : 1);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void updateStock(Integer id, Integer stock) {
        Integer dishId = toRequiredPositiveInt(id, "菜品ID");
        Integer newStock = toOptionalNonNegativeInt(stock, null, "库存");
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        dish.setStock(newStock);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    private Integer yuanToFen(BigDecimal yuan, boolean required) {
        if (yuan == null) {
            if (required) {
                throw new IllegalArgumentException("价格不能为空");
            }
            return 0;
        }
        if (yuan.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("价格不能小于0");
        }
        BigDecimal amount = yuan.setScale(2, RoundingMode.HALF_UP);
        return amount.multiply(new BigDecimal(100)).intValue();
    }

    private Integer toRequiredPositiveInt(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "必须大于0");
        }
        return value;
    }

    private Integer toOptionalNonNegativeInt(Integer value, Integer defaultValue, String fieldName) {
        if (value == null) {
            if (defaultValue == null) {
                throw new IllegalArgumentException(fieldName + "不能为空");
            }
            return defaultValue;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于0");
        }
        return value;
    }

    private String toRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String toNullableText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
