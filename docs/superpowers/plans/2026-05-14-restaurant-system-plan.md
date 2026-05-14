# 铜锅涮肉餐饮管理系统 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现铜锅涮肉小餐饮管理系统，包含商户端（菜品/桌台/订单/结账管理）和用户端（扫码点餐/积分/消费记录）。

**Architecture:** Spring Boot 2.x 单体应用，内嵌 SQLite 数据库，前端 Vue 3 SPA 打包进 static 目录。商户端 `/m/*` 和用户端 `/c/*` 通过路由隔离，后端通过 `/api/m/**` vs `/api/c/**` 路径实现鉴权隔离。

**Tech Stack:** Java 8 + Spring Boot 2.x + MyBatis-Plus + SQLite（后端）；Vue 3 + Element Plus + Vite + Axios（前端）

**设计文档：** `docs/superpowers/specs/2026-05-14-restaurant-system-design.md`

---

## 文件结构总览

### 后端文件

```
backend/
├── pom.xml
├── src/main/java/com/tongguo/
│   ├── TongguoApplication.java
│   ├── config/
│   │   ├── WebMvcConfig.java            # CORS + uploads 静态资源映射
│   │   └── AuthInterceptor.java         # 商户端鉴权拦截器
│   ├── entity/
│   │   ├── MerchantUser.java
│   │   ├── DishCategory.java
│   │   ├── Dish.java
│   │   ├── TableInfo.java
│   │   ├── DiningSession.java
│   │   ├── SessionCheckout.java
│   │   ├── Customer.java
│   │   ├── Orders.java
│   │   ├── OrderItem.java
│   │   └── PointsRecord.java
│   ├── mapper/
│   │   ├── MerchantUserMapper.java
│   │   ├── DishCategoryMapper.java
│   │   ├── DishMapper.java
│   │   ├── TableInfoMapper.java
│   │   ├── DiningSessionMapper.java
│   │   ├── SessionCheckoutMapper.java
│   │   ├── CustomerMapper.java
│   │   ├── OrdersMapper.java
│   │   ├── OrderItemMapper.java
│   │   └── PointsRecordMapper.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── CategoryService.java
│   │   ├── DishService.java
│   │   ├── TableService.java
│   │   ├── SessionService.java
│   │   ├── OrderService.java
│   │   ├── CustomerService.java
│   │   ├── DashboardService.java
│   │   └── UploadService.java
│   └── controller/
│       ├── AuthController.java          # /api/m/auth/*
│       ├── CategoryController.java      # /api/m/category/*
│       ├── DishController.java          # /api/m/dish/* + /api/c/dish/list
│       ├── TableController.java         # /api/m/table/*
│       ├── OrderController.java         # /api/m/order/* + /api/c/order/*
│       ├── SessionController.java       # /api/m/session/*
│       ├── CustomerController.java      # /api/m/customer/* + /api/c/customer/*
│       ├── MerchantController.java      # /api/m/dashboard
│       └── UploadController.java        # /api/m/upload/*
├── src/main/resources/
│   ├── application.yml
│   ├── schema.sql
│   └── data.sql                         # 初始化数据（admin 账号）
└── src/test/java/com/tongguo/
    └── TongguoApplicationTests.java
```

### 前端文件

```
frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/
│   │   └── index.js
│   ├── api/
│   │   ├── request.js                   # axios 实例 + 拦截器
│   │   ├── auth.js
│   │   ├── dish.js
│   │   ├── category.js
│   │   ├── table.js
│   │   ├── order.js
│   │   ├── session.js
│   │   ├── customer.js
│   │   ├── dashboard.js
│   │   └── upload.js
│   ├── views/
│   │   ├── merchant/
│   │   │   ├── Login.vue
│   │   │   ├── Layout.vue
│   │   │   ├── Dashboard.vue
│   │   │   ├── Dishes.vue
│   │   │   ├── Tables.vue
│   │   │   ├── Orders.vue
│   │   │   ├── Sessions.vue
│   │   │   ├── Customers.vue
│   │   │   └── ManualOrder.vue
│   │   └── customer/
│   │       ├── Login.vue
│   │       ├── Order.vue
│   │       ├── Status.vue
│   │       └── Mine.vue
│   └── components/
│       └── DishFormDialog.vue           # 菜品编辑弹窗（商户端复用）
```

---

## Phase 1: 后端基础框架

### Task 1: Spring Boot 项目骨架 + SQLite 配置

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/tongguo/TongguoApplication.java`
- Create: `backend/src/main/java/com/tongguo/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 backend 目录并初始化 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.tongguo</groupId>
    <artifactId>tongguo-restaurant</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.3.2</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.5</version>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.45.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>tongguo</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:${user.dir}/tongguo.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

logging:
  level:
    com.tongguo: debug
```

- [ ] **Step 3: 创建主启动类**

```java
package com.tongguo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tongguo.mapper")
public class TongguoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TongguoApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 WebMvcConfig（CORS + uploads 静态映射）**

```java
package com.tongguo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/m/**")
                .excludePathPatterns("/api/m/auth/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
    }
}
```

- [ ] **Step 5: 创建 AuthInterceptor 占位（空实现，Task 3 完善）**

```java
package com.tongguo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("merchantUser") == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "请先登录");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 6: 运行 `mvn compile` 验证项目骨架**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add backend/
git commit -m "feat: 初始化 Spring Boot 项目骨架 + SQLite + MyBatis-Plus 配置"
```

---

### Task 2: 数据库 Schema + 全部 Entity + Mapper

**Files:**
- Create: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/resources/data.sql`
- Create: `backend/src/main/java/com/tongguo/entity/` 下全部 10 个实体
- Create: `backend/src/main/java/com/tongguo/mapper/` 下全部 10 个 Mapper

- [ ] **Step 1: 创建 schema.sql**

```sql
CREATE TABLE IF NOT EXISTS merchant_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    must_change_password INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dish_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dish (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER,
    name TEXT NOT NULL,
    price INTEGER NOT NULL,
    image TEXT,
    description TEXT,
    status INTEGER DEFAULT 1,
    stock INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS table_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    area TEXT DEFAULT '大厅',
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS dining_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_id INTEGER,
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    end_time DATETIME
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_active_session_per_table
    ON dining_session(table_id) WHERE status = 0 AND table_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS session_checkout (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER UNIQUE NOT NULL,
    total_amount INTEGER DEFAULT 0,
    actual_paid INTEGER DEFAULT 0,
    discount_amount INTEGER DEFAULT 0,
    customer_id INTEGER,
    points_earned INTEGER DEFAULT 0,
    checkout_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    phone TEXT UNIQUE NOT NULL,
    name TEXT,
    points INTEGER DEFAULT 0,
    total_spent INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT UNIQUE NOT NULL,
    session_id INTEGER NOT NULL,
    table_id INTEGER,
    customer_id INTEGER,
    total_amount INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,
    remark TEXT,
    create_time DATETIME DEFAULT (datetime('now','localtime')),
    update_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS order_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    dish_id INTEGER NOT NULL,
    dish_name TEXT NOT NULL,
    dish_price INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    status INTEGER DEFAULT 0,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS points_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    checkout_id INTEGER,
    points INTEGER NOT NULL,
    type INTEGER DEFAULT 0,
    remark TEXT,
    create_time DATETIME DEFAULT (datetime('now','localtime'))
);
```

- [ ] **Step 2: 创建 data.sql（初始化 admin 账号，BCrypt 加密 admin）**

```sql
-- 仅在表为空时插入，schema.sql 的 IF NOT EXISTS 保证表结构安全
-- admin/admin 的 BCrypt 值
INSERT OR IGNORE INTO merchant_user (id, username, password, must_change_password)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1);
```

> 注意：上述 BCrypt 值由 `new BCryptPasswordEncoder().encode("admin")` 生成，实现时可用代码验证。

- [ ] **Step 3: 创建全部 10 个 Entity 类**

每个 Entity 使用 MyBatis-Plus 的 `@TableName` + `@TableId(type = IdType.AUTO)` + `@TableField` 注解。公共字段 `create_time`、`update_time` 直接放在每个实体中（不抽取基类，保持简单）。

**MerchantUser.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("merchant_user")
public class MerchantUser {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    @TableField("must_change_password")
    private Integer mustChangePassword;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

**DishCategory.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dish_category")
public class DishCategory {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

**Dish.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("category_id")
    private Integer categoryId;
    private String name;
    private Integer price;
    private String image;
    private String description;
    private Integer status;
    private Integer stock;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

**TableInfo.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("table_info")
public class TableInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String area;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

**DiningSession.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dining_session")
public class DiningSession {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("table_id")
    private Integer tableId;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("end_time")
    private LocalDateTime endTime;
}
```

**SessionCheckout.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("session_checkout")
public class SessionCheckout {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("session_id")
    private Integer sessionId;
    @TableField("total_amount")
    private Integer totalAmount;
    @TableField("actual_paid")
    private Integer actualPaid;
    @TableField("discount_amount")
    private Integer discountAmount;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("points_earned")
    private Integer pointsEarned;
    @TableField("checkout_time")
    private LocalDateTime checkoutTime;
}
```

**Customer.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String phone;
    private String name;
    private Integer points;
    @TableField("total_spent")
    private Integer totalSpent;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

**Orders.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("order_no")
    private String orderNo;
    @TableField("session_id")
    private Integer sessionId;
    @TableField("table_id")
    private Integer tableId;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("total_amount")
    private Integer totalAmount;
    private Integer status;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private List<OrderItem> items;
}
```

**OrderItem.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("order_id")
    private Integer orderId;
    @TableField("dish_id")
    private Integer dishId;
    @TableField("dish_name")
    private String dishName;
    @TableField("dish_price")
    private Integer dishPrice;
    private Integer quantity;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

**PointsRecord.java：**

```java
package com.tongguo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("customer_id")
    private Integer customerId;
    @TableField("checkout_id")
    private Integer checkoutId;
    private Integer points;
    private Integer type;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 创建全部 10 个 Mapper 接口**

每个 Mapper 都继承 `BaseMapper<Entity>`，无需自定义方法（复杂查询用 XML 或后续 Task 添加）。

**MerchantUserMapper.java：**

```java
package com.tongguo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tongguo.entity.MerchantUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantUserMapper extends BaseMapper<MerchantUser> {
}
```

**其余 9 个 Mapper 同理：** `DishCategoryMapper`、`DishMapper`、`TableInfoMapper`、`DiningSessionMapper`、`SessionCheckoutMapper`、`CustomerMapper`、`OrdersMapper`、`OrderItemMapper`、`PointsRecordMapper`，分别对应各自的 Entity。

- [ ] **Step 5: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/
git commit -m "feat: 添加数据库 schema + 全部 entity + mapper"
```

---

### Task 3: 统一响应封装 + 图片上传

**Files:**
- Create: `backend/src/main/java/com/tongguo/controller/UploadController.java`
- Create: `backend/src/main/java/com/tongguo/service/UploadService.java`
- Create: `backend/src/main/java/com/tongguo/config/Result.java`

- [ ] **Step 1: 创建统一响应类 Result.java**

```java
package com.tongguo.config;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(1, message);
    }
}
```

- [ ] **Step 2: 创建 UploadService.java**

```java
package com.tongguo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    public String uploadImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 格式");
        }

        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "dish", yearMonth);
        Files.createDirectories(dir);

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        return "/uploads/dish/" + yearMonth + "/" + filename;
    }
}
```

- [ ] **Step 3: 创建 UploadController.java**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/m/upload")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = uploadService.uploadImage(file);
            return Result.ok(url);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}
