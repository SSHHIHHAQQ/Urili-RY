# AGENTS.md

本目录是 URILI 的若依验证工程，独立于 `E:\Urili`。后续 AI agent 必须先读本文件。

## 项目定位

- 后端唯一基线：`RuoYi-Vue` 官方后端，路径 `RuoYi-Vue/`。
- 前端唯一基线：`ruoyi-react` 的 `antdesign6` 分支前端，路径 `react-ui/`。
- 默认 Vue 前端 `RuoYi-Vue/ruoyi-ui` 已删除，后续不要恢复、复制或继续开发它。
- `ruoyi-react` 仓库里的后端副本已删除，后续不要重新引入第二套若依后端。
- 当前目标是验证并改造“若依后端 + React/Ant Design 前端”，再逐步承载 URILI 业务。

## 目录规则

```text
E:\Urili-Ruoyi\
  RuoYi-Vue\        # 官方若依后端
  react-ui\         # React + Ant Design Pro 前端
  docker-compose.yml
  README.md
  logs\
```

规则：

- 后端业务改动只进入 `RuoYi-Vue/`。
- 前端业务改动只进入 `react-ui/`。
- 不要把旧项目 `E:\Urili` 的代码直接搬进本工程；需要迁移时先写 Markdown 方案。
- 生成报告、迁移记录、阶段总结时，都必须生成 Markdown 文件。
- 数据库和 Redis 默认通过根目录 `docker-compose.yml` 启动。

## 默认服务

- MySQL：`localhost:3306`，库名 `ry-vue`，账号 `root`，密码 `password`。
- Redis：`localhost:6379`，无密码。
- 后端：`http://127.0.0.1:8080`。
- React 前端：`http://127.0.0.1:8001`。
- 默认登录：`admin / admin123`。

## 开发边界

- 保留若依的用户、角色、菜单、权限、字典、日志、定时任务、代码生成等基础能力。
- URILI 商品、库存、订单、履约、财务、领星接入等业务必须按模块逐步设计，不要用简单 CRUD 直接替代业务规则。
- 新增业务表前，先写清楚模块职责、字段含义和权限点。
- 后续 AI 开发时优先保持小步任务，不要一次同时改后端、前端、数据库和 UI 主题。

## 常用命令

```powershell
cd E:\Urili-Ruoyi
docker compose up -d

cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install
java -jar .\ruoyi-admin\target\ruoyi-admin.jar

cd E:\Urili-Ruoyi\react-ui
npm install
$env:PORT='8001'; npm run dev
```
