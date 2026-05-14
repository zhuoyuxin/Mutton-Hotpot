# 铜锅涮肉餐饮管理系统 - 设计文档

## 概述

为铜锅涮肉小店开发一套点餐管理系统，包含商户端（平板/手机操作）和用户端（手机扫码点餐）。单体应用架构，前后端分离开发，最终打包为一个 JAR 部署。

## 技术栈

- **前端**：Vue 3 + Element Plus + Vite + Vue Router + Axios
- **后端**：Spring Boot 2.x + MyBatis-Plus + Java 8
- **数据库**：SQLite（单文件，轻量部署）
- **部署**：前端打包放入 Spring Boot static 目录，`java -jar app.jar` 一键启动
- **本地文件**：运行时上传文件存放在 `app.jar` 同级 `./uploads/` 目录，通过 Spring ResourceHandler 映射为 `/uploads/**`

## 项目结构

```
铜锅涮肉/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/tongguo/
│   │   ├── controller/               # API 接口
│   │   │   ├── AuthController        # 商户登录/登出/改密
│   │   │   ├── MerchantController    # 商户端接口（工作台数据）
│   │   │   ├── CustomerController    # 用户端接口（登录、积分、消费记录）
│   │   │   ├── DishController        # 菜品管理（含分类）
│   │   │   ├── TableController       # 桌台管理（含二维码生成）
│   │   │   ├── OrderController       # 订单管理（下单、确认、上菜、退菜）
│   │   │   └── SessionController     # 就餐会话（当前会话、整桌结账）
│   │   ├── service/                  # 业务逻辑
│   │   ├── mapper/                   # MyBatis-Plus Mapper
│   │   ├── entity/                   # 数据实体
│   │   ├── config/                   # 配置类（跨域、SQLite 等）
│   │   └── TongguoApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── schema.sql                # 建表脚本
│   │   └── static/                   # 前端打包后放这里
│   └── pom.xml
│
└── frontend/                         # Vue 3 前端
    ├── src/
    │   ├── views/
    │   │   ├── merchant/             # 商户端页面
    │   │   └── customer/             # 用户端页面
    │   ├── router/
    │   ├── api/                      # axios 请求封装
    │   ├── components/
    │   └── App.vue
    ├── package.json
    └── vite.config.js
```

**路由规则：**
- `/m/*` — 商户端（平板操作）
- `/c/*` — 用户端（手机扫码进入）

## 身份与鉴权

### 商户端鉴权

- 商户端需要登录才能操作，登录方式：用户名 + 密码
- 系统预置一个管理员账号（admin/admin），首次登录后强制修改密码
- 预置管理员账号通过 `must_change_password` 标记首次登录，登录成功后若该值为 1，前端强制跳转改密页
- 使用 Spring Session（基于 Cookie），商户端**所有接口（GET 和写操作）**均需校验登录状态
- 未登录请求返回 401，前端捕获后跳转登录页；用户端公开接口（菜品列表、下单、订单状态）不受影响
- **路径隔离方案**：商户端接口统一使用 `/api/m/**` 前缀，用户端接口使用 `/api/c/**` 前缀，鉴权拦截器仅拦截 `/api/m/**`（登录接口 `/api/m/auth/login` 除外）
- 这样同一业务（如菜品列表）有两套路径：`/api/c/dish/list`（用户端，无需鉴权）和 `/api/m/dish/list`（商户端，含下架菜品，需鉴权）
- 无需多角色权限，单店只有一个操作者身份

### 用户端身份

- 用户扫码后进入登录页，可选择输入手机号（无验证码，直接登录）
- **输入手机号**：可点单 + 查看消费记录 + 查看积分
- **不输手机号**：只能点单，看不到消费记录和积分
- 用户手机号通过 localStorage 存储，请求时放在 Header（X-Phone）中传递，后端无状态校验
- `/api/c/customer/info`、`/api/c/customer/orders`、`/api/c/customer/points` 缺失 `X-Phone` 时返回 `401 { code: 40101, message: "请先登录" }`，前端捕获后跳转登录页

## 数据库设计

### merchant_user（商户账号）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| username | TEXT UNIQUE | 用户名 |
| password | TEXT | 密码（BCrypt 加密存储） |
| must_change_password | INTEGER | 是否首次登录必须改密（1是 0否） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