```

- [ ] **Step 4: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add backend/
git commit -m "feat: 统一响应封装 + 图片上传接口"
```

---

## Phase 2: 后端 CRUD 业务 API

### Task 4: 商户认证（登录/登出/改密/信息）

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/AuthService.java`
- Create: `backend/src/main/java/com/tongguo/controller/AuthController.java`

- [ ] **Step 1: 创建 AuthService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.MerchantUser;
import com.tongguo.mapper.MerchantUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password, HttpSession session) {
        MerchantUser user = merchantUserMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 将用户信息存入 session（不存密码）
        MerchantUser sessionUser = new MerchantUser();
        sessionUser.setId(user.getId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setMustChangePassword(user.getMustChangePassword());
        session.setAttribute("merchantUser", sessionUser);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("mustChangePassword", user.getMustChangePassword());
        return result;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        MerchantUser user = merchantUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(0);
        user.setUpdateTime(LocalDateTime.now());
        merchantUserMapper.updateById(user);
    }

    public Map<String, Object> getInfo(HttpSession session) {
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("mustChangePassword", user.getMustChangePassword());
        return result;
    }
}
```

- [ ] **Step 2: 创建 AuthController.java**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.MerchantUser;
import com.tongguo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/m/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            Map<String, Object> data = authService.login(params.get("username"), params.get("password"), session);
            return Result.ok(data);
        } catch (IllegalArgumentException e) {
            return Result.error(4001, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout(session);
        return Result.ok();
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params, HttpSession session) {
        MerchantUser user = (MerchantUser) session.getAttribute("merchantUser");
        try {
            authService.changePassword(user.getId(), params.get("oldPassword"), params.get("newPassword"));
            // 更新 session 中的 mustChangePassword
            user.setMustChangePassword(0);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(HttpSession session) {
        return Result.ok(authService.getInfo(session));
    }
}
```

- [ ] **Step 3: 运行 `mvn compile` + 启动测试**

Run: `cd backend && mvn spring-boot:run`
Expected: 应用启动成功，访问 `POST /api/m/auth/login` 返回正常响应

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: 商户认证接口（登录/登出/改密/信息）"
```

---

### Task 5: 分类 CRUD + 菜品 CRUD + 库存管理

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/CategoryService.java`
- Create: `backend/src/main/java/com/tongguo/service/DishService.java`
- Create: `backend/src/main/java/com/tongguo/controller/CategoryController.java`
- Create: `backend/src/main/java/com/tongguo/controller/DishController.java`

- [ ] **Step 1: 创建 CategoryService.java**

```java
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
        // 检查该分类下是否有菜品
        Long count = dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, id)
        );
        if (count > 0) {
            throw new IllegalArgumentException("请先移走该分类下的菜品");
        }
        categoryMapper.deleteById(id);
    }
}
```

- [ ] **Step 2: 创建 DishService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Dish;
import com.tongguo.mapper.DishMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DishService {

    @Autowired
    private DishMapper dishMapper;

    /** 商户端：看全部菜品（含下架） */
    public List<Dish> listAll() {
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getSortOrder)
        );
    }

    /** 用户端：只看上架菜品 */
    public List<Dish> listPublished() {
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .orderByAsc(Dish::getSortOrder)
        );
    }

    public void add(Map<String, Object> params) {
        Dish dish = new Dish();
        dish.setCategoryId((Integer) params.get("categoryId"));
        dish.setName((String) params.get("name"));
        dish.setPrice(yuanToFen(params.get("price")));
        dish.setImage((String) params.get("image"));
        dish.setDescription((String) params.get("description"));
        dish.setStatus(1);
        dish.setStock(params.get("stock") != null ? (Integer) params.get("stock") : 0);
        dish.setSortOrder(params.get("sortOrder") != null ? (Integer) params.get("sortOrder") : 0);
        dishMapper.insert(dish);
    }

    public void update(Map<String, Object> params) {
        Dish dish = dishMapper.selectById((Integer) params.get("id"));
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        if (params.containsKey("categoryId")) dish.setCategoryId((Integer) params.get("categoryId"));
        if (params.containsKey("name")) dish.setName((String) params.get("name"));
        if (params.containsKey("price")) dish.setPrice(yuanToFen(params.get("price")));
        if (params.containsKey("image")) dish.setImage((String) params.get("image"));
        if (params.containsKey("description")) dish.setDescription((String) params.get("description"));
        if (params.containsKey("stock")) dish.setStock((Integer) params.get("stock"));
        if (params.containsKey("sortOrder")) dish.setSortOrder((Integer) params.get("sortOrder"));
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void toggle(Integer id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new IllegalArgumentException("菜品不存在");
        dish.setStatus(dish.getStatus() == 1 ? 0 : 1);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    public void updateStock(Integer id, Integer stock) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new IllegalArgumentException("菜品不存在");
        dish.setStock(stock);
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.updateById(dish);
    }

    /**
     * 元转分。接收 Number 或 String，支持最多两位小数。
     */
    private Integer yuanToFen(Object yuan) {
        if (yuan == null) return 0;
        BigDecimal bd = new BigDecimal(yuan.toString());
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.multiply(new BigDecimal(100)).intValue();
    }
}
```

- [ ] **Step 3: 创建 CategoryController.java**

```java
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
```

- [ ] **Step 4: 创建 DishController.java（含商户端和用户端两个路径）**

```java
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

    // ========== 商户端 ==========

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

    // ========== 用户端 ==========

    @GetMapping("/api/c/dish/list")
    public Result<Map<String, Object>> customerList() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryService.list());
        data.put("dishes", dishService.listPublished());
        return Result.ok(data);
    }
}
```

- [ ] **Step 5: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/
git commit -m "feat: 分类 CRUD + 菜品 CRUD + 库存管理接口"
```

---

### Task 6: 桌台 CRUD + 二维码生成

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/TableService.java`
- Create: `backend/src/main/java/com/tongguo/controller/TableController.java`

- [ ] **Step 1: 创建 TableService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.TableInfo;
import com.tongguo.mapper.DiningSessionMapper;
import com.tongguo.mapper.TableInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class TableService {

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private DiningSessionMapper diningSessionMapper;

    public List<TableInfo> list() {
        return tableInfoMapper.selectList(null);
    }

    public void add(TableInfo tableInfo) {
        tableInfoMapper.insert(tableInfo);
    }

    public void update(TableInfo tableInfo) {
        tableInfoMapper.updateById(tableInfo);
    }

    public void delete(Integer id) {
        // 检查是否有进行中的 session
        Long count = diningSessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, id)
                        .eq(DiningSession::getStatus, 0)
        );
        if (count > 0) {
            throw new IllegalArgumentException("该桌台存在进行中的就餐会话，无法删除");
        }
        tableInfoMapper.deleteById(id);
    }

    public String generateQRCode(Integer id, String baseUrl) throws WriterException, IOException {
        TableInfo table = tableInfoMapper.selectById(id);
        if (table == null) throw new IllegalArgumentException("桌台不存在");

        String content = baseUrl + "/c/login/" + id;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
```

- [ ] **Step 2: 创建 TableController.java**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
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

    @PostMapping("/add")
    public Result<Void> add(@RequestBody TableInfo tableInfo) {
        tableService.add(tableInfo);
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody TableInfo tableInfo) {
        tableService.update(tableInfo);
        return Result.ok();
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
```

- [ ] **Step 3: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: 桌台 CRUD + 二维码生成接口"
```

---

### Task 7: 客户管理（商户端 + 用户端）

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/CustomerService.java`
- Create: `backend/src/main/java/com/tongguo/controller/CustomerController.java`

- [ ] **Step 1: 创建 CustomerService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.mapper.CustomerMapper;
import com.tongguo.mapper.PointsRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    /** 根据手机号查找或创建客户 */
    @Transactional
    public Customer findOrCreateByPhone(String phone) {
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)
        );
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setName(phone);
            customer.setPoints(0);
            customer.setTotalSpent(0);
            customerMapper.insert(customer);
        }
        return customer;
    }

    public Customer getByPhone(String phone) {
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)
        );
    }

    public List<Customer> search(String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Customer::getPhone, keyword).or().like(Customer::getName, keyword);
        }
        wrapper.orderByDesc(Customer::getUpdateTime);
        return customerMapper.selectList(wrapper);
    }

    public Customer detail(Integer id) {
        return customerMapper.selectById(id);
    }

    public List<PointsRecord> getPointsRecords(Integer customerId) {
        return pointsRecordMapper.selectList(
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getCustomerId, customerId)
                        .orderByDesc(PointsRecord::getCreateTime)
        );
    }

    /** 手动加减积分 */
    @Transactional
    public void manualPoints(Integer customerId, Integer points, String remark) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("客户不存在");
        customer.setPoints(customer.getPoints() + points);
        customer.setUpdateTime(LocalDateTime.now());
        customerMapper.updateById(customer);

        PointsRecord record = new PointsRecord();
        record.setCustomerId(customerId);
        record.setPoints(points);
        record.setType(points > 0 ? 1 : 2); // 1=手动增加, 2=扣减（含手动扣减和兑换扣减）
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }
}
```

- [ ] **Step 2: 创建 CustomerController.java（含商户端 + 用户端路径）**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.Customer;
import com.tongguo.entity.PointsRecord;
import com.tongguo.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // ========== 用户端 ==========

    @PostMapping("/api/c/auth/login")
    public Result<Map<String, Object>> customerLogin(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            // 不填手机号，直接返回成功，无客户信息
            return Result.ok();
        }
        Customer customer = customerService.findOrCreateByPhone(phone);
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("phone", customer.getPhone());
        data.put("name", customer.getName());
        return Result.ok(data);
    }

    private String extractPhone(String phoneHeader) {
        if (phoneHeader == null || phoneHeader.trim().isEmpty()) return null;
        return phoneHeader.trim();
    }

    @GetMapping("/api/c/customer/info")
    public Result<Map<String, Object>> customerInfo(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) {
            return Result.error(40101, "请先登录");
        }
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) {
            return Result.error(40101, "请先登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", customer.getId());
        data.put("phone", customer.getPhone());
        data.put("name", customer.getName());
        data.put("points", customer.getPoints());
        data.put("totalSpent", customer.getTotalSpent());
        return Result.ok(data);
    }

    @GetMapping("/api/c/customer/orders")
    public Result<?> customerOrders(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) return Result.error(40101, "请先登录");
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) return Result.error(40101, "请先登录");
        // 返回该客户关联的订单（具体实现在 OrderService 中）
        return Result.ok(java.util.Collections.emptyList());
    }

    @GetMapping("/api/c/customer/points")
    public Result<List<PointsRecord>> customerPoints(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
        String phone = extractPhone(phoneHeader);
        if (phone == null) return Result.error(40101, "请先登录");
        Customer customer = customerService.getByPhone(phone);
        if (customer == null) return Result.error(40101, "请先登录");
        return Result.ok(customerService.getPointsRecords(customer.getId()));
    }

    // ========== 商户端 ==========

    @GetMapping("/api/m/customer/list")
    public Result<List<Customer>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(customerService.search(keyword));
    }

    @GetMapping("/api/m/customer/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Customer customer = customerService.detail(id);
        if (customer == null) return Result.error("客户不存在");
        List<PointsRecord> records = customerService.getPointsRecords(id);
        Map<String, Object> data = new HashMap<>();
        data.put("customer", customer);
        data.put("pointsRecords", records);
        return Result.ok(data);
    }

    @PostMapping("/api/m/customer/points")
    public Result<Void> manualPoints(@RequestBody Map<String, Object> params) {
        try {
            customerService.manualPoints(
                    (Integer) params.get("customerId"),
                    (Integer) params.get("points"),
                    (String) params.get("remark")
            );
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: 客户管理接口（商户端 + 用户端）"
```

