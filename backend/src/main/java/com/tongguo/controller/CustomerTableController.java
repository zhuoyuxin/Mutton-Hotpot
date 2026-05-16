package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.TableInfo;
import com.tongguo.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerTableController {

    @Autowired
    private TableService tableService;

    @GetMapping("/api/c/table/{id}")
    public Result<TableInfo> detail(@PathVariable Integer id) {
        TableInfo table = tableService.getById(id);
        if (table == null) {
            return Result.error("Table does not exist");
        }
        return Result.ok(table);
    }
}