系统启动时自动检查，若 merchant_user 表为空则插入默认账号 admin/admin，且 `must_change_password=1`。

### dish_category（菜品分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| name | TEXT | 分类名（肉类、蔬菜、锅底、酒水等） |
| sort_order | INTEGER | 排序序号 |
| create_time | DATETIME | 创建时间 |

### dish（菜品）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| category_id | INTEGER | 关联分类 |
| name | TEXT | 菜品名 |
| price | INTEGER | 价格（单位分；展示和录入时按元、最多两位小数） |
| image | TEXT | 图片访问路径（如 `/uploads/dish/202605/abc.jpg`，可为空） |
| description | TEXT | 描述（可为空） |
| status | INTEGER | 1上架 0下架 |
| stock | INTEGER | 库存数量 |
| sort_order | INTEGER | 排序序号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### table_info（桌台）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| name | TEXT | 桌号（如 A1、包厢1） |
| area | TEXT | 大厅/包厢 |
| status | INTEGER | 0空闲 1使用中 |
| create_time | DATETIME | 创建时间 |

### dining_session（就餐会话）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| table_id | INTEGER | 关联桌台（可为空，散客无桌号时为空） |
| status | INTEGER | 0进行中 1已结束 |
| create_time | DATETIME | 开台时间 |
| end_time | DATETIME | 结束时间（可为空） |

**约束规则：**
- 同一 `table_id` 任意时刻只能有一个 `status=0` 的进行中 session
- SQLite 建议建立部分唯一索引：`CREATE UNIQUE INDEX uniq_active_session_per_table ON dining_session(table_id) WHERE status = 0 AND table_id IS NOT NULL`
- 创建 session 时采用“先查进行中 session，查无再创建；若唯一索引冲突则回查复用”的事务模式，确保多人同时扫码时复用同一桌 session

### session_checkout（整桌结账）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| session_id | INTEGER UNIQUE | 关联就餐会话 |
| total_amount | INTEGER | 该 session 应结总额（单位分，按有效订单项实时汇总） |
| actual_paid | INTEGER | 整桌实收金额（单位分） |
| discount_amount | INTEGER | 整桌优惠/抹零金额（单位分） |
| customer_id | INTEGER | 积分归属客户（可为空） |
| points_earned | INTEGER | 本次获得积分 |
| checkout_time | DATETIME | 结账时间 |

### customer（客户）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| phone | TEXT UNIQUE | 手机号 |
| name | TEXT | 客户名称（默认为手机号） |
| points | INTEGER | 当前积分 |
| total_spent | INTEGER | 累计消费金额（单位分） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### orders（订单）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| order_no | TEXT UNIQUE | 订单号 |
| session_id | INTEGER | 关联就餐会话 |
| table_id | INTEGER | 关联桌台（可为空，散客无桌号时为空） |
| customer_id | INTEGER | 关联客户（可为空） |
| total_amount | INTEGER | 订单菜品总额（单位分，菜单价格之和） |
| status | INTEGER | 0待确认 1制作中 2部分上菜 3全部上菜 4已结账 5已取消 |
| remark | TEXT | 备注（可为空） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### order_item（订单项）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| order_id | INTEGER | 关联订单 |
| dish_id | INTEGER | 关联菜品 |
| dish_name | TEXT | 菜品名（下单时快照） |
| dish_price | INTEGER | 单价（单位分，下单时快照） |
| quantity | INTEGER | 数量 |
| status | INTEGER | 0待确认 1待上菜 2已上菜 3库存不足 4已退菜 |
| create_time | DATETIME | 创建时间 |

### points_record（积分记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER PK | 主键 |
| customer_id | INTEGER | 关联客户 |
| checkout_id | INTEGER | 关联整桌结账记录（可为空，手动加积分时） |
| points | INTEGER | 积分变动值（正数增加，负数扣减） |
| type | INTEGER | 0消费获得 1手动增加 2兑换扣减 |
| remark | TEXT | 备注 |
| create_time | DATETIME | 创建时间 |