---

## Phase 3: 后端订单与会话核心

### Task 8: 就餐会话服务（getOrCreate 逻辑）

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/SessionService.java`
- Create: `backend/src/main/java/com/tongguo/controller/SessionController.java`

- [ ] **Step 1: 创建 SessionService.java**

这是系统的核心服务之一，负责就餐会话的创建、查找、结账等逻辑。

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    @Autowired
    private DiningSessionMapper sessionMapper;

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    /**
     * 获取或创建指定桌台的进行中 session。
     * SQLite 天然串行写入，配合部分唯一索引，保证同桌只有一个活跃 session。
     */
    @Transactional
    public DiningSession getOrCreateSession(Integer tableId) {
        // 1. 查找该桌进行中的 session
        DiningSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        );
        if (session != null) {
            return session;
        }

        // 2. 不存在则创建
        session = new DiningSession();
        session.setTableId(tableId);
        session.setStatus(0);
        sessionMapper.insert(session);

        // 3. 若插入后唯一索引冲突（并发），回查复用
        //    SQLite 串行写入下此场景极少，但做防御性处理
        session = sessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        );

        // 4. 更新桌台状态为"使用中"
        if (tableId != null) {
            TableInfo table = tableInfoMapper.selectById(tableId);
            if (table != null && table.getStatus() == 0) {
                table.setStatus(1);
                tableInfoMapper.updateById(table);
            }
        }

        return session;
    }

    /** 为散客创建无桌台 session */
    public DiningSession createWalkInSession() {
        DiningSession session = new DiningSession();
        session.setStatus(0);
        sessionMapper.insert(session);
        return session;
    }

    /** 获取桌台当前进行中 session */
    public DiningSession getCurrentByTableId(Integer tableId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<DiningSession>()
                        .eq(DiningSession::getTableId, tableId)
                        .eq(DiningSession::getStatus, 0)
        );
    }

    /** 获取 session 详情（含订单列表、菜品汇总） */
    public Map<String, Object> getDetail(Integer sessionId) {
        DiningSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");

        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, sessionId)
                        .ne(Orders::getStatus, 5) // 排除已取消
        );

        // 批量查出该 session 下所有有效订单的 order_item（避免 N+1 查询）
        List<Integer> orderIds = new ArrayList<>();
        for (Orders order : orders) {
            if (order.getStatus() != 4) orderIds.add(order.getId());
        }
        List<OrderItem> allItems = orderIds.isEmpty() ? Collections.emptyList() :
                orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId, orderIds)
                                .in(OrderItem::getStatus, 1, 2)
                );
        // 按 orderId 分组
        Map<Integer, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 计算应结总额
        int totalAmount = 0;
        for (OrderItem item : allItems) {
            totalAmount += item.getDishPrice() * item.getQuantity();
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("session", session);
        detail.put("orders", orders);
        detail.put("totalAmount", totalAmount);
        return detail;
    }

    /**
     * 整桌结账 — 核心事务操作
     * 幂等：若已存在 checkout 记录直接返回成功
     */
    @Transactional
    public SessionCheckout checkout(Integer sessionId, Integer actualPaidFen, String phone) {
        // 幂等检查
        SessionCheckout existing = checkoutMapper.selectOne(
                new LambdaQueryWrapper<SessionCheckout>().eq(SessionCheckout::getSessionId, sessionId)
        );
        if (existing != null) {
            return existing;
        }

        DiningSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() != 0) {
            throw new IllegalArgumentException("会话不存在或已结束");
        }

        // 加载该 session 下全部可结账订单
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, sessionId)
                        .notIn(Orders::getStatus, 4, 5)
        );

        // 检查是否还有待确认订单
        for (Orders order : orders) {
            if (order.getStatus() == 0) {
                throw new IllegalArgumentException("存在待确认订单，请先确认或取消后再结账");
            }
        }

        // 批量查出全部订单的 order_item（避免 N+1 查询）
        List<Integer> orderIds = new ArrayList<>();
        for (Orders order : orders) { orderIds.add(order.getId()); }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .in(OrderItem::getStatus, 1, 2)
        );

        // 计算应结总额
        int totalAmount = 0;
        for (OrderItem item : allItems) {
            totalAmount += item.getDishPrice() * item.getQuantity();
        }

        int discountAmount = totalAmount - actualPaidFen;
        int pointsEarned = actualPaidFen / 100; // floor(分/100) = 元数

        // 创建 checkout 记录
        SessionCheckout checkout = new SessionCheckout();
        checkout.setSessionId(sessionId);
        checkout.setTotalAmount(totalAmount);
        checkout.setActualPaid(actualPaidFen);
        checkout.setDiscountAmount(discountAmount);
        checkout.setPointsEarned(pointsEarned);
        checkout.setCheckoutTime(LocalDateTime.now());

        // 处理客户积分
        Integer customerId = null;
        if (phone != null && !phone.trim().isEmpty()) {
            Customer customer = customerService.findOrCreateByPhone(phone);
            customerId = customer.getId();
            checkout.setCustomerId(customerId);
        }

        // 先插入 checkout 获取自增 ID，再写入积分记录时直接设置 checkoutId
        checkoutMapper.insert(checkout);

        if (customerId != null) {
            // 写入积分记录（checkoutId 已可用，直接设置）
            PointsRecord pointsRecord = new PointsRecord();
            pointsRecord.setCustomerId(customerId);
            pointsRecord.setCheckoutId(checkout.getId());
            pointsRecord.setPoints(pointsEarned);
            pointsRecord.setType(0);
            pointsRecord.setRemark("消费获得");
            pointsRecordMapper.insert(pointsRecord);

            // 更新客户积分和累计消费
            Customer customer = customerMapper.selectById(customerId);
            customer.setPoints(customer.getPoints() + pointsEarned);
            customer.setTotalSpent(customer.getTotalSpent() + actualPaidFen);
            customer.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(customer);
        }

        // 批量更新订单状态为已结账
        for (Orders order : orders) {
            order.setStatus(4);
            order.setUpdateTime(LocalDateTime.now());
            ordersMapper.updateById(order);
        }

        // 结束 session
        session.setStatus(1);
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 恢复桌台状态
        if (session.getTableId() != null) {
            TableInfo table = tableInfoMapper.selectById(session.getTableId());
            if (table != null) {
                table.setStatus(0);
                tableInfoMapper.updateById(table);
            }
        }

        return checkout;
    }
}
```

- [ ] **Step 2: 创建 SessionController.java**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.DiningSession;
import com.tongguo.entity.SessionCheckout;
import com.tongguo.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/m/session")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/current/{tableId}")
    public Result<DiningSession> current(@PathVariable Integer tableId) {
        DiningSession session = sessionService.getCurrentByTableId(tableId);
        return Result.ok(session);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        try {
            return Result.ok(sessionService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/checkout/{id}")
    public Result<SessionCheckout> checkout(@PathVariable Integer id,
                                             @RequestBody Map<String, Object> params) {
        try {
            // actualPaid 前端传元，转为分
            Object paidObj = params.get("actualPaid");
            int actualPaidFen = 0;
            if (paidObj != null) {
                BigDecimal bd = new BigDecimal(paidObj.toString())
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                actualPaidFen = bd.multiply(new BigDecimal(100)).intValueExact();
            }
            String phone = (String) params.get("phone");
            SessionCheckout checkout = sessionService.checkout(id, actualPaidFen, phone);
            return Result.ok(checkout);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: 就餐会话服务 + 整桌结账事务接口"
```

---

### Task 9: 订单创建（用户端 + 商户端）

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/OrderService.java`
- Create: `backend/src/main/java/com/tongguo/controller/OrderController.java`

- [ ] **Step 1: 创建 OrderService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CustomerService customerService;

    /**
     * 创建订单（用户端和商户端共用）
     * @param tableId  桌台ID（可为空，散客）
     * @param sessionId 指定会话ID（商户加菜时使用）
     * @param items    菜品列表 [{dishId, quantity}]
     * @param phone    客户手机号（可为空）
     * @param remark   备注
     * @return 创建的订单
     */
    @Transactional
    public Orders createOrder(Integer tableId, Integer sessionId,
                               List<Map<String, Object>> items,
                               String phone, String remark) {
        // 1. 确定 session
        DiningSession session;
        if (sessionId != null) {
            // 商户加菜到已有 session，需验证 session 存在且进行中
            session = sessionMapper.selectById(sessionId);
            if (session == null || session.getStatus() != 0) {
                throw new IllegalArgumentException("会话不存在或已结束");
            }
        } else if (tableId != null) {
            session = sessionService.getOrCreateSession(tableId);
        } else {
            session = sessionService.createWalkInSession();
        }

        // 2. 生成订单号
        String orderNo = generateOrderNo();

        // 3. 计算订单总额 + 创建订单项
        int totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Integer dishId = (Integer) item.get("dishId");
            Integer quantity = ((Number) item.get("quantity")).intValue();
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) continue;

            OrderItem oi = new OrderItem();
            oi.setDishId(dishId);
            oi.setDishName(dish.getName());
            oi.setDishPrice(dish.getPrice());
            oi.setQuantity(quantity);
            oi.setStatus(0); // 待确认
            orderItems.add(oi);

            totalAmount += dish.getPrice() * quantity;
        }

        // 4. 关联客户
        Integer customerId = null;
        if (phone != null && !phone.trim().isEmpty()) {
            Customer customer = customerService.findOrCreateByPhone(phone);
            customerId = customer.getId();
        }

        // 5. 创建订单
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setSessionId(session.getId());
        order.setTableId(tableId);
        order.setCustomerId(customerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待确认
        order.setRemark(remark);
        ordersMapper.insert(order);

        // 6. 创建订单项
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);
        }

        return order;
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return date + random;
    }

    /** 商户端订单列表（附带订单项） */
    public List<Orders> listOrders(Integer status, Integer tableId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Orders::getStatus, status);
        if (tableId != null) wrapper.eq(Orders::getTableId, tableId);
        wrapper.orderByDesc(Orders::getCreateTime);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        if (orders.isEmpty()) return orders;

        // 批量加载订单项（避免 N+1 查询）
        List<Integer> orderIds = new ArrayList<>();
        for (Orders o : orders) { orderIds.add(o.getId()); }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );
        Map<Integer, List<OrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), Collections.emptyList()));
        }
        return orders;
    }

    /** 获取桌台当前 session 的进行中订单（用户端），附带订单项 */
    public List<Orders> getTableCurrentOrders(Integer tableId) {
        DiningSession session = sessionService.getCurrentByTableId(tableId);
        if (session == null) return Collections.emptyList();
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getSessionId, session.getId())
                        .notIn(Orders::getStatus, 4, 5)
                        .orderByDesc(Orders::getCreateTime)
        );
        if (orders.isEmpty()) return orders;

        // 批量查出所有 order_item（避免 N+1 查询）
        List<Integer> orderIds = new ArrayList<>();
        for (Orders o : orders) { orderIds.add(o.getId()); }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
        );
        Map<Integer, List<OrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemsMap.getOrDefault(order.getId(), Collections.emptyList()));
        }
        return orders;
    }

    /** 获取订单项列表 */
    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );
    }

    /** 获取客户的消费记录 */
    public List<Orders> getCustomerOrders(Integer customerId) {
        return ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getCustomerId, customerId)
                        .orderByDesc(Orders::getCreateTime)
        );
    }
}
```

- [ ] **Step 2: 创建 OrderController.java（含用户端 + 商户端路径）**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.entity.OrderItem;
import com.tongguo.entity.Orders;
import com.tongguo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ========== 用户端 ==========

    @PostMapping("/api/c/order/create")
    public Result<Orders> customerCreate(@RequestBody Map<String, Object> params) {
        try {
            Orders order = orderService.createOrder(
                    toInt(params.get("tableId")),
                    null,
                    (List<Map<String, Object>>) params.get("items"),
                    (String) params.get("phone"),
                    (String) params.get("remark")
            );
            return Result.ok(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/c/order/table/{tableId}")
    public Result<List<Orders>> tableOrders(@PathVariable Integer tableId) {
        return Result.ok(orderService.getTableCurrentOrders(tableId));
    }

    // ========== 商户端 ==========

    @GetMapping("/api/m/order/list")
    public Result<List<Orders>> list(@RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) Integer tableId) {
        return Result.ok(orderService.listOrders(status, tableId));
    }

    @PostMapping("/api/m/order/create")
    public Result<Orders> merchantCreate(@RequestBody Map<String, Object> params) {
        try {
            Orders order = orderService.createOrder(
                    toInt(params.get("tableId")),
                    toInt(params.get("sessionId")),
                    (List<Map<String, Object>>) params.get("items"),
                    (String) params.get("phone"),
                    (String) params.get("remark")
            );
            return Result.ok(order);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/api/m/order/items/{orderId}")
    public Result<List<OrderItem>> items(@PathVariable Integer orderId) {
        return Result.ok(orderService.getOrderItems(orderId));
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        return ((Number) obj).intValue();
    }
}
```

