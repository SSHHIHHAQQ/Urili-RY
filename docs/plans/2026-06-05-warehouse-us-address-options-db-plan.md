# 仓库美国地址选项缺失根因与补库方案

## 当前结论

- 前端在国家/地区选择 `US` 后，会请求仓库模块地址选项接口：
  - `/api/warehouse/options/us-states`
  - `/api/warehouse/options/us-cities`
- 当前运行库返回 500，错误为 `Table 'fenxiao.us_state' doesn't exist`。
- 因此页面上“州/省”和“城市”不是没有接字典交互，而是当前激活数据库缺少美国地址选项表和种子数据，导致候选数据为空。

## 字典承载方式

- 本次美国省/城市没有放入若依 `sys_dict_data`。
- 原因：美国城市数据量较大，并且需要按州、关键字、分页上限查询；若依 `sys_dict` 是扁平 code/label 字典，不适合作为城市地址库。
- 当前设计采用独立地址选项表：
  - `us_state`：美国州/地区全称与缩写。
  - `us_city`：美国城市/place 数据，支持按 `state_name` 和关键字模糊查询。
- 前端仍使用 Ant Design `AutoComplete`，只在 `countryCode = US` 时启用候选下拉和模糊搜索；其他国家保持普通输入，支持自定义输入。

## 待执行 SQL

- SQL 文件：`RuoYi-Vue/sql/warehouse_us_address_seed.sql`
- 内容范围：
  - `create table if not exists us_state (...)`
  - `create table if not exists us_city (...)`
  - 初始化美国州和城市/place 选项数据。

## 目标环境

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 使用 `druid`。
- 当前 JDBC 指向远端 MySQL，库名为 `fenxiao`。
- 本方案只针对当前激活库补充美国地址选项表和初始化数据，不修改仓库业务事实表。

## 风险与边界

- 这是远端数据库 DDL/DML，执行前必须得到确认。
- SQL 使用 `create table if not exists`，建表本身可重复执行。
- 种子数据已使用唯一键配合 `on duplicate key update`，重复执行会更新同一 GEOID / 州编码对应记录，不会按相同唯一键重复插入。
- 不涉及账号、权限、菜单、仓库主数据和配对数据。

## 回滚方式

- 若确认这些表仅用于仓库美国地址选项，可回滚：
  - `drop table us_city;`
  - `drop table us_state;`
- 如后续已有业务引用，应先停用相关接口或确认无业务数据依赖后再回滚。

## 执行后验证

- 接口验证：
  - `/api/warehouse/options/us-states` 返回 `code=200` 且包含 `California` 等州全称。
  - `/api/warehouse/options/us-cities?stateName=California&keyword=Los` 返回 `code=200` 且包含匹配城市。
- 页面验证：
  - 新增官方仓库弹窗选择 `美国 / United States (US)` 后，州/省字段聚焦展示州候选。
  - 州/省输入 `c` 或 `ca` 时可模糊筛选。
  - 选中州后，城市字段聚焦展示该州城市候选，并支持关键字模糊搜索。
  - 非美国国家仍为普通输入，不启用美国地址候选。

## 当前状态

- 前端已修复 Ant Design `AutoComplete` 的受控展开和模糊过滤逻辑。
- 当前数据库尚未执行补库 SQL。
- 由于当前库缺少 `us_state` / `us_city`，页面真实下拉验证被数据库缺表阻断。