**关键设计点：**
- orders.customer_id 可为空，支持不填手机号也能下单
- 同一桌支持多人同时扫码、各自提交独立订单，但这些订单共享同一个进行中的 session
- orders.session_id 关联就餐会话，桌台统计、上菜状态、整桌结账都以 session 聚合，避免串单
- 整桌结账以 session 为单位，结账信息独立保存在 session_checkout，订单只保留菜品明细与流程状态
- orders.total_amount 是下单时的原始金额快照，不直接作为整桌结账金额依据
- order_item 有独立 status，支持菜品级别的部分上菜
- dish_name/dish_price 快照到订单项，菜品改价不影响历史订单
- 客户 name 默认为手机号，商户可后续修改

## 金额精度规则

- 业务展示单位为**元**，用户输入和页面展示最多保留两位小数
- SQLite 落库统一使用**分**为单位的 `INTEGER` 字段，避免 `DECIMAL`/浮点精度问题
- 前后端接口保持语义上的“元”：
  - 请求参数如 `price`、`actualPaid` 使用元值，最多两位小数
  - 后端在入库前使用 `BigDecimal` 校验小数位数 `<= 2`，并按 `yuan * 100` 转为分
  - 响应给前端时再将分转换为元展示
- 金额计算（订单汇总、优惠差额、累计消费）全部在“分”这个整数单位上完成，最终展示时再格式化为元

## 本地图片上传规则

- 菜品图片采用**本地文件上传**，不接第三方对象存储
- 上传接口：`POST /api/m/upload/image`，`multipart/form-data`，字段名 `file`
- 文件写入 `app.jar` 同级 `./uploads/dish/YYYYMM/` 目录
- 后端通过 ResourceHandler 暴露 `/uploads/**` 静态访问路径，接口返回图片 URL，前端将其写入 `dish.image`
- v1 仅允许常见图片格式（jpg、jpeg、png、webp），非图片文件直接拒绝

## 就餐会话规则

- 用户扫码点餐时，系统检查该桌是否有"进行中"的 session：
  - **有** → 用户加入当前 session，可加单（同一批客人多轮点菜）
  - **没有** → 自动创建新 session（table_id 关联桌台），桌台状态变为"使用中"
- 商户手动下单时：
  - **指定桌台** → 同上，关联或创建该桌的 session
  - **散客无桌号** → 创建 table_id 为空的独立 session，不涉及桌台状态变更
- 同一桌多人并发扫码或并发下单时，必须复用同一个进行中的 session，不能创建第二个同桌进行中 session
- 商户结账以 session 为单位：
  - 一次性结清该 session 下全部未结账订单
  - 结账完成后 session 结束，若有桌台则恢复空闲
- 用户端 `/c/status/{tableId}` 只查当前 session 下未结账的订单

## 库存扣减规则

- **订单项状态流转**：
  - 用户提交 → order_item.status=0（待确认），**不扣库存**
  - 商户确认前移除某个菜品 → `status=0 -> 4`（已退菜），**不归还库存**
  - 商户确认订单 → 逐项尝试扣减库存：
    - 库存充足 → status=1（待上菜），**扣减库存**
    - 库存不足 → status=3（库存不足），**不扣减**，提示商户
  - 上菜 → status=2（已上菜）
  - 已确认未上菜项退菜 → `status=1 -> 4`（已退菜），**归还库存**
- **库存归还规则**（核心：只有 status=1 才归还，因为只有它扣过库存）：
  - **退单个菜品**：
    - `status=0` → 直接标记为 4，不归还库存
    - `status=1` → 归还库存（`stock += quantity`），item.status→4
    - `status=2/3/4` → 不允许调用 `cancel-item`
  - **整单取消**：所有 status=1（待上菜）的 item 归还库存并标记为 4；status=0（待确认）的直接标记为 4 不归还；status=2（已上菜）和 3（库存不足）不受影响
- **超卖防护**：SQLite 单写者模型天然串行，用 `UPDATE dish SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}` 做乐观扣减，返回影响行数为 0 则标记为库存不足

## 订单状态规则

