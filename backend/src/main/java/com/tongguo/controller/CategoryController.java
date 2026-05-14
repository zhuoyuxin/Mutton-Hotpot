package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.DishCategory;
import com.tongguo.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/m/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<DishCategory>> list() {
        return Result.ok(categoryService.list());
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody DishCategory category) {
        categoryService.add(category);
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody DishCategory category) {
        categoryService.update(category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        try {
            categoryService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
