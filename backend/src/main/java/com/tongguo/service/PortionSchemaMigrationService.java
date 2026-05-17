package com.tongguo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PortionSchemaMigrationService implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateDishTable();
        migrateOrderItemTable();
    }

    private void migrateDishTable() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(dish)");
        if (columns == null || columns.isEmpty()) {
            return;
        }

        if (!hasColumn(columns, "allow_half_portion")) {
            jdbcTemplate.execute("ALTER TABLE dish ADD COLUMN allow_half_portion INTEGER DEFAULT 0");
        }
        if (!hasColumn(columns, "half_price")) {
            jdbcTemplate.execute("ALTER TABLE dish ADD COLUMN half_price INTEGER");
        }

        jdbcTemplate.execute("UPDATE dish SET allow_half_portion = 0 WHERE allow_half_portion IS NULL");
    }

    private void migrateOrderItemTable() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(order_item)");
        if (columns == null || columns.isEmpty()) {
            return;
        }

        if (!hasColumn(columns, "portion_type")) {
            jdbcTemplate.execute("ALTER TABLE order_item ADD COLUMN portion_type TEXT DEFAULT 'FULL'");
        }

        jdbcTemplate.execute("UPDATE order_item SET portion_type = 'FULL' " +
                "WHERE portion_type IS NULL OR TRIM(portion_type) = ''");
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
