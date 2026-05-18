package com.tongguo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CheckoutFeeSchemaMigrationService implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(session_checkout)");
        if (columns == null || columns.isEmpty()) {
            return;
        }

        boolean addedDishAmount = addColumnIfMissing(
                columns,
                "dish_amount",
                "ALTER TABLE session_checkout ADD COLUMN dish_amount INTEGER DEFAULT 0"
        );
        addColumnIfMissing(columns, "self_service_count", "ALTER TABLE session_checkout ADD COLUMN self_service_count INTEGER DEFAULT 0");
        addColumnIfMissing(columns, "self_service_unit_price", "ALTER TABLE session_checkout ADD COLUMN self_service_unit_price INTEGER DEFAULT 0");
        addColumnIfMissing(columns, "self_service_amount", "ALTER TABLE session_checkout ADD COLUMN self_service_amount INTEGER DEFAULT 0");
        addColumnIfMissing(columns, "tableware_count", "ALTER TABLE session_checkout ADD COLUMN tableware_count INTEGER DEFAULT 0");
        addColumnIfMissing(columns, "tableware_unit_price", "ALTER TABLE session_checkout ADD COLUMN tableware_unit_price INTEGER DEFAULT 0");
        addColumnIfMissing(columns, "tableware_amount", "ALTER TABLE session_checkout ADD COLUMN tableware_amount INTEGER DEFAULT 0");

        if (addedDishAmount) {
            jdbcTemplate.execute("UPDATE session_checkout SET dish_amount = total_amount WHERE dish_amount IS NULL OR dish_amount = 0");
        } else {
            jdbcTemplate.execute("UPDATE session_checkout SET dish_amount = total_amount WHERE dish_amount IS NULL");
        }
        jdbcTemplate.execute("UPDATE session_checkout SET self_service_count = 0 WHERE self_service_count IS NULL");
        jdbcTemplate.execute("UPDATE session_checkout SET self_service_unit_price = 0 WHERE self_service_unit_price IS NULL");
        jdbcTemplate.execute("UPDATE session_checkout SET self_service_amount = 0 WHERE self_service_amount IS NULL");
        jdbcTemplate.execute("UPDATE session_checkout SET tableware_count = 0 WHERE tableware_count IS NULL");
        jdbcTemplate.execute("UPDATE session_checkout SET tableware_unit_price = 0 WHERE tableware_unit_price IS NULL");
        jdbcTemplate.execute("UPDATE session_checkout SET tableware_amount = 0 WHERE tableware_amount IS NULL");
    }

    private boolean addColumnIfMissing(List<Map<String, Object>> columns, String columnName, String sql) {
        if (hasColumn(columns, columnName)) {
            return false;
        }
        jdbcTemplate.execute(sql);
        return true;
    }

    private boolean hasColumn(List<Map<String, Object>> columns, String columnName) {
        for (Map<String, Object> column : columns) {
            Object name = column.get("name");
            if (name != null && columnName.equalsIgnoreCase(String.valueOf(name))) {
                return true;
            }
        }
        return false;
    }
}
