# 仓库美国地址选项补库执行记录

## 执行时间

- 2026-06-05

## 目标环境

- 项目：`E:\Urili-Ruoyi`
- 后端激活配置来源：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 数据源类型：远端 MySQL
- 目标库：`fenxiao`
- Redis：本次未读写 Redis

## 执行内容

- SQL 文件：`RuoYi-Vue/sql/warehouse_us_address_seed.sql`
- 执行方式：本机 Java 21 + Maven 缓存中的 `mysql-connector-j` JDBC 驱动，临时执行器读取 SQL 文件逐条执行。
- 执行性质：
  - DDL：创建 `us_state`、`us_city`
  - DML：初始化美国州和城市/place 选项数据
- 幂等性：SQL 使用唯一键配合 `on duplicate key update`，重复执行会更新同一唯一键记录。

## 执行结果

- 执行 SQL 语句数：69
- `us_state` 记录数：51
- `us_city` 记录数：32058

## 页面与接口验证

- 登录方式：`admin / admin123`
- 页面：`http://127.0.0.1:8001/warehouse/warehouse-official`
- 验证动作：
  - 打开“新增官方仓库”弹窗。
  - 国家/地区选择 `美国 / United States (US)`。
  - 州/省字段切换为 Ant Design `AutoComplete`。
  - 点击州/省字段后展示州全称候选，如 `Alabama`、`California`。
  - 输入 `ca` 后未失焦，并过滤出 `California`、`North Carolina`、`South Carolina`。
  - 选中 `California` 后点击城市字段，展示该州城市候选。
  - 输入 `los` 后未失焦，并触发城市关键字查询。
- 网络请求结果：
  - `GET /api/warehouse/options/us-states`：200
  - `GET /api/warehouse/options/us-cities?stateName=California`：200
  - `GET /api/warehouse/options/us-cities?stateName=California&keyword=los`：200

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
npm run build

cd E:\Urili-Ruoyi
codegraph sync .
```

## 注意事项

- 本次没有把美国省/城市写入若依 `sys_dict_data`。原因是城市数据量较大且需要按州和关键字查询，独立地址选项表比扁平字典更适合。
- 前端仍使用 Ant Design 原生 `AutoComplete`，只在 `countryCode = US` 时启用美国地址候选；其他国家保持普通输入并支持自定义。
