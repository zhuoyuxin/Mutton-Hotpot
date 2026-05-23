# Repository Guidelines

## 项目结构与模块组织
本仓库是一个多端单体仓，包含 3 个可运行应用和文档：
- `backend/`：Spring Boot + MyBatis 后端（`src/main/java/com/tongguo`、`src/main/resources`）。
- `frontend/`：Vue 3 + Vite Web 端，覆盖商户后台与 H5 顾客流程（`src/views/merchant`、`src/views/customer`、`src/api`）。
- `miniapp/`：微信原生小程序顾客端（`pages/`、`api/`、`utils/`）。
- `docs/`：设计与计划文档，仅作参考，不参与运行。

后端运行时数据目录为 `backend/data/`，上传图片目录为 `backend/uploads/`（启动时自动创建）。

## 构建、测试与开发命令
- 后端开发启动：`cd backend && mvn spring-boot:run`（默认监听 `:8080`）。
- 后端打包：`cd backend && mvn clean package -DskipTests`（产物 `target/tongguo.jar`）。
- 后端测试：`cd backend && mvn test`（当前测试覆盖较少，新增逻辑请补测）。
- 前端开发：`cd frontend && npm install && npm run dev`（Vite 默认 `:5173`）。
- 前端构建：`cd frontend && npm run build`（输出到 `backend/src/main/resources/static/`）。
- 前端预览：`cd frontend && npm run preview`。
- 小程序调试：使用微信开发者工具打开 `miniapp/`，并在 `miniapp/utils/config.js` 配置 API 地址。

## 代码风格与命名规范
- Java：4 空格缩进；类名 `UpperCamelCase`；方法/字段 `lowerCamelCase`；按 `controller`、`service`、`mapper` 分层。
- Vue/JS/WXML：2 空格缩进；组件文件使用 `PascalCase`（如 `DishFormDialog.vue`）；API 模块文件使用小写命名（如 `src/api/order.js`）。
- API 路由保持约定：商户端 `/api/m/**`，顾客端 `/api/c/**`。
- 当前未配置强制 lint/format 工具；提交前请保持与周边代码风格一致，并尽量控制改动范围。

## 测试指南
当前仓库尚无完整自动化测试体系。涉及后端功能变更时：
- 视情况在 `backend/src/test/java` 新增或更新 Spring Boot 测试；
- 本地执行 `mvn test`；
- 在 PR 中补充前端/小程序关键流程的手工验证说明。

## 提交与合并请求规范
提交信息建议沿用仓库历史中的 Conventional Commit 前缀：`feat:`、`fix:`、`refactor:`、`chore:`，主题使用祈使句并明确变更点（中文）。

PR 建议包含：
- 变更内容与动机；
- 影响范围（`backend` / `frontend` / `miniapp`）；
- 配置或数据迁移说明（如 SQLite 表结构、环境变量）；
- UI 改动截图或短视频；
- 已执行的命令与验证结果（构建/测试/手工检查）。

## 安全与配置提示
- 不要提交密钥或生产地址；后端优先使用环境变量（如 `WECHAT_MINIAPP_*`、结账默认参数）。
- 新增代码避免硬编码外部主机地址，优先走环境化配置。
- 不要提交运行时产物（如 `*.db`、`uploads/`、构建输出文件）。
