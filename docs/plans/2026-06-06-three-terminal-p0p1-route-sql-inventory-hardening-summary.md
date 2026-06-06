# 2026-06-06 三端 P0/P1 路由 SQL 库存入口收口摘要

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 范围：只修 P0/P1；不做浏览器、截图、DOM、UI 细调。
- 结果：动态菜单路由、Portal Home 权限、SQL legacy guard、端内 seed 授权、未确认库存入口和默认测试入口已收口。
- 验证：`npm run verify:three-terminal`、`npm test -- --runInBand`、`mvn -pl ruoyi-admin -am -DskipTests compile` 均通过。
