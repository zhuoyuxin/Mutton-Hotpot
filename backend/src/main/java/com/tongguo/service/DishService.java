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
        if (params == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        Dish dish = new Dish();
        dish.setCategoryId(toRequiredPositiveInt(params.get("categoryId"), "分类ID"));
        dish.setName(toRequiredText(params.get("name"), "菜品名称"));
        dish.setPrice(yuanToFen(params.get("price"), true));
        dish.setImage(toNullableText(params.get("image")));
        dish.setDescription(toNullableText(params.get("description")));
        dish.setStatus(1);
        dish.setStock(toOptionalNonNegativeInt(params.get("stock"), 0, "库存"));
        dish.setSortOrder(toOptionalNonNegativeInt(params.get("sortOrder"), 0, "排序"));
        dishMapper.insert(dish);
    }

    public void update(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        Integer id = toRequiredPositiveInt(params.get("id"), "菜品ID");
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }

        boolean updated = false;
        if (params.containsKey("categoryId")) {
            dish.setCategoryId(toRequiredPositiveInt(params.get("categoryId"), "分类ID"));
            updated = true;
        }
        if (params.containsKey("name")) {
            dish.setName(toRequiredText(params.get("name"), "菜品名称"));
            updated = true;
        }
        if (params.containsKey("price")) {
            dish.setPrice(yuanToFen(params.get("price"), true));
            updated = true;
        }
        if (params.containsKey("image")) {
            dish.setImage(toNullableText(params.get("image")));
            updated = true;
        }
        if (params.containsKey("description")) {
            dish.setDescription(toNullableText(params.get("description")));
            updated = true;
        }
        if (params.containsKey("stock")) {
            dish.setStock(toOptionalNonNegativeInt(params.get("stock"), null, "库存"));
            updated = true;
        }
        if (params.containsKey("sortOrder")) {
            dish.setSortOrder(toOptionalNonNegativeInt(params.get("sortOrder"), null, "排序"));
            updated = true;
        }
        if (!updated) {
            throw new IllegalArgumentException("请至少提供一个需要更新的字段");
        }

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

    private Integer yuanToFen(Object yuan, boolean required) {
        if (yuan == null) {
            if (required) {
                throw new IllegalArgumentException("价格不能为空");
            }
            return 0;
        }
        BigDecimal bd = new BigDecimal(yuan.toString());
        if (bd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("价格不能小于0");
        }
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.multiply(new BigDecimal(100)).intValue();
    }

    private Integer toRequiredPositiveInt(Object value, String fieldName) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        int intValue = ((Number) value).intValue();
        if (intValue <= 0) {
            throw new IllegalArgumentException(fieldName + "必须大于0");
        }
        return intValue;
    }

    private Integer toOptionalNonNegativeInt(Object value, Integer defaultValue, String fieldName) {
        if (value == null) {
            if (defaultValue == null) {
                throw new IllegalArgumentException(fieldName + "不能为空");
            }
            return defaultValue;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(fieldName + "格式不正确");
        }
        int intValue = ((Number) value).intValue();
        if (intValue < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于0");
        }
        return intValue;
    }

    private String toRequiredText(Object value, String fieldName) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.toString().trim();
    }

    private String toNullableText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