- `0待确认`：订单刚提交，全部 `order_item.status=0`
- `1制作中`：订单已确认，存在至少一个 `order_item.status=1`，且尚无已上菜项
- `2部分上菜`：存在至少一个 `order_item.status=2`，且仍有 `order_item.status=1`
- `3全部上菜`：不存在 `order_item.status=1`，且至少一个 `order_item.status=2`；`status=3/4` 的项视为终态，不阻塞“全部上菜”
- 若订单内全部 `order_item` 最终都变为 `status=3`（库存不足）或 `status=4`（已退菜），且不存在已上菜项，则订单自动收口为 `5已取消`；若发生在确认前，则订单从 `0待确认` 直接收口为 `5已取消`
- `5已取消`：仅当订单内不存在 `order_item.status=2` 时允许整单取消；取消后所有未上菜有效项按库存规则处理
- `4已结账`：所属 session 完成整桌结账后，session 下全部未取消订单统一标记为已结账

## 分类与桌台管理规则

- 删除分类前必须检查该分类下是否仍有菜品；若存在菜品则拒绝删除，返回提示“请先移走该分类下的菜品”
- 删除桌台前必须检查该桌是否存在 `status=0` 的进行中 session；若存在则拒绝删除

## 整桌结账规则

- 商户在结账界面查看当前 session 下全部未结账订单及应结总额
- 应结总额计算规则：汇总该 session 下所有 `status != 5` 的订单中，`order_item.status IN (1, 2)` 的 `dish_price * quantity`
- 结账接口以 session 为单位：`PUT /api/m/session/checkout/{sessionId}`
- 默认一次性结清该 session 下全部 `status != 4 && status != 5` 的订单，不支持按订单拆分结账
- 若该 session 下仍存在 `status=0`（待确认）订单，系统不允许直接结账，商户需先确认或取消这些订单
- 商户可手动填写整桌实收金额（actual_paid），差额自动计算为 `discount_amount`
- 积分计算：整桌 `points_earned = floor(actual_paid / 100)`（1元=1积分，金额单位分），若结账时填写手机号则自动累加到该客户
- 用户端消费记录按各自提交的订单展示；积分和 `customer.total_spent` 仅累计到结账时填写的手机号
- 结账接口做幂等：若 session 已存在 `session_checkout` 记录，直接返回成功，不重复写积分
- **事务一致性**：整桌结账在同一个 `@Transactional` 中完成，包含：
  1. 校验 session 仍为进行中，并加载该 session 下全部可结账订单
  2. 按订单项实时计算 `total_amount`，计算 `points_earned = floor(actual_paid / 100)`，创建 `session_checkout`（`session_id`, `total_amount`, `actual_paid`, `discount_amount`, `points_earned`, `checkout_time`）
  3. 批量更新该 session 下全部未取消订单为 `status=4`（已结账）
  4. 若有手机号：查找或创建 customer 记录，更新 `session_checkout.customer_id`
  5. 若有手机号：写入 `points_record`，积分值为 `points_earned`
  6. 若有手机号：更新 `customer.points` 和 `customer.total_spent`
  7. 更新 `dining_session`（`status=1`, `end_time`），若有桌台则更新 `table_info.status=0`
  - 以上任一步失败则整体回滚，避免“session 已结账但订单/积分未同步”

## 页面设计

### 商户端（平板，路由 /m/*）

| 页面 | 路由 | 功能 |
|------|------|------|
| 工作台 | /m/dashboard | 今日订单数、营收、桌台状态一览 |
| 手动下单 | /m/manual-order | 为指定桌台代客下单、加菜，或开散客订单（无桌号） |
| 订单管理 | /m/orders | 订单列表（可按状态/桌台筛选），逐个菜品标记上菜、退菜 |
| 会话结账 | /m/sessions | 按桌查看当前 session，汇总多张订单并整桌结账 |
| 菜品管理 | /m/dishes | 分类管理 + 菜品增删改查、上下架、库存 |
| 桌台管理 | /m/tables | 桌台增删改、生成二维码、大厅/包厢分类 |
| 客户管理 | /m/customers | 按手机号/名称搜索，查看积分，手动加减积分 |

### 用户端（手机，路由 /c/*）

| 页面 | 路由 | 功能 |
|------|------|------|
| 手机号登录 | /c/login/{tableId} | 可选填手机号，跳过也可进入 |
| 点餐页 | /c/order/{tableId} | 菜品分类浏览，加入购物车，提交订单 |
| 订单状态 | /c/status/{tableId} | 当前桌台订单，查看哪些菜已上/未上 |
| 我的 | /c/mine | 需登录：积分余额、消费记录、积分明细 |

