package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.DishListDTO;
import com.tongguo.dto.request.DishSaveRequest;
import com.tongguo.dto.request.DishStockUpdateRequest;
import com.tongguo.entity.Dish;
import com.tongguo.service.CategoryService;
import com.tongguo.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/api/m/dish/list")
    public Result<List<Dish>> merchantList() {
        return Result.ok(dishService.listAll());
    }

    @PostMapping("/api/m/dish/add")
    public Result<Void> add(@RequestBody DishSaveRequest request) {
        try {
            dishService.add(request);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/dish/update")
    public Result<Void> update(@RequestBody DishSaveRequest request) {
        try {
            dishService.update(request);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/dish/toggle/{id}")
    public Result<Void> toggle(@PathVariable Integer id) {
        try {
            dishService.toggle(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/api/m/dish/stock")
    public Result<Void> updateStock(@RequestBody DishStockUpdateRequest request) {
        try {
            dishService.updateStock(request == null ? null : request.getId(), request == null ? null : request.getStock());
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/c/dish/list")
    public Result<DishListDTO> customerList() {
        DishListDTO data = new DishListDTO();
        data.setCategories(categoryService.list());
        data.setDishes(dishService.listPublished());
        return Result.ok(data);
    }
}
