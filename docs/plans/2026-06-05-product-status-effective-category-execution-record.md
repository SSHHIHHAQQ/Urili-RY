# 商品属性状态与类目有效状态执行记录

## 目标

- 商品属性配置的启用/停用改为开关操作。
- 启用/停用商品属性必须二次确认，避免误操作。
- 类目状态按“自身停用或任一上级停用即有效停用”计算，不新增数据库字段。
- 类目属性模板筛选“正常类目”时排除上级已停用的下级类目，筛选“停用类目”时包含上级停用导致不可用的下级类目。

## 实现范围

- 后端 `product` 模块新增商品属性状态更新接口：`PUT /product/admin/attributes/{attributeId}/status`。
- 后端类目查询返回 `effectiveStatus` 和 `disabledByAncestor`，由 SQL 根据 `status` 与 `ancestors` 实时计算。
- 后端类目列表支持 `effectiveStatus` 查询参数。
- 卖家/买家商品发布 Schema 入口改为读取类目有效状态，避免上级停用后下级仍可用。
- 前端属性库状态列改为 `Switch`，操作前弹出二次确认。
- 前端属性新增/编辑表单移除状态字段，状态只能通过开关变更。
- 前端类目属性模板使用 `effectiveStatus` 做类目筛选；停用类目筛选走分页搜索，避免依赖正常上级节点懒加载。

## 数据库影响

- 未新增表。
- 未新增字段。
- 未执行 DDL。
- 本次状态有效性由运行时查询计算，不持久化冗余字段。

## 验证记录

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests package`：通过。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1 -Restart`：后端已重启。
- `Invoke-WebRequest http://127.0.0.1:8080`：返回 HTTP 200。
- 浏览器验证属性库：状态列展示启用/停用开关。
- 浏览器验证二次确认：点击停用属性的开关后弹出“启用商品属性”确认框，取消后未提交状态变更。
- 浏览器验证正常类目筛选请求：`GET /api/product/admin/categories/children?parentId=0&effectiveStatus=0` 返回 200。
- 浏览器验证停用类目筛选请求：`GET /api/product/admin/categories/search?effectiveStatus=1&pageNum=1&pageSize=200` 返回 200。
- 浏览器控制台：错误 0，警告 0。

## 复用与边界检查

- 继续复用现有 `product` 模块 Controller、Service、Mapper 分层。
- 继续复用现有商品配置 API service 与类目属性模板组件，不新建跨端外层模块。
- 商品属性状态仍保存原有 `status` code：`0` 正常，`1` 停用。
- 类目有效状态不写入 `sys_dict`，因为它不是独立字典值，而是由业务规则计算出来的展示/筛选状态。
- 本次没有新增公共组件或跨模块公共工具，不需要新增复用台账条目。

## 风险与后续

- 当前没有执行真实“确认启用/确认停用”提交，以避免改动测试数据；接口编译和页面确认弹窗已验证。
- 若后续要在列表中明显区分“自身停用”和“上级停用导致停用”，可以使用已返回的 `disabledByAncestor` 做灰色提示或标签说明。
- 如果类目数量继续扩大，类目筛选仍应保持分页搜索和懒加载，不应恢复一次性全量树。
