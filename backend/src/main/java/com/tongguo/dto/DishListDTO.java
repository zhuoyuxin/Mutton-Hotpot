package com.tongguo.dto;

import com.tongguo.entity.Dish;
import com.tongguo.entity.DishCategory;
import lombok.Data;

import java.util.List;

@Data
public class DishListDTO {
    private List<DishCategory> categories;
    private List<Dish> dishes;
}
