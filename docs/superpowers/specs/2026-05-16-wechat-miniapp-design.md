# 微信小程序改造设计

## 本次定位

这次不是把现有 `frontend/` 商家后台直接硬迁移到小程序，而是单独新增 `miniapp/` 原生微信小程序工程，优先覆盖最适合微信场景的顾客链路：

- 顾客扫码进入桌台
- 自动微信登录
- 浏览菜单并下单
- 查看当前桌台订单状态
- 查看会员积分与消费记录

商家后台继续保留 Web 端，避免把复杂表格和后台操作强行塞进小程序。

## 身份方案调整

原顾客端主要依赖手机号识别用户，但微信小程序直接获取手机号是收费能力，因此本次改为：

- 小程序前端使用 `wx.login`
- 后端调用微信 `code2Session`
- 用 `openid` 作为小程序顾客主身份
- 后端签发业务 token
- 后续请求通过 `X-Customer-Token` 鉴权

保留原有手机号链路作为兼容方案，方便旧 H5 顾客端继续使用。

## 数据模型调整

### customer 表

新增：

- `openid`

放宽：

- `phone` 从必填改为可空

原因：

- 微信登录顾客可能没有手机号
- `openid` 才是小程序内稳定身份标识

为了兼容已有 SQLite 数据，本次增加了启动时自动迁移逻辑。

## 页面结构

### 1. 入口页 `pages/entry`

- 读取 `tableId` 或 `scene`
- 展示桌台信息
- 自动发起微信登录
- 完成桌台上下文初始化

### 2. 菜单页 `pages/menu`

- 展示分类与搜索
- 菜品卡片化
- 固定底部购物袋
- 支持备注
- 下单时自动附带微信登录态

### 3. 进度页 `pages/status`

- 展示当前桌台所有有效订单
- 展示订单状态与菜品状态
- 10 秒自动刷新

### 4. 我的页 `pages/mine`

- 展示微信顾客信息
- 展示消费记录
- 展示积分明细

## 后端接口

沿用接口：

- `/api/c/dish/list`
- `/api/c/order/create`
- `/api/c/order/table/{tableId}`
- `/api/c/customer/info`
- `/api/c/customer/orders`
- `/api/c/customer/points`

兼容保留：

- `/api/c/auth/login`

新增：

- `/api/c/auth/wechat-login`
- `/api/c/table/{id}`

## 配置要求

后端需要增加微信配置：

- `WECHAT_MINIAPP_APP_ID`
- `WECHAT_MINIAPP_APP_SECRET`
- `WECHAT_MINIAPP_TOKEN_SECRET`
- `WECHAT_MINIAPP_TOKEN_EXPIRE_DAYS`

没有配置前两项时，小程序无法换取 `openid`。

## 下一阶段建议

1. 增加手机号手动绑定，支持老会员合并
2. 商家端生成真正的小程序码，而不是 H5 二维码
3. 接入微信支付或结账通知能力
4. 增加后台“微信顾客”和“手机号会员”的合并运营工具