- [ ] **Step 3: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add backend/
git commit -m "feat: 订单创建接口（用户端 + 商户端）"
```

---

### Task 10: 订单操作（确认/上菜/退单品/整单取消）+ 订单状态自动流转

**Files:**
- Modify: `backend/src/main/java/com/tongguo/service/OrderService.java`（新增 4 个方法）
- Modify: `backend/src/main/java/com/tongguo/controller/OrderController.java`（新增 4 个端点）

- [ ] **Step 1: 在 OrderService.java 中新增以下方法**

```java
@Autowired
private DishMapper dishMapper;

/** 确认订单：逐项扣减库存 */
@Transactional
public Orders confirmOrder(Integer orderId) {
    Orders order = ordersMapper.selectById(orderId);
    if (order == null) throw new IllegalArgumentException("订单不存在");
    if (order.getStatus() != 0) throw new IllegalArgumentException("订单状态不允许确认");

    List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
    );

    for (OrderItem item : items) {
        if (item.getStatus() != 0) continue; // 跳过已处理的
        // 乐观扣减库存
        int affected = dishMapper.deductStock(item.getDishId(), item.getQuantity());
        if (affected > 0) {
            item.setStatus(1); // 待上菜
        } else {
            item.setStatus(3); // 库存不足
        }
        orderItemMapper.updateById(item);
    }

    // 刷新订单状态
    refreshOrderStatus(orderId);
    return ordersMapper.selectById(orderId);
}

/** 标记菜品已上菜 */
public OrderItem serveItem(Integer itemId) {
    OrderItem item = orderItemMapper.selectById(itemId);
    if (item == null) throw new IllegalArgumentException("订单项不存在");
    if (item.getStatus() != 1) throw new IllegalArgumentException("只能上待上菜的菜品");
    item.setStatus(2); // 已上菜
    orderItemMapper.updateById(item);

    refreshOrderStatus(item.getOrderId());
    return item;
}

/** 退单个菜品 */
public OrderItem cancelItem(Integer itemId) {
    OrderItem item = orderItemMapper.selectById(itemId);
    if (item == null) throw new IllegalArgumentException("订单项不存在");

    if (item.getStatus() == 0) {
        // 待确认 → 直接标记退菜，不归还库存
        item.setStatus(4);
    } else if (item.getStatus() == 1) {
        // 待上菜 → 归还库存后标记退菜
        dishMapper.addStock(item.getDishId(), item.getQuantity());
        item.setStatus(4);
    } else {
        throw new IllegalArgumentException("该状态不允许退菜");
    }
    orderItemMapper.updateById(item);

    refreshOrderStatus(item.getOrderId());
    return item;
}

/** 整单取消 */
@Transactional
public Orders cancelOrder(Integer orderId) {
    Orders order = ordersMapper.selectById(orderId);
    if (order == null) throw new IllegalArgumentException("订单不存在");

    List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
    );

    // 检查是否有已上菜项
    boolean hasServed = items.stream().anyMatch(i -> i.getStatus() == 2);
    if (hasServed) throw new IllegalArgumentException("订单中有已上菜菜品，无法整单取消");

    for (OrderItem item : items) {
        if (item.getStatus() == 1) {
            // 归还库存
            dishMapper.addStock(item.getDishId(), item.getQuantity());
            item.setStatus(4);
        } else if (item.getStatus() == 0) {
            item.setStatus(4);
        }
        // status=3(库存不足) 不受影响
        if (item.getStatus() != 3) {
            orderItemMapper.updateById(item);
        }
    }

    refreshOrderStatus(orderId);
    return ordersMapper.selectById(orderId);
}

/**
 * 根据 order_item 状态自动刷新订单状态
 * 0待确认 1制作中 2部分上菜 3全部上菜 4已结账 5已取消
 */
