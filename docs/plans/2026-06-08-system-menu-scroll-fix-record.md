# 菜单管理页面滚动修复记录

## 问题

菜单管理页面在菜单树较长时无法向下滚动，底部菜单行被裁掉。

## 根因

全局列表页样式会对包含 `ProTable` 的 `PageContainer` 设置固定视口高度和 `overflow: hidden`，用于让标准列表页的表格主体滚动、分页器固定在底部。

菜单管理页是树表页面，没有分页，也没有给 `ProTable` 配置 `scroll.y`。因此 Ant Design 表格没有生成 `.ant-table-body` 内部滚动容器，外层页面滚动又被全局样式关闭，最终导致超出可视区域的菜单行不可见。

浏览器验证修复前关键指标：

- `.ant-pro-page-container`：`overflowY=hidden`
- `.ant-pro-table`：`overflowY=hidden`
- `.ant-table`：`scrollHeight=685`，可见高度约 `372`
- `.ant-table-body`：不存在
- `.ant-table-fixed-header`：不存在

## 修复

在 `react-ui/src/pages/System/Menu/index.tsx` 和同目录 JS 镜像中引入并使用已有工具函数：

- `getProTableScroll(1300)`

该配置让菜单树表生成固定表头和表格主体滚动区，保持全局列表页布局规则不变。

## 验证

- `npm run tsc`：通过
- `codegraph sync .`：通过
- 浏览器刷新 `http://127.0.0.1:8001/system/menu` 后验证：
  - `.ant-table-fixed-header` 已存在
  - `.ant-table-body` 已存在
  - `.ant-table-body overflowY=auto`
  - `.ant-table-body scrollHeight=657`，`clientHeight=310`
  - 鼠标滚轮后 `.ant-table-body scrollTop=347`，底部菜单行可见
