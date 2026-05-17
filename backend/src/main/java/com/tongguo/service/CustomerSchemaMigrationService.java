package com.tongguo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomerSchemaMigrationService implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateCustomerTableIfNeeded();
    }

    private void migrateCustomerTableIfNeeded() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(customer)");
        if (columns == null || columns.isEmpty()) {
            return;
        }

        boolean hasOpenId = false;
        boolean phoneNotNull = false;
        for (Map<String, Object> column : columns) {
            String name = column.get("name") == null ? null : String.valueOf(column.get("name"));
            if ("openid".equalsIgnoreCase(name)) {
                hasOpenId = true;
            }
            if ("phone".equalsIgnoreCase(name)) {
                Object notNullValue = column.get("notnull");
                phoneNotNull = notNullValue != null && Integer.parseInt(String.valueOf(notNullValue)) == 1;
            }
        }

        if (hasOpenId && !phoneNotNull) {
            ensureCustomerIndexes();
            return;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS customer_migrating");
        jdbcTemplate.execute("CREATE TABLE customer_migrating (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "phone TEXT, " +
                "openid TEXT, " +
                "name TEXT, " +
                "points INTEGER DEFAULT 0, " +
                "total_spent INTEGER DEFAULT 0, " +
                "create_time DATETIME DEFAULT (datetime('now','localtime')), " +
                "update_time DATETIME DEFAULT (datetime('now','localtime'))" +
                ")");

        if (hasOpenId) {
            jdbcTemplate.execute("INSERT INTO customer_migrating " +
                    "(id, phone, openid, name, points, total_spent, create_time, update_time) " +
                    "SELECT id, phone, openid, name, points, total_spent, create_time, update_time FROM customer");
        } else {
            jdbcTemplate.execute("INSERT INTO customer_migrating " +
                    "(id, phone, name, points, total_spent, create_time, update_time) " +
                    "SELECT id, phone, name, points, total_spent, create_time, update_time FROM customer");
        }

        jdbcTemplate.execute("DROP TABLE customer");
        jdbcTemplate.execute("ALTER TABLE customer_migrating RENAME TO customer");
        ensureCustomerIndexes();
    }

    private void ensureCustomerIndexes() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uniq_customer_phone " +
                "ON customer(phone) WHERE phone IS NOT NULL");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uniq_customer_openid " +
                "ON customer(openid) WHERE openid IS NOT NULL");
    }
}
