package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.Dish;
import com.tongguo.service.CategoryService;
import com.tongguo.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Result<Void> add(@RequestBody Map<String, Object> params) {
        dishService.add(params);
        return Result.ok();
    }

    @PutMapping("/api/m/dish/update")
    public Result<Void> update(@RequestBody Map<String, Object> params) {
        dishService.update(params);
        return Result.ok();
    }

    @PutMapping("/api/m/dish/toggle/{id}")
    public Result<Void> toggle(@PathVariable Integer id) {
        dishService.toggle(id);
        return Result.ok();
    }

    @PutMapping("/api/m/dish/stock")
    public Result<Void> updateStock(@RequestBody Map<String, Object> params) {
        dishService.updateStock((Integer) params.get("id"), (Integer) params.get("stock"));
        return Result.ok();
    }

    @GetMapping("/api/c/dish/list")
    public Result<Map<String, Object>> customerList() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryService.list());
        data.put("dishes", dishService.listPublished());
        return Result.ok(data);
    }
}
