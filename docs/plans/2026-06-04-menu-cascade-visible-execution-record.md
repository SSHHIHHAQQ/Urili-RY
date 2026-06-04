# 菜单级联显示隐藏执行记录

## 目标

在系统管理的菜单管理页面增加“级联显示 / 级联隐藏”功能，操作选中菜单时同步影响其所有下级菜单和按钮权限。

## 已完成

- 后端新增 `PUT /system/menu/cascadeVisible` 接口，沿用 `system:menu:edit` 权限。
- 服务层新增菜单显示状态级联更新逻辑，范围包含选中菜单及全部子级菜单。
- Mapper 新增批量更新 `sys_menu.visible` 的 SQL。
- 前端菜单管理新增显示状态列。
- 前端工具栏和选中项底部工具栏新增“级联显示”“级联隐藏”按钮。

## 数据影响

- 本次只修改代码，没有直接执行菜单显示隐藏的数据更新。
- `visible=0` 表示显示，`visible=1` 表示隐藏。

## 验证

- `npm run tsc`
- `npx biome lint src/pages/System/Menu/index.tsx`
- `mvn -DskipTests install`
- `.\start-backend-local.ps1 -Restart`
- `Invoke-WebRequest http://127.0.0.1:8080/captchaImage` 返回 200
- 浏览器打开菜单管理页，选中菜单后确认“级联显示 / 级联隐藏”按钮出现。
- 浏览器打开“级联隐藏”确认框后取消，未执行真实数据更新。

## 说明

- 后端重新打包时曾因旧的运行中 jar 被占用失败，停止旧 8080 进程后重新打包成功。
- 当前后端已按本地启动脚本重启。
