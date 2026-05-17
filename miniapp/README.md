# 铜锅涮肉微信小程序

这是一个独立于现有 `frontend/` Web 管理端的原生微信小程序工程，当前优先覆盖顾客扫码点餐链路。

## 当前能力

- 入口页：扫码 / 手输桌号，自动 `wx.login`
- 菜单页：分类筛选、搜索、购物袋、备注、提交订单
- 进度页：查看当前桌台订单和上菜状态，10 秒自动刷新
- 我的页：基于微信登录态读取会员信息、消费记录、积分明细

## 认证方案

小程序不再依赖手机号作为顾客身份主键，而是改成：

1. 小程序调用 `wx.login`
2. 前端把 `code` 发给后端 `/api/c/auth/wechat-login`
3. 后端调用微信 `code2Session` 换取 `openid`
4. 后端按 `openid` 创建 / 查找顾客
5. 后端签发业务 token
6. 小程序后续请求统一带 `X-Customer-Token`

这样可以确认请求来自微信小程序环境，同时避免依赖收费的手机号能力。

## 目录

- `app.*`：小程序全局配置
- `pages/entry`：扫码落座入口
- `pages/menu`：点餐主页面
- `pages/status`：上菜状态
- `pages/mine`：会员信息
- `api/`：对接后端 `/api/c/*`
- `utils/config.js`：接口地址配置
- `utils/auth.js`：微信登录与 token 存储

## 后端新增 / 调整

- 新增 `POST /api/c/auth/wechat-login`
- 新增 `GET /api/c/table/{id}`
- 顾客鉴权支持 `X-Customer-Token`
- `customer` 表增加 `openid`
- 启动时自动迁移旧版 `customer` 表结构

## 配置

### 1. 小程序接口地址

修改 `utils/config.js`：

```js
const API_BASE_URL = 'http://你的后端地址:8080'
```

### 2. 后端微信配置

通过环境变量配置：

```bash
WECHAT_MINIAPP_APP_ID=你的小程序AppID
WECHAT_MINIAPP_APP_SECRET=你的小程序AppSecret
WECHAT_MINIAPP_TOKEN_SECRET=自定义长随机串
WECHAT_MINIAPP_TOKEN_EXPIRE_DAYS=30
```

如果没有配置 `AppID / AppSecret`，小程序入口页会拿不到微信登录态。

## 本地运行

1. 启动后端服务
2. 配好上面的微信环境变量
3. 用微信开发者工具打开 `miniapp/`
4. 确认 `utils/config.js` 指向你的后端
5. 在开发者工具中进入入口页调试，可传 `tableId=1`

## 真机调试注意

- 真机不能用 `127.0.0.1`
- 请改成你电脑在同一局域网下可访问的 IP
- 微信后台还要把接口域名加入合法域名白名单

## 兼容说明

- 旧的 Web 顾客端手机号登录接口仍然保留
- 小程序新链路优先使用 `openid + token`
- 商家后台结账时仍可手动录入手机号做会员累计
