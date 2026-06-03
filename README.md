# URILI RuoYi 验证工程

本工程用于验证“官方 RuoYi-Vue 后端 + ruoyi-react Ant Design 前端”的组合，和原项目 `E:\Urili` 分开维护。

## 当前结构

```text
RuoYi-Vue/          官方若依后端，已删除 ruoyi-ui
react-ui/           React + Ant Design Pro 前端
docker-compose.yml  MySQL/Redis 本地依赖
logs/               本地启动日志
```

来源记录：

- 后端：`https://gitee.com/y_project/RuoYi-Vue`，克隆时分支 `master`，提交 `7da12b0c`。
- 前端：`https://gitee.com/whiteshader/ruoyi-react/tree/antdesign6`，克隆时提交 `cc08cc8`，只保留 `react-ui`。

## 启动依赖

```powershell
cd E:\Urili-Ruoyi
docker compose up -d
```

默认依赖：

- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- 数据库：`ry-vue`
- MySQL root 密码：`password`

首次创建 MySQL 数据卷时，Compose 会自动导入：

- `RuoYi-Vue/sql/ry_20260417.sql`
- `RuoYi-Vue/sql/quartz.sql`

## 启动后端

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install
java -jar .\ruoyi-admin\target\ruoyi-admin.jar
```

后端地址：

- `http://127.0.0.1:8080`
- 验证接口：`http://127.0.0.1:8080/captchaImage`

## 启动前端

```powershell
cd E:\Urili-Ruoyi\react-ui
npm install
$env:PORT='8001'
npm run dev
```

前端地址：

- `http://127.0.0.1:8001`

默认账号：

- 用户名：`admin`
- 密码：`admin123`

## 停止服务

停止后端和前端进程：

```powershell
netstat -ano | Select-String ':8080|:8001'
Stop-Process -Id <PID> -Force
```

停止 MySQL/Redis：

```powershell
cd E:\Urili-Ruoyi
docker compose stop
```

删除容器但保留数据卷：

```powershell
docker compose down
```

删除容器和数据卷会清空数据库：

```powershell
docker compose down -v
```

## 下一步开发建议

1. 替换品牌：标题、Logo、页脚、默认欢迎页和“若依官网”菜单。
2. 建立 URILI 后台菜单：上游系统、仓库、商品、库存、订单、履约、财务。
3. 先做上游系统和仓库模块，验证若依权限、菜单、代码生成和 React 前端页面改造流程。
4. 再进入商品、库存、订单等交易核心模块，避免直接用 CRUD 替代业务规则。
