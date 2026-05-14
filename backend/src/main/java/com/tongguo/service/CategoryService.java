package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Dish;
import com.tongguo.entity.DishCategory;
import com.tongguo.mapper.DishCategoryMapper;
import com.tongguo.mapper.DishMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private DishCategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    public List<DishCategory> list() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<DishCategory>().orderByAsc(DishCategory::getSortOrder)
        );
    }

    public void add(DishCategory category) {
        categoryMapper.insert(category);
    }

    public void update(DishCategory category) {
        categoryMapper.updateById(category);
    }

    public void delete(Integer id) {
        Long count = dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, id)
        );
        if (count > 0) {
            throw new IllegalArgumentException("请先移走该分类下的菜品");
        }
        categoryMapper.deleteById(id);
    }
}