private void refreshOrderStatus(Integer orderId) {
    Orders order = ordersMapper.selectById(orderId);
    List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
    );

    if (items.isEmpty()) {
        order.setStatus(5);
        ordersMapper.updateById(order);
        return;
    }

    long status0 = items.stream().filter(i -> i.getStatus() == 0).count();
    long status1 = items.stream().filter(i -> i.getStatus() == 1).count();
    long status2 = items.stream().filter(i -> i.getStatus() == 2).count();
    long status3 = items.stream().filter(i -> i.getStatus() == 3).count();
    long status4 = items.stream().filter(i -> i.getStatus() == 4).count();

    // 全部为 3/4 且无 2 → 自动取消
    if (status2 == 0 && (status0 + status1) == 0) {
        order.setStatus(5);
    } else if (status0 > 0) {
        order.setStatus(0);
    } else if (status2 > 0 && status1 > 0) {
        order.setStatus(2); // 部分上菜
    } else if (status2 > 0 && status1 == 0) {
        order.setStatus(3); // 全部上菜
    } else if (status1 > 0) {
        order.setStatus(1); // 制作中
    } else {
        order.setStatus(5);
    }

    order.setUpdateTime(LocalDateTime.now());
    ordersMapper.updateById(order);
}
```

- [ ] **Step 2: 在 DishMapper.java 中新增两个自定义方法**

在 `DishMapper` 接口中添加：

```java
@Update("UPDATE dish SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
int deductStock(@Param("id") Integer id, @Param("qty") Integer qty);

@Update("UPDATE dish SET stock = stock + #{qty} WHERE id = #{id}")
int addStock(@Param("id") Integer id, @Param("qty") Integer qty);
```

需在文件头部添加 import：

```java
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
```

- [ ] **Step 3: 在 OrderController.java 中新增 4 个商户端端点**

```java
@PutMapping("/api/m/order/confirm/{id}")
public Result<Orders> confirm(@PathVariable Integer id) {
    try {
        return Result.ok(orderService.confirmOrder(id));
    } catch (IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}

@PutMapping("/api/m/order/serve-item/{id}")
public Result<OrderItem> serveItem(@PathVariable Integer id) {
    try {
        return Result.ok(orderService.serveItem(id));
    } catch (IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}

@PutMapping("/api/m/order/cancel-item/{id}")
public Result<OrderItem> cancelItem(@PathVariable Integer id) {
    try {
        return Result.ok(orderService.cancelItem(id));
    } catch (IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}

@PutMapping("/api/m/order/cancel/{id}")
public Result<Orders> cancel(@PathVariable Integer id) {
    try {
        return Result.ok(orderService.cancelOrder(id));
    } catch (IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }
}
```

- [ ] **Step 4: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add backend/
git commit -m "feat: 订单操作接口（确认/上菜/退单品/整单取消）+ 状态自动流转"
```

---

### Task 11: 工作台 Dashboard API

**Files:**
- Create: `backend/src/main/java/com/tongguo/service/DashboardService.java`
- Create: `backend/src/main/java/com/tongguo/controller/MerchantController.java`

- [ ] **Step 1: 创建 DashboardService.java**

```java
package com.tongguo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tongguo.entity.*;
import com.tongguo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private SessionCheckoutMapper checkoutMapper;

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private DiningSessionMapper sessionMapper;

    public Map<String, Object> getTodayData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> data = new HashMap<>();

        // 今日订单数（不含已取消）
        Long orderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .ge(Orders::getCreateTime, todayStart)
                        .ne(Orders::getStatus, 5)
        );
        data.put("todayOrders", orderCount);

        // 今日营收
        List<SessionCheckout> checkouts = checkoutMapper.selectList(
                new LambdaQueryWrapper<SessionCheckout>()
                        .ge(SessionCheckout::getCheckoutTime, todayStart)
        );
        int revenue = checkouts.stream().mapToInt(SessionCheckout::getActualPaid).sum();
        data.put("todayRevenue", revenue); // 单位：分

        // 桌台状态
        List<TableInfo> tables = tableInfoMapper.selectList(null);
        long freeTables = tables.stream().filter(t -> t.getStatus() == 0).count();
        long busyTables = tables.stream().filter(t -> t.getStatus() == 1).count();
        data.put("totalTables", tables.size());
        data.put("freeTables", freeTables);
        data.put("busyTables", busyTables);

        // 进行中的 session 数
        Long activeSessions = sessionMapper.selectCount(
                new LambdaQueryWrapper<DiningSession>().eq(DiningSession::getStatus, 0)
        );
        data.put("activeSessions", activeSessions);

        return data;
    }
}
```

- [ ] **Step 2: 创建 MerchantController.java**

```java
package com.tongguo.controller;

import com.tongguo.config.Result;
import com.tongguo.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/m")
public class MerchantController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(dashboardService.getTodayData());
    }
}
```

- [ ] **Step 3: 完善 CustomerController 中客户消费记录**

修改 `CustomerController` 中 `/api/c/customer/orders` 端点，注入 OrderService：

```java
@Autowired
private OrderService orderService;

@GetMapping("/api/c/customer/orders")
public Result<?> customerOrders(@RequestHeader(value = "X-Phone", required = false) String phoneHeader) {
    String phone = extractPhone(phoneHeader);
    if (phone == null) return Result.error(40101, "请先登录");
    Customer customer = customerService.getByPhone(phone);
    if (customer == null) return Result.error(40101, "请先登录");
    return Result.ok(orderService.getCustomerOrders(customer.getId()));
}
```

- [ ] **Step 4: 运行 `mvn compile` 验证编译通过**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 启动应用并手动测试核心 API**

Run: `cd backend && mvn spring-boot:run`

测试用例：
1. `POST /api/m/auth/login` → 登录成功，返回 `mustChangePassword: 1`
2. `GET /api/m/category/list` → 空列表
3. `POST /api/m/category/add` → 新增分类
4. `POST /api/m/dish/add` → 新增菜品
5. `POST /api/c/order/create` → 用户下单
6. `PUT /api/m/order/confirm/1` → 确认订单
7. `GET /api/m/dashboard` → 今日数据

- [ ] **Step 6: 提交**

```bash
git add backend/
git commit -m "feat: 工作台 Dashboard API + 客户消费记录接口"
```

---

## Phase 4: 前端基础 + 商户端页面

### Task 12: Vue 3 项目骨架 + Router + Axios 封装

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/api/request.js`

- [ ] **Step 1: 创建 frontend 目录并初始化**

Run: `cd /home/CFFEX/Desktop/zyx/workspece/aicoding/铜锅涮肉 && npm create vite@latest frontend -- --template vue`

然后安装依赖：

Run: `cd frontend && npm install element-plus vue-router@4 axios`

- [ ] **Step 2: 配置 vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})
```

- [ ] **Step 3: 配置 main.js**

```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
```

- [ ] **Step 4: 创建 App.vue**

```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 5: 创建 router/index.js**

```javascript
import { createRouter, createWebHistory } from 'vue-router'

// 商户端
const MerchantLogin = () => import('../views/merchant/Login.vue')
const MerchantLayout = () => import('../views/merchant/Layout.vue')
const Dashboard = () => import('../views/merchant/Dashboard.vue')
const Dishes = () => import('../views/merchant/Dishes.vue')
const Tables = () => import('../views/merchant/Tables.vue')
const Orders = () => import('../views/merchant/Orders.vue')
const Sessions = () => import('../views/merchant/Sessions.vue')
const Customers = () => import('../views/merchant/Customers.vue')
const ManualOrder = () => import('../views/merchant/ManualOrder.vue')

// 用户端
const CustomerLogin = () => import('../views/customer/Login.vue')
const CustomerOrder = () => import('../views/customer/Order.vue')
const CustomerStatus = () => import('../views/customer/Status.vue')
const CustomerMine = () => import('../views/customer/Mine.vue')

const routes = [
  // 商户端
  { path: '/m/login', component: MerchantLogin },
  {
    path: '/m',
    component: MerchantLayout,
    meta: { requiresMerchantAuth: true },
    children: [
      { path: '', redirect: '/m/dashboard' },
      { path: 'dashboard', component: Dashboard },
      { path: 'dishes', component: Dishes },
      { path: 'tables', component: Tables },
      { path: 'orders', component: Orders },
      { path: 'sessions', component: Sessions },
      { path: 'customers', component: Customers },
      { path: 'manual-order', component: ManualOrder },
    ]
  },

  // 用户端
  { path: '/c/login/:tableId', component: CustomerLogin },
  { path: '/c/order/:tableId', component: CustomerOrder },
  { path: '/c/status/:tableId', component: CustomerStatus },
  { path: '/c/mine', component: CustomerMine },

  // 默认
  { path: '/', redirect: '/m/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 商户端路由守卫
router.beforeEach((to, from, next) => {
  if (to.matched.some(r => r.meta.requiresMerchantAuth)) {
    const merchant = sessionStorage.getItem('merchantUser')
    if (!merchant) {
      next('/m/login')
      return
    }
  }
  next()
})

export default router
```

- [ ] **Step 6: 创建 api/request.js（axios 实例 + 拦截器）**

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 商户端 axios 实例
const merchantRequest = axios.create({
  baseURL: '',
  withCredentials: true
})

merchantRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        sessionStorage.removeItem('merchantUser')
        router.push('/m/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('merchantUser')
      router.push('/m/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 用户端 axios 实例
const customerRequest = axios.create({
  baseURL: ''
})

// 自动附加 X-Phone header
customerRequest.interceptors.request.use(config => {
  const phone = localStorage.getItem('customerPhone')
  if (phone) {
    config.headers['X-Phone'] = phone
  }
  return config
})

customerRequest.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 40101) {
      // 用户端需要手机号
      const tableId = localStorage.getItem('currentTableId')
      router.push('/c/login/' + (tableId || '0'))
      return Promise.reject(new Error('请先登录'))
    }
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export { merchantRequest, customerRequest }
```

- [ ] **Step 7: 创建 API 模块文件**

**api/auth.js：**

```javascript
import { merchantRequest as req } from './request'

export const login = (data) => req.post('/api/m/auth/login', data)
export const logout = () => req.post('/api/m/auth/logout')
export const changePassword = (data) => req.put('/api/m/auth/password', data)
export const getInfo = () => req.get('/api/m/auth/info')
```

**api/dish.js：**

```javascript
import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = () => mReq.get('/api/m/dish/list')
export const addDish = (data) => mReq.post('/api/m/dish/add', data)
export const updateDish = (data) => mReq.put('/api/m/dish/update', data)
export const toggleDish = (id) => mReq.put(`/api/m/dish/toggle/${id}`)
export const updateStock = (data) => mReq.put('/api/m/dish/stock', data)
export const customerList = () => cReq.get('/api/c/dish/list')
export const customerCategoryList = () => cReq.get('/api/c/dish/list')  // 同一接口，返回含 categories
```

**api/category.js：**

```javascript
import { merchantRequest as req } from './request'

export const list = () => req.get('/api/m/category/list')
export const add = (data) => req.post('/api/m/category/add', data)
export const update = (data) => req.put('/api/m/category/update', data)
export const remove = (id) => req.delete(`/api/m/category/${id}`)
```

**api/table.js：**

```javascript
import { merchantRequest as req } from './request'

export const list = () => req.get('/api/m/table/list')
export const add = (data) => req.post('/api/m/table/add', data)
export const update = (data) => req.put('/api/m/table/update', data)
export const remove = (id) => req.delete(`/api/m/table/${id}`)
export const qrcode = (id) => req.get(`/api/m/table/qrcode/${id}`)
```

**api/order.js：**

```javascript
import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = (params) => mReq.get('/api/m/order/list', { params })
export const merchantCreate = (data) => mReq.post('/api/m/order/create', data)
export const confirm = (id) => mReq.put(`/api/m/order/confirm/${id}`)
export const serveItem = (id) => mReq.put(`/api/m/order/serve-item/${id}`)
export const cancelItem = (id) => mReq.put(`/api/m/order/cancel-item/${id}`)
export const cancelOrder = (id) => mReq.put(`/api/m/order/cancel/${id}`)
export const getItems = (orderId) => mReq.get(`/api/m/order/items/${orderId}`)

export const customerCreate = (data) => cReq.post('/api/c/order/create', data)
export const tableOrders = (tableId) => cReq.get(`/api/c/order/table/${tableId}`)
```

**api/session.js：**

```javascript
import { merchantRequest as req } from './request'

export const current = (tableId) => req.get(`/api/m/session/current/${tableId}`)
export const detail = (id) => req.get(`/api/m/session/detail/${id}`)
export const checkout = (id, data) => req.put(`/api/m/session/checkout/${id}`, data)
```

**api/customer.js：**

```javascript
import { merchantRequest as mReq, customerRequest as cReq } from './request'

export const merchantList = (params) => mReq.get('/api/m/customer/list', { params })
export const merchantDetail = (id) => mReq.get(`/api/m/customer/detail/${id}`)
export const manualPoints = (data) => mReq.post('/api/m/customer/points', data)

export const customerLogin = (data) => cReq.post('/api/c/auth/login', data)
export const customerInfo = () => cReq.get('/api/c/customer/info')
export const customerOrders = () => cReq.get('/api/c/customer/orders')
export const customerPoints = () => cReq.get('/api/c/customer/points')
```

**api/dashboard.js：**

```javascript
import { merchantRequest as req } from './request'

export const getData = () => req.get('/api/m/dashboard')
```

**api/upload.js：**

```javascript
import { merchantRequest as req } from './request'

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return req.post('/api/m/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

- [ ] **Step 8: 运行 `npm run dev` 验证前端启动**

Run: `cd frontend && npm run dev`
Expected: Vite dev server 启动成功

- [ ] **Step 9: 提交**

```bash
git add frontend/
git commit -m "feat: Vue 3 前端项目骨架 + Router + Axios + API 模块"
```

---

### Task 13: 商户端登录页 + 布局框架

**Files:**
- Create: `frontend/src/views/merchant/Login.vue`
- Create: `frontend/src/views/merchant/Layout.vue`

- [ ] **Step 1: 创建 Login.vue**

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align:center; margin-bottom:20px">铜锅涮肉管理系统</h2>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" native-type="submit" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 强制改密弹窗 -->
      <el-dialog v-model="showChangePwd" title="首次登录，请修改密码" :close-on-click-modal="false" :show-close="false">
        <el-form :model="pwdForm">
          <el-form-item label="旧密码">
            <el-input v-model="pwdForm.oldPassword" type="password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" @click="handleChangePwd" :loading="loading">确认修改</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, changePassword } from '../../api/auth'

const router = useRouter()
const loading = ref(false)
const showChangePwd = ref(false)
const form = ref({ username: '', password: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '' })

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await login(form.value)
    sessionStorage.setItem('merchantUser', JSON.stringify(res.data))
    if (res.data.mustChangePassword === 1) {
      pwdForm.value.oldPassword = form.value.password
      showChangePwd.value = true
    } else {
      router.push('/m/dashboard')
    }
  } finally {
    loading.value = false
  }
}

const handleChangePwd = async () => {
  loading.value = true
  try {
    await changePassword(pwdForm.value)
    ElMessage.success('密码修改成功')
    showChangePwd.value = false
    router.push('/m/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f5f5;
}
.login-card {
  width: 400px;
}
</style>
```

- [ ] **Step 2: 创建 Layout.vue（商户端侧边栏布局）**

```vue
<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div style="color: #fff; text-align: center; padding: 20px 0; font-size: 16px">
        铜锅涮肉
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/m/dashboard">
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/m/dishes">
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/m/tables">
          <span>桌台管理</span>
        </el-menu-item>
        <el-menu-item index="/m/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/m/sessions">
          <span>会话结账</span>
        </el-menu-item>
        <el-menu-item index="/m/manual-order">
          <span>手动下单</span>
        </el-menu-item>
        <el-menu-item index="/m/customers">
          <span>客户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex; justify-content:flex-end; align-items:center; background:#fff; border-bottom:1px solid #eee">
        <el-button @click="handleLogout" text>退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { logout } from '../../api/auth'

const router = useRouter()
const handleLogout = async () => {
  await logout()
  sessionStorage.removeItem('merchantUser')
  router.push('/m/login')
}
</script>
```

- [ ] **Step 3: 运行 `npm run dev` 验证登录流程**

Run: `cd frontend && npm run dev`
Expected: 访问 `/m/login` 可看到登录页面，输入 admin/admin 后弹出改密弹窗

- [ ] **Step 4: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端登录页 + 侧边栏布局"
```

---

### Task 14: 商户端 — 菜品管理页

**Files:**
- Create: `frontend/src/views/merchant/Dishes.vue`
- Create: `frontend/src/components/DishFormDialog.vue`

- [ ] **Step 1: 创建 DishFormDialog.vue（菜品编辑弹窗组件）**

```vue
<template>
  <el-dialog :model-value="visible" :title="isEdit ? '编辑菜品' : '新增菜品'" @close="handleClose" width="500px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="菜品名">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="价格(元)">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="form.stock" :min="0" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>
      <el-form-item label="图片">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          accept="image/*"
        >
          <img v-if="form.image" :src="form.image" style="width:100px;height:100px;object-fit:cover" />
          <el-button v-else size="small">上传图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addDish, updateDish } from '../../api/dish'
import { uploadImage } from '../../api/upload'

const props = defineProps({
  visible: Boolean,
  dish: Object,
  categories: Array
})
const emit = defineEmits(['update:visible', 'saved'])

const loading = ref(false)
const isEdit = ref(false)
const form = ref({})

watch(() => props.dish, (val) => {
  if (val) {
    isEdit.value = true
    form.value = { ...val, price: val.price / 100 } // 分→元
  } else {
    isEdit.value = false
    form.value = { categoryId: null, name: '', price: 0, stock: 0, sortOrder: 0, image: '', description: '' }
  }
}, { immediate: true })

const beforeUpload = (file) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、png、webp 格式')
    return false
  }
  return true
}

const handleUpload = async ({ file }) => {
  const res = await uploadImage(file)
  form.value.image = res.data
}

const handleSubmit = async () => {
  loading.value = true
  try {
    if (isEdit.value) {
      await updateDish({ ...form.value })
    } else {
      await addDish({ ...form.value })
    }
    ElMessage.success('保存成功')
    emit('saved')
    handleClose()
  } finally {
    loading.value = false
  }
}

const handleClose = () => emit('update:visible', false)
</script>
```

- [ ] **Step 2: 创建 Dishes.vue**

```vue
<template>
  <div>
    <!-- 分类管理 -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display:flex; justify-content:space-between">
              <span>分类管理</span>
              <el-button size="small" @click="showAddCategory = true">新增分类</el-button>
            </div>
          </template>
          <el-table :data="categories" size="small">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" text @click="editCategory(row)">编辑</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteCategory(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 新增/编辑分类弹窗 -->
      <el-dialog v-model="showAddCategory" :title="editingCategory ? '编辑分类' : '新增分类'" width="300px">
        <el-form>
          <el-form-item label="分类名">
            <el-input v-model="categoryForm.name" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="categoryForm.sortOrder" :min="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddCategory = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCategory">确定</el-button>
        </template>
      </el-dialog>
    </el-row>

    <!-- 菜品列表 -->
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>菜品管理</span>
          <el-button type="primary" @click="openDishForm(null)">新增菜品</el-button>
        </div>
      </template>
      <el-table :data="dishes">
        <el-table-column prop="name" label="名称" />
        <el-table-column label="价格(元)" width="100">
          <template #default="{ row }">{{ (row.price / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" text @click="openDishForm(row)">编辑</el-button>
            <el-button size="small" text @click="handleToggle(row.id)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" text @click="handleEditStock(row)">库存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 库存修改弹窗 -->
    <el-dialog v-model="showStockDialog" title="修改库存" width="300px">
      <el-input-number v-model="stockForm.stock" :min="0" style="width:100%" />
      <template #footer>
        <el-button @click="showStockDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveStock">确定</el-button>
      </template>
    </el-dialog>

    <DishFormDialog
      v-model:visible="showDishForm"
      :dish="editingDish"
      :categories="categories"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList as listDishes, toggleDish, updateStock } from '../../api/dish'
import { list as listCategories, add as addCategory, update as updateCategory, remove as removeCategory } from '../../api/category'
import DishFormDialog from '../../components/DishFormDialog.vue'

const categories = ref([])
const dishes = ref([])
const showAddCategory = ref(false)
const editingCategory = ref(null)
const categoryForm = ref({ name: '', sortOrder: 0 })
const showDishForm = ref(false)
const editingDish = ref(null)
const showStockDialog = ref(false)
const stockForm = ref({ id: null, stock: 0 })

const loadData = async () => {
  const [cRes, dRes] = await Promise.all([listCategories(), listDishes()])
  categories.value = cRes.data
  dishes.value = dRes.data
}

const editCategory = (row) => {
  editingCategory.value = row
  categoryForm.value = { name: row.name, sortOrder: row.sortOrder }
  showAddCategory.value = true
}

const handleSaveCategory = async () => {
  if (editingCategory.value) {
    await updateCategory({ id: editingCategory.value.id, ...categoryForm.value })
  } else {
    await addCategory(categoryForm.value)
  }
  ElMessage.success('保存成功')
  showAddCategory.value = false
  editingCategory.value = null
  categoryForm.value = { name: '', sortOrder: 0 }
  loadData()
}

const handleDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该分类？', '提示')
    await removeCategory(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消 */ }
}

const openDishForm = (dish) => {
  editingDish.value = dish
  showDishForm.value = true
}

const handleToggle = async (id) => {
  await toggleDish(id)
  loadData()
}

const handleEditStock = (row) => {
  stockForm.value = { id: row.id, stock: row.stock }
  showStockDialog.value = true
}

const handleSaveStock = async () => {
  await updateStock(stockForm.value)
  showStockDialog.value = false
  loadData()
}

onMounted(loadData)
</script>
```

- [ ] **Step 3: 验证页面渲染**

Run: `cd frontend && npm run dev`
Expected: 登录后进入菜品管理页，可新增/编辑分类和菜品

- [ ] **Step 4: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端菜品管理页（分类 + 菜品 CRUD + 图片上传）"
```

---

### Task 15: 商户端 — 桌台管理页

**Files:**
- Create: `frontend/src/views/merchant/Tables.vue`

- [ ] **Step 1: 创建 Tables.vue**

```vue
<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>桌台管理</span>
          <el-button type="primary" @click="openForm(null)">新增桌台</el-button>
        </div>
      </template>
      <el-table :data="tables">
        <el-table-column prop="name" label="桌号" />
        <el-table-column prop="area" label="区域" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'warning'">
              {{ row.status === 0 ? '空闲' : '使用中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" text @click="openForm(row)">编辑</el-button>
            <el-button size="small" text @click="showQR(row)">二维码</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showForm" :title="editing ? '编辑桌台' : '新增桌台'" width="400px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="桌号">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="form.area" style="width:100%">
            <el-option label="大厅" value="大厅" />
            <el-option label="包厢" value="包厢" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="showQRDialog" title="桌台二维码" width="350px">
      <div style="text-align:center">
        <p>{{ currentTable?.name }}</p>
        <img v-if="qrImage" :src="qrImage" style="width:250px" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { list, add, update, remove, qrcode } from '../../api/table'

const tables = ref([])
const showForm = ref(false)
const editing = ref(null)
const form = ref({ name: '', area: '大厅' })
const showQRDialog = ref(false)
const qrImage = ref('')
const currentTable = ref(null)

const loadData = async () => {
  const res = await list()
  tables.value = res.data
}

const openForm = (row) => {
  editing.value = row
  form.value = row ? { name: row.name, area: row.area } : { name: '', area: '大厅' }
  showForm.value = true
}

const handleSave = async () => {
  if (editing.value) {
    await update({ id: editing.value.id, ...form.value })
  } else {
    await add(form.value)
  }
  showForm.value = false
  ElMessage.success('保存成功')
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该桌台？', '提示')
    await remove(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消 */ }
}

const showQR = async (row) => {
  currentTable.value = row
  const res = await qrcode(row.id)
  qrImage.value = res.data.image
  showQRDialog.value = true
}

onMounted(loadData)
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端桌台管理页（CRUD + 二维码生成）"
```

---

### Task 16: 商户端 — 订单管理页

**Files:**
- Create: `frontend/src/views/merchant/Orders.vue`

- [ ] **Step 1: 创建 Orders.vue**

```vue
<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <span>订单管理</span>
          <div>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px; margin-right:10px" @change="loadOrders">
              <el-option label="待确认" :value="0" />
              <el-option label="制作中" :value="1" />
              <el-option label="部分上菜" :value="2" />
              <el-option label="全部上菜" :value="3" />
              <el-option label="已取消" :value="5" />
            </el-select>
          </div>
        </div>
      </template>

      <el-collapse v-model="expandedOrders">
        <el-collapse-item v-for="order in orders" :key="order.id" :name="order.id">
          <template #title>
            <div style="display:flex; justify-content:space-between; width:100%; padding-right:20px">
              <span>{{ order.orderNo }} - {{ statusText(order.status) }}</span>
              <span style="color:#999">{{ order.createTime }}</span>
            </div>
          </template>

          <!-- 操作按钮 -->
          <div style="margin-bottom:10px">
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handleConfirm(order.id)">
              确认订单
            </el-button>
            <el-button v-if="order.status !== 5 && order.status !== 4" type="danger" size="small" @click="handleCancel(order.id)">
              整单取消
            </el-button>
          </div>

          <!-- 菜品列表 -->
          <el-table :data="order.items" size="small">
            <el-table-column prop="dishName" label="菜品" />
            <el-table-column label="单价(元)" width="80">
              <template #default="{ row }">{{ (row.dishPrice / 100).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="60" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="itemStatusType(row.status)">{{ itemStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" size="small" text type="success" @click="handleServe(row.id)">
                  上菜
                </el-button>
                <el-button v-if="row.status === 0 || row.status === 1" size="small" text type="danger" @click="handleCancelItem(row.id)">
                  退菜
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantList, confirm, serveItem, cancelItem, cancelOrder, getItems } from '../../api/order'

const orders = ref([])
const filterStatus = ref(null)
const expandedOrders = ref([])

const statusText = (s) => ['待确认','制作中','部分上菜','全部上菜','已结账','已取消'][s] || ''
const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','','success','warning','danger'][s] || ''

const loadOrders = async () => {
  const res = await merchantList({ status: filterStatus.value })
  orders.value = res.data
  // 后端 listOrders 已批量返回 items，无需逐个加载
}

const handleConfirm = async (id) => {
  await confirm(id)
  ElMessage.success('订单已确认')
  loadOrders()
}

const handleServe = async (itemId) => {
  await serveItem(itemId)
  ElMessage.success('已上菜')
  loadOrders()
}

const handleCancelItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定退该菜品？', '提示')
    await cancelItem(itemId)
    ElMessage.success('已退菜')
    loadOrders()
  } catch (e) { /* 取消 */ }
}

const handleCancel = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定取消整单？', '提示')
    await cancelOrder(orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (e) { /* 取消 */ }
}

onMounted(loadOrders)
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端订单管理页（确认/上菜/退菜/取消）"
```

---

### Task 17: 商户端 — 会话结账页

**Files:**
- Create: `frontend/src/views/merchant/Sessions.vue`

- [ ] **Step 1: 创建 Sessions.vue**

```vue
<template>
  <div>
    <el-card>
      <template #header>
        <span>会话结账</span>
      </template>
      <el-row :gutter="16">
        <el-col v-for="table in busyTables" :key="table.id" :span="6" style="margin-bottom:16px">
          <el-card shadow="hover" @click="openSession(table)" style="cursor:pointer">
            <div style="text-align:center">
              <h3>{{ table.name }}</h3>
              <el-tag type="warning">{{ table.area }} - 使用中</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结账弹窗 -->
    <el-dialog v-model="showCheckout" title="整桌结账" width="500px">
      <div v-if="sessionDetail">
        <p>应结总额：<strong style="color:#f56c6c">{{ (sessionDetail.totalAmount / 100).toFixed(2) }} 元</strong></p>

        <el-descriptions title="订单明细" :column="1" border size="small" style="margin-top:10px">
          <template v-for="order in sessionDetail.orders" :key="order.id">
            <el-descriptions-item :label="order.orderNo">
              <span v-for="item in order._items" :key="item.id" style="margin-right:10px">
                {{ item.dishName }} x{{ item.quantity }}
                <el-tag size="small" :type="itemStatusType(item.status)">{{ itemStatusText(item.status) }}</el-tag>
              </span>
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <el-form :model="checkoutForm" label-width="100px" style="margin-top:20px">
          <el-form-item label="实收金额(元)">
            <el-input-number v-model="checkoutForm.actualPaid" :min="0" :precision="2" :step="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="手机号(可选)">
            <el-input v-model="checkoutForm.phone" placeholder="填写手机号累加积分" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button type="primary" @click="handleCheckout" :loading="loading">确认结账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { current, detail, checkout } from '../../api/session'
import { getItems } from '../../api/order'

const busyTables = ref([])
const showCheckout = ref(false)
const sessionDetail = ref(null)
const checkoutForm = ref({ actualPaid: 0, phone: '' })
const loading = ref(false)
const currentTableId = ref(null)

const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','','success','warning','danger'][s] || ''

const loadTables = async () => {
  const res = await listTables()
  busyTables.value = res.data.filter(t => t.status === 1)
}

const openSession = async (table) => {
  currentTableId.value = table.id
  // 查找该桌当前 session
  const sessRes = await current(table.id)
  if (!sessRes.data) {
    ElMessage.warning('该桌无进行中会话')
    return
  }
  const detailRes = await detail(sessRes.data.id)
  sessionDetail.value = detailRes.data

  // 加载每个订单的 items
  for (const order of sessionDetail.value.orders) {
    const itemRes = await getItems(order.id)
    order._items = itemRes.data
  }

  checkoutForm.value.actualPaid = sessionDetail.value.totalAmount / 100
  checkoutForm.value.phone = ''
  showCheckout.value = true
}

const handleCheckout = async () => {
  loading.value = true
  try {
    const sessRes = await current(currentTableId.value)
    await checkout(sessRes.data.id, checkoutForm.value)
    ElMessage.success('结账成功')
    showCheckout.value = false
    loadTables()
  } finally {
    loading.value = false
  }
}

onMounted(loadTables)
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端会话结账页（整桌结账 + 积分）"
```

---

### Task 18: 商户端 — 客户管理 + 工作台 + 手动下单

**Files:**
- Create: `frontend/src/views/merchant/Customers.vue`
- Create: `frontend/src/views/merchant/Dashboard.vue`
- Create: `frontend/src/views/merchant/ManualOrder.vue`

- [ ] **Step 1: 创建 Dashboard.vue**

```vue
<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日订单" :value="data.todayOrders || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日营收(元)" :value="(data.todayRevenue || 0) / 100" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="空闲桌台" :value="data.freeTables || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="进行中会话" :value="data.activeSessions || 0" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getData } from '../../api/dashboard'

const data = ref({})
onMounted(async () => {
  const res = await getData()
  data.value = res.data
})
</script>
```

- [ ] **Step 2: 创建 Customers.vue**

```vue
<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between">
          <span>客户管理</span>
          <el-input v-model="keyword" placeholder="搜索手机号/名称" style="width:200px" @keyup.enter="loadCustomers" clearable>
            <template #append>
              <el-button @click="loadCustomers">搜索</el-button>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="customers">
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="积分" width="100">
          <template #default="{ row }">{{ row.points }}</template>
        </el-table-column>
        <el-table-column label="累计消费(元)" width="120">
          <template #default="{ row }">{{ (row.totalSpent / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" text @click="openDetail(row)">详情</el-button>
            <el-button size="small" text @click="openPoints(row)">积分</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 客户详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="客户详情" width="500px">
      <template v-if="detailData">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="手机号">{{ detailData.customer?.phone }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detailData.customer?.name }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ detailData.customer?.points }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">{{ ((detailData.customer?.totalSpent || 0) / 100).toFixed(2) }} 元</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 15px 0 10px">积分明细</h4>
        <el-table :data="detailData.pointsRecords || []" size="small" max-height="300">
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="积分变动" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.points > 0 ? '#67c23a' : '#f56c6c' }">
                {{ row.points > 0 ? '+' : '' }}{{ row.points }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160" />
        </el-table>
      </template>
    </el-dialog>

    <!-- 积分操作弹窗 -->
    <el-dialog v-model="showPointsDialog" title="手动调整积分" width="400px">
      <p>客户：{{ currentCustomer?.name }}（当前积分：{{ currentCustomer?.points }}）</p>
      <el-input-number v-model="pointsForm.points" style="width:100%; margin-top:10px" />
      <p style="color:#999; margin-top:5px">正数为增加，负数为扣减</p>
      <el-input v-model="pointsForm.remark" placeholder="备注原因" style="margin-top:10px" />
      <template #footer>
        <el-button @click="showPointsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSavePoints">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantList, merchantDetail, manualPoints } from '../../api/customer'

const customers = ref([])
const keyword = ref('')
const showPointsDialog = ref(false)
const showDetailDialog = ref(false)
const detailData = ref(null)
const currentCustomer = ref(null)
const pointsForm = ref({ customerId: null, points: 0, remark: '' })

const loadCustomers = async () => {
  const res = await merchantList({ keyword: keyword.value })
  customers.value = res.data
}

const openDetail = async (row) => {
  const res = await merchantDetail(row.id)
  detailData.value = res.data
  showDetailDialog.value = true
}

const openPoints = (row) => {
  currentCustomer.value = row
  pointsForm.value = { customerId: row.id, points: 0, remark: '' }
  showPointsDialog.value = true
}

const handleSavePoints = async () => {
  await manualPoints(pointsForm.value)
  ElMessage.success('积分调整成功')
  showPointsDialog.value = false
  loadCustomers()
}

onMounted(loadCustomers)
</script>
```

- [ ] **Step 3: 创建 ManualOrder.vue**

```vue
<template>
  <div>
    <el-card>
      <template #header><span>手动下单</span></template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="桌台">
          <el-select v-model="form.tableId" placeholder="选择桌台（散客留空）" clearable style="width:300px">
            <el-option v-for="t in tables" :key="t.id" :label="t.name + ' (' + t.area + ')'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="客户手机号（可选）" style="width:300px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" style="width:300px" />
        </el-form-item>
      </el-form>

      <!-- 菜品选择 -->
      <el-divider>选择菜品</el-divider>
      <el-row :gutter="12">
        <el-col v-for="dish in dishes" :key="dish.id" :span="6" style="margin-bottom:12px">
          <el-card shadow="hover" body-style="padding:10px">
            <div>{{ dish.name }}</div>
            <div style="color:#f56c6c; font-size:14px">{{ (dish.price / 100).toFixed(2) }}元</div>
            <div style="margin-top:5px">
              <el-input-number v-model="cart[dish.id]" :min="0" :max="dish.stock" size="small" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider />
      <el-button type="primary" @click="handleSubmit" :loading="loading">提交订单</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { list as listTables } from '../../api/table'
import { merchantList as listDishes } from '../../api/dish'
import { merchantCreate } from '../../api/order'

const tables = ref([])
const dishes = ref([])
const cart = reactive({})
const loading = ref(false)
const form = ref({ tableId: null, phone: '', remark: '' })

const loadData = async () => {
  const [tRes, dRes] = await Promise.all([listTables(), listDishes()])
  tables.value = tRes.data
  // 商户端 /api/m/dish/list 返回全部菜品数组，过滤仅展示上架菜品
  dishes.value = (dRes.data || []).filter(d => d.status === 1)
}

const handleSubmit = async () => {
  const items = []
  for (const [dishId, qty] of Object.entries(cart)) {
    if (qty > 0) items.push({ dishId: Number(dishId), quantity: qty })
  }
  if (items.length === 0) {
    ElMessage.warning('请选择菜品')
    return
  }

  loading.value = true
  try {
    await merchantCreate({
      tableId: form.value.tableId || null,
      items,
      phone: form.value.phone || null,
      remark: form.value.remark || null
    })
    ElMessage.success('下单成功')
    // 清空购物车
    Object.keys(cart).forEach(k => delete cart[k])
    form.value.remark = ''
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
```

- [ ] **Step 4: 提交**

```bash
git add frontend/
git commit -m "feat: 商户端工作台 + 客户管理 + 手动下单页面"
```

---

## Phase 5: 用户端前端

### Task 19: 用户端 — 登录页

**Files:**
- Create: `frontend/src/views/customer/Login.vue`

- [ ] **Step 1: 创建 Login.vue（移动端适配）**

```vue
<template>
  <div class="customer-page">
    <div class="login-box">
      <h2 style="text-align:center; margin-bottom:20px; color:#333">铜锅涮肉</h2>
      <p style="text-align:center; color:#999; margin-bottom:30px">扫码点餐</p>

      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="phone" placeholder="输入手机号（可选）" size="large" maxlength="11" clearable />
        </el-form-item>
        <el-button type="danger" size="large" style="width:100%; margin-top:10px" native-type="submit">
          进入点餐
        </el-button>
        <p style="text-align:center; color:#999; margin-top:15px; font-size:12px">
          不填手机号可直接点餐，但无法查看积分和消费记录
        </p>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customerLogin } from '../../api/customer'

const route = useRoute()
const router = useRouter()
const phone = ref('')

const handleLogin = async () => {
  if (phone.value) {
    await customerLogin({ phone: phone.value })
    localStorage.setItem('customerPhone', phone.value)
  }
  localStorage.setItem('currentTableId', route.params.tableId)
  router.push('/c/order/' + route.params.tableId)
}
</script>

<style scoped>
.customer-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 60px 20px;
}
.login-box {
  max-width: 400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 30px 20px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 用户端登录页（手机号可选）"
```

---

### Task 20: 用户端 — 点餐页（菜单 + 购物车）

**Files:**
- Create: `frontend/src/views/customer/Order.vue`

- [ ] **Step 1: 创建 Order.vue（移动端核心页面）**

```vue
<template>
  <div class="customer-page">
    <!-- 分类标签 -->
    <div class="category-tabs">
      <span
        v-for="cat in categories" :key="cat.id"
        :class="['tab-item', { active: activeCategory === cat.id }]"
        @click="activeCategory = cat.id"
      >{{ cat.name }}</span>
    </div>

    <!-- 菜品列表 -->
    <div class="dish-list">
      <div v-for="dish in filteredDishes" :key="dish.id" class="dish-card">
        <img v-if="dish.image" :src="dish.image" class="dish-img" />
        <div class="dish-info">
          <div class="dish-name">{{ dish.name }}</div>
          <div class="dish-desc">{{ dish.description }}</div>
          <div class="dish-bottom">
            <span class="dish-price">&yen;{{ (dish.price / 100).toFixed(2) }}</span>
            <div class="dish-qty">
              <el-button v-if="cart[dish.id]" size="small" circle @click="changeQty(dish.id, -1)">-</el-button>
              <span v-if="cart[dish.id]" class="qty-num">{{ cart[dish.id] }}</span>
              <el-button size="small" circle type="danger" @click="changeQty(dish.id, 1)">+</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部购物车 -->
    <div class="cart-bar">
      <div class="cart-info" @click="showCartDetail = !showCartDetail">
        <el-badge :value="totalCount" :hidden="totalCount === 0" type="danger">
          <el-icon size="28"><ShoppingCart /></el-icon>
        </el-badge>
        <span class="cart-total">&yen;{{ (totalPrice / 100).toFixed(2) }}</span>
      </div>
      <el-button type="danger" size="large" :disabled="totalCount === 0" @click="handleSubmit" :loading="submitting">
        提交订单
      </el-button>
    </div>

    <!-- 购物车详情弹窗 -->
    <el-drawer v-model="showCartDetail" title="购物车" direction="btt" size="50%">
      <div v-for="item in cartItems" :key="item.dishId" style="display:flex; justify-content:space-between; padding:10px 0; border-bottom:1px solid #eee">
        <span>{{ item.name }}</span>
        <div>
          <el-button size="small" circle @click="changeQty(item.dishId, -1)">-</el-button>
          <span style="margin:0 8px">{{ item.qty }}</span>
          <el-button size="small" circle @click="changeQty(item.dishId, 1)">+</el-button>
          <span style="margin-left:10px; color:#f56c6c">&yen;{{ (item.price * item.qty / 100).toFixed(2) }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { customerList } from '../../api/dish'
import { customerCreate } from '../../api/order'

const route = useRoute()
const router = useRouter()
const tableId = computed(() => route.params.tableId)

const categories = ref([{ id: 0, name: '全部' }])
const dishes = ref([])
const activeCategory = ref(0)
const cart = reactive({})
const submitting = ref(false)
const showCartDetail = ref(false)

const filteredDishes = computed(() => {
  if (activeCategory.value === 0) return dishes.value
  return dishes.value.filter(d => d.categoryId === activeCategory.value)
})

const cartItems = computed(() => {
  return Object.entries(cart)
    .filter(([, qty]) => qty > 0)
    .map(([dishId, qty]) => {
      const dish = dishes.value.find(d => d.id === Number(dishId))
      return { dishId: Number(dishId), name: dish?.name, price: dish?.price || 0, qty }
    })
})

const totalCount = computed(() => Object.values(cart).reduce((s, q) => s + q, 0))
const totalPrice = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.qty, 0))

const changeQty = (dishId, delta) => {
  const newVal = (cart[dishId] || 0) + delta
  if (newVal < 0) return
  cart[dishId] = newVal
}

const loadData = async () => {
  const res = await customerList()
  dishes.value = res.data.dishes || res.data
  // 使用后端返回的分类数据
  const cats = res.data.categories || []
  categories.value = [{ id: 0, name: '全部' }, ...cats]
}

const handleSubmit = async () => {
  const items = cartItems.value.map(i => ({ dishId: i.dishId, quantity: i.qty }))
  if (items.length === 0) return

  submitting.value = true
  try {
    const phone = localStorage.getItem('customerPhone') || null
    await customerCreate({
      tableId: Number(tableId.value),
      items,
      phone,
      remark: ''
    })
    ElMessage.success('下单成功')
    Object.keys(cart).forEach(k => delete cart[k])
    router.push('/c/status/' + tableId.value)
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 70px; }
.category-tabs {
  display: flex; overflow-x: auto; background: #fff; padding: 10px;
  position: sticky; top: 0; z-index: 10; gap: 10px;
}
.tab-item { white-space: nowrap; padding: 6px 16px; border-radius: 20px; font-size: 14px; color: #666; cursor: pointer; }
.tab-item.active { background: #f56c6c; color: #fff; }
.dish-list { padding: 10px; }
.dish-card { display: flex; background: #fff; border-radius: 8px; margin-bottom: 10px; overflow: hidden; }
.dish-img { width: 100px; height: 100px; object-fit: cover; }
.dish-info { flex: 1; padding: 10px; display: flex; flex-direction: column; justify-content: space-between; }
.dish-name { font-size: 16px; font-weight: 500; }
.dish-desc { font-size: 12px; color: #999; }
.dish-bottom { display: flex; justify-content: space-between; align-items: center; }
.dish-price { color: #f56c6c; font-size: 16px; font-weight: 500; }
.dish-qty { display: flex; align-items: center; gap: 6px; }
.cart-bar {
  position: fixed; bottom: 0; left: 0; right: 0; height: 60px;
  background: #333; display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; z-index: 100;
}
.cart-info { display: flex; align-items: center; gap: 10px; color: #fff; cursor: pointer; }
.cart-total { font-size: 18px; font-weight: 500; color: #fff; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 用户端点餐页（菜单浏览 + 购物车 + 下单）"
```

---

### Task 21: 用户端 — 订单状态页

**Files:**
- Create: `frontend/src/views/customer/Status.vue`

- [ ] **Step 1: 创建 Status.vue**

```vue
<template>
  <div class="customer-page">
    <h2 style="text-align:center; padding:15px 0; background:#fff">上菜状态</h2>

    <div v-if="orders.length === 0" style="text-align:center; padding:40px; color:#999">
      暂无订单
    </div>

    <div v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span>{{ order.orderNo }}</span>
        <el-tag size="small">{{ orderStatusText(order.status) }}</el-tag>
      </div>

      <div v-for="item in order.items" :key="item.id" class="item-row">
        <span class="item-name">{{ item.dishName }}</span>
        <span class="item-qty">x{{ item.quantity }}</span>
        <el-tag size="small" :type="itemStatusType(item.status)">{{ itemStatusText(item.status) }}</el-tag>
      </div>
    </div>

    <div style="text-align:center; padding:20px">
      <el-button type="danger" @click="$router.push('/c/order/' + $route.params.tableId)">继续点餐</el-button>
      <el-button @click="$router.push('/c/mine')">我的</el-button>
    </div>

    <!-- 自动刷新 -->
    <div style="text-align:center; color:#999; font-size:12px; padding:10px">
      每 10 秒自动刷新
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { tableOrders } from '../../api/order'

const route = useRoute()
const orders = ref([])
let timer = null

const orderStatusText = (s) => ['待确认','制作中','部分上菜','全部上菜','已结账','已取消'][s] || ''
const itemStatusText = (s) => ['待确认','待上菜','已上菜','库存不足','已退菜'][s] || ''
const itemStatusType = (s) => ['info','warning','success','danger','info'][s] || ''

const loadData = async () => {
  try {
    const res = await tableOrders(route.params.tableId)
    orders.value = res.data
  } catch (e) { /* 忽略 */ }
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; }
.order-card { background: #fff; margin: 10px; border-radius: 8px; padding: 15px; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 10px; font-weight: 500; }
.item-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.item-name { flex: 1; }
.item-qty { width: 40px; text-align: center; color: #666; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 用户端订单状态页（自动刷新 + 上菜状态）"
```

---

### Task 22: 用户端 — 我的（积分/消费记录）

**Files:**
- Create: `frontend/src/views/customer/Mine.vue`

- [ ] **Step 1: 创建 Mine.vue**

```vue
<template>
  <div class="customer-page">
    <!-- 用户信息 -->
    <div class="user-card">
      <h3>{{ info.name || '未登录' }}</h3>
      <p v-if="info.phone">{{ info.phone }}</p>
      <div class="stats">
        <div class="stat-item">
          <div class="stat-val">{{ info.points || 0 }}</div>
          <div class="stat-label">积分</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">{{ ((info.totalSpent || 0) / 100).toFixed(2) }}</div>
          <div class="stat-label">累计消费(元)</div>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" style="background:#fff; margin-top:10px">
      <el-tab-pane label="消费记录" name="orders">
        <div v-for="order in orders" :key="order.id" class="order-item">
          <div style="display:flex; justify-content:space-between">
            <span>{{ order.orderNo }}</span>
            <span style="color:#f56c6c">&yen;{{ (order.totalAmount / 100).toFixed(2) }}</span>
          </div>
          <div style="color:#999; font-size:12px; margin-top:4px">{{ order.createTime }}</div>
        </div>
        <div v-if="orders.length === 0" style="text-align:center; padding:30px; color:#999">暂无消费记录</div>
      </el-tab-pane>

      <el-tab-pane label="积分明细" name="points">
        <div v-for="pr in pointsRecords" :key="pr.id" class="order-item">
          <div style="display:flex; justify-content:space-between">
            <span>{{ pr.remark }}</span>
            <span :style="{ color: pr.points > 0 ? '#67c23a' : '#f56c6c' }">
              {{ pr.points > 0 ? '+' : '' }}{{ pr.points }}
            </span>
          </div>
          <div style="color:#999; font-size:12px; margin-top:4px">{{ pr.createTime }}</div>
        </div>
        <div v-if="pointsRecords.length === 0" style="text-align:center; padding:30px; color:#999">暂无积分记录</div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { customerInfo, customerOrders, customerPoints } from '../../api/customer'

const info = ref({})
const orders = ref([])
const pointsRecords = ref([])
const activeTab = ref('orders')

const loadData = async () => {
  try {
    const [infoRes, ordersRes, pointsRes] = await Promise.all([
      customerInfo(),
      customerOrders(),
      customerPoints()
    ])
    info.value = infoRes.data || {}
    orders.value = ordersRes.data || []
    pointsRecords.value = pointsRes.data || []
  } catch (e) { /* 未登录等情况 */ }
}

onMounted(loadData)
</script>

<style scoped>
.customer-page { min-height: 100vh; background: #f5f5f5; }
.user-card {
  background: linear-gradient(135deg, #f56c6c, #e6393d);
  color: #fff; padding: 30px 20px; text-align: center;
}
.user-card h3 { margin: 0 0 5px; }
.user-card p { margin: 0; opacity: 0.8; font-size: 14px; }
.stats { display: flex; justify-content: center; gap: 40px; margin-top: 20px; }
.stat-item { text-align: center; }
.stat-val { font-size: 24px; font-weight: 500; }
.stat-label { font-size: 12px; opacity: 0.8; margin-top: 4px; }
.order-item { padding: 12px 15px; border-bottom: 1px solid #f5f5f5; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/
git commit -m "feat: 用户端我的页面（积分 + 消费记录 + 积分明细）"
```

---

## Phase 6: 集成与打包

### Task 23: 前端构建 + 后端打包 + 联调

**Files:**
- Modify: `frontend/vite.config.js`（确认 build 输出路径）
- Modify: `backend/src/main/resources/application.yml`（调整 SQL 初始化策略）

- [ ] **Step 1: 修改 application.yml 的 SQL 初始化策略**

SQLite 使用 `IF NOT EXISTS` 创建表，但 `data.sql` 的 `INSERT OR IGNORE` 每次启动都会执行。改为仅首次初始化：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
```

schema.sql 和 data.sql 都使用 `IF NOT EXISTS` 和 `INSERT OR IGNORE`，可安全重复执行。

- [ ] **Step 2: 确认 vite.config.js 的 build 输出到后端 static**

```javascript
build: {
  outDir: '../backend/src/main/resources/static',
  emptyOutDir: true
}
```

- [ ] **Step 3: 构建前端**

Run: `cd frontend && npm run build`
Expected: 构建成功，文件输出到 `backend/src/main/resources/static/`

- [ ] **Step 4: 打包后端 JAR**

Run: `cd backend && mvn clean package -DskipTests`
Expected: 生成 `backend/target/tongguo.jar`

- [ ] **Step 5: 启动测试**

Run: `cd backend/target && java -jar tongguo.jar`

验证：
1. 访问 `http://localhost:8080/m/login` → 商户端登录页
2. 登录 admin/admin → 强制改密 → 进入工作台
3. 新增分类、菜品、桌台
4. 访问 `http://localhost:8080/c/login/1` → 用户端登录
5. 点餐、提交订单
6. 商户端确认、上菜、结账

- [ ] **Step 6: 提交**

```bash
git add .
git commit -m "feat: 前后端集成打包 + 联调验证"
```

---

## 实施计划自检

### 规格覆盖检查

| 设计文档章节 | 对应 Task |
|---|---|
| 技术栈 + 项目结构 | Task 1, 12 |
| 商户端鉴权 | Task 3, 4, 13 |
| 用户端身份（X-Phone） | Task 7, 12 |
| merchant_user 表 | Task 2 |
| dish_category + dish 表 | Task 2, 5 |
| table_info 表 | Task 2, 6 |
| dining_session 表 | Task 2, 8 |
| session_checkout 表 | Task 2, 8 |
| customer 表 | Task 2, 7 |
| orders + order_item 表 | Task 2, 9 |
| points_record 表 | Task 2, 7, 8 |
| 金额精度规则 | Task 5 (yuanToFen), Task 8 (checkout) |
| 图片上传 | Task 3 |
| 就餐会话规则 | Task 8 |
| 库存扣减规则 | Task 10 |
| 订单状态规则 | Task 10 (refreshOrderStatus) |
| 分类/桌台管理规则 | Task 5, 6 |
| 整桌结账规则 | Task 8 (checkout 事务) |
| 商户端全部页面 | Task 13-18 |
| 用户端全部页面 | Task 19-22 |
| 积分规则 | Task 7, 8 |
| API 全部端点 | Task 4-11 |

### 占位符扫描

无 TBD、TODO、implement later 等占位符。

### 类型一致性

- 金额字段统一使用 `Integer`（分），前端展示转为元
- Entity 字段名与数据库列名通过 `@TableField` 映射一致
- API 路径与设计文档完全对应
