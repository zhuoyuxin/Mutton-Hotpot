package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.dto.TableOverviewDTO;
import com.tongguo.entity.TableInfo;
import com.tongguo.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/m/table")
public class TableController {

    @Autowired
    private TableService tableService;

    @GetMapping("/list")
    public Result<List<TableInfo>> list() {
        return Result.ok(tableService.list());
    }

    @GetMapping("/overview")
    public Result<List<TableOverviewDTO>> overview() {
        return Result.ok(tableService.getTableOverview());
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody TableInfo tableInfo) {
        tableService.add(tableInfo);
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody TableInfo tableInfo) {
        try {
            tableService.update(tableInfo);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        try {
            tableService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/qrcode/{id}")
    public Result<Map<String, String>> qrcode(@PathVariable Integer id,
                                               @RequestHeader(value = "Host", defaultValue = "localhost:8080") String host,
                                               @RequestHeader(value = "X-Forwarded-Proto", defaultValue = "http") String proto) {
        try {
            String base64 = tableService.generateQRCode(id, proto + "://" + host);
            Map<String, String> data = new HashMap<>();
            data.put("image", "data:image/png;base64," + base64);
            return Result.ok(data);
        } catch (Exception e) {
            return Result.error("二维码生成失败：" + e.getMessage());
        }
    }
}
