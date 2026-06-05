# 仓库美国地址大小写不敏感修复记录

## 问题

- 用户在美国州/城市选择中输入大写关键字时，城市候选没有按预期返回。
- 前端本地 `AutoComplete` 过滤已经使用 `toLowerCase()`，不区分大小写。
- 实际问题在后端 SQL：`us_state` / `us_city` 查询直接使用 `like concat('%', #{keyword}, '%')`，当前 MySQL 排序规则下大小写敏感。

## 根因验证

- 直接查询当前远端库 `fenxiao`：
  - `los_count=7`
  - `LOS_count=0`
  - 使用 `lower(...)` 后 `lower_LOS_count=23`
- 说明后端 SQL 需要显式做大小写归一化，不能依赖数据库默认 collation。

## 修复内容

- 文件：`RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml`
- 修改点：
  - 美国州查询：`state_name`、`state_code` 使用 `lower(column) like concat('%', lower(#{keyword}), '%')`
  - 美国城市查询：`city_name`、`place_name`、`place_type` 使用 `lower(column) like concat('%', lower(#{keyword}), '%')`
  - 城市按州过滤：`lower(state_name) = lower(#{stateName})`

## 验证

- 后端打包：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

- 后端重启：

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

- 页面验证：
  - 打开 `http://127.0.0.1:8001/warehouse/warehouse-official`
  - 新增官方仓库
  - 国家/地区选择 `美国 / United States (US)`
  - 州/省输入大写 `CA`，候选包含 `California`
  - 选择 `California`
  - 城市输入大写 `LOS`，候选包含 `East Los Angeles`、`Los Angeles`、`Los Banos` 等

- 网络验证：
  - `GET /api/warehouse/options/us-states`：200
  - `GET /api/warehouse/options/us-cities?stateName=California`：200
  - `GET /api/warehouse/options/us-cities?stateName=California&keyword=LOS`：200

## 备注

- 第一次打包失败是因为旧 8080 后端进程锁住 `ruoyi-admin.jar`，停掉 8080 对应进程后重新打包成功。
- 本次未修改数据库结构或数据。