### 用户端流程

扫码(/c/login/{tableId}) → 可选输入手机号 → 进入点餐页 → 选择菜品提交订单 → 查看上菜状态

## API 设计

### 用户端接口（`/api/c/**`，无需鉴权）

```
POST   /api/c/auth/login             # 手机号登录（仅存手机号，无验证）
GET    /api/c/dish/list              # 获取上架菜品列表（按分类）
POST   /api/c/order/create           # 提交订单 {tableId, items[], phone?, remark?}，自动关联或创建 session
GET    /api/c/order/table/{tableId}  # 获取当前桌台当前 session 的进行中订单（可能包含多人提交的多张订单）
GET    /api/c/customer/info          # 获取当前客户信息（积分等，需手机号）
GET    /api/c/customer/orders        # 获取消费记录（需手机号）
GET    /api/c/customer/points        # 获取积分明细（需手机号）
```

### 商户端接口（`/api/m/**`，需鉴权，拦截器统一拦截 `/api/m/**` 排除 `/api/m/auth/login`）

```
# 登录
POST   /api/m/auth/login             # 登录 {username, password}，返回 mustChangePassword
POST   /api/m/auth/logout            # 登出
PUT    /api/m/auth/password          # 修改密码，成功后 must_change_password 置为 0
GET    /api/m/auth/info              # 获取当前登录信息（含 mustChangePassword）

# 工作台
GET    /api/m/dashboard              # 今日数据概览

# 菜品
GET    /api/m/dish/list              # 菜品列表（商户看全部含下架）
POST   /api/m/dish/add               # 新增菜品
PUT    /api/m/dish/update            # 修改菜品
PUT    /api/m/dish/toggle/{id}       # 上下架切换
PUT    /api/m/dish/stock             # 修改库存
POST   /api/m/upload/image           # 本地上传菜品图片，返回可访问路径

# 分类
GET    /api/m/category/list          # 分类列表
POST   /api/m/category/add           # 新增分类
PUT    /api/m/category/update        # 修改分类
DELETE /api/m/category/{id}          # 删除分类（若分类下仍有菜品则拒绝）

# 桌台
GET    /api/m/table/list             # 桌台列表
POST   /api/m/table/add              # 新增桌台
PUT    /api/m/table/update           # 修改桌台
DELETE /api/m/table/{id}             # 删除桌台（存在进行中 session 时拒绝）
GET    /api/m/table/qrcode/{id}      # 获取桌台二维码图片

# 订单
GET    /api/m/order/list             # 订单列表（支持状态筛选）
POST   /api/m/order/create           # 商户手动下单 {tableId?, sessionId?, items[], phone?, remark?}
PUT    /api/m/order/confirm/{id}     # 确认订单（开始制作，此时扣减库存）
PUT    /api/m/order/serve-item/{id}  # 标记某个菜品已上菜
PUT    /api/m/order/cancel-item/{id} # 退单个菜品（status=0 直接置4；status=1 归还库存后置4）
PUT    /api/m/order/cancel/{id}      # 整单取消（仅对已扣库存的项归还，item.status→4，order.status→5）

# 会话
GET    /api/m/session/current/{tableId} # 获取桌台当前进行中 session（含订单汇总）
GET    /api/m/session/detail/{id}       # 获取 session 详情（订单列表、菜品汇总、待结账金额）
PUT    /api/m/session/checkout/{id}     # 整桌结账 {actualPaid, phone?}，一次结清当前 session 全部未结账订单

# 客户
GET    /api/m/customer/list          # 客户列表（搜索）
GET    /api/m/customer/detail/{id}   # 客户详情（积分、消费记录）
POST   /api/m/customer/points        # 手动加减积分 {customerId, points, remark}
```

## 积分规则

- 整桌结账时按 `points_earned = floor(actual_paid / 100)` 计算积分，即 1 元 = 1 积分，不足 1 元的部分不计积分
- 如整桌结账时填了手机号，积分自动累加到该客户账户
- 结账接口幂等：同一 session 重复提交直接返回成功，不重复加积分
- 商户可手动给客户加减积分，并备注原因

## 单规格菜品

每个菜品只有一个价格和一份库存，不做多规格（大小份）支持。

## 单店模式

系统只服务一个商户，不需要多商户隔离。
