# 腾讯云 COS 文件存储配置执行记录

## 目标

- 在现有 `FileStorageService` 抽象下新增腾讯云 COS 存储实现。
- 本地运行可通过环境变量切换到 COS，不把 SecretId、SecretKey 写入仓库。
- 保持 `/common/upload`、`/common/uploads`、`/system/user/profile/avatar` 的接口响应结构不变。

## 配置来源

- SecretId、SecretKey：用户在当前对话提供，并已按用户确认写入本机忽略提交的 `.env.local`；本文档不记录明文。
- Bucket：`fenxiao-1306508427`
- Region：`ap-guangzhou`
- Endpoint：`https://fenxiao-1306508427.cos.ap-guangzhou.myqcloud.com`

## 已修改内容

- 新增 `CosFileStorageService`，负责上传、删除和生成 COS 访问地址。
- 新增 `CosFileResourceController`，在 COS 模式下将稳定的 `/profile/**` 路径重定向到 COS 地址，避免头像和历史调用方直接依赖对象存储路径。
- 新增 `CosFileStorageProperties`，集中读取 `ruoyi.file-storage.cos.*` 配置。
- `application.yml` 新增 COS 配置占位符，默认仍为 `local`。
- `.env.example` 新增 COS 环境变量示例，真实密钥不提交。
- `reuse-ledger.md` 记录文件存储复用规则。

## 安全判断

- SecretId、SecretKey 不写入 Git 跟踪文件。
- `.env.local` 已被 `.gitignore` 命中，真实凭证只保留在本机运行配置中。
- 默认 `TENCENT_COS_PUBLIC_READ=false`，后端生成临时访问地址或重定向地址。
- COS 对象 key 默认位于 `profile/` 前缀下；业务持久化路径继续使用 `/profile/...`。
- 本次不执行数据库 DDL/DML，不调整远端数据库、Redis、字典、菜单或权限数据。

## 验证结果

- 已运行 `mvn -pl ruoyi-framework -am -DskipTests compile`，结果通过。
- 已运行 `mvn -pl ruoyi-admin -am -DskipTests compile`，结果通过。
- 首次运行 `mvn -pl ruoyi-admin -am -DskipTests package` 时失败，原因是旧后端进程锁住 `ruoyi-admin.jar`；停止 8080 旧进程后重新打包通过。
- 已运行 `.\start-backend-local.ps1 -Restart`，后端以 COS 配置启动，8080 监听正常。
- 已通过 `/login` 使用 `admin/admin123` 获取 token；当前验证码配置返回 `captchaEnabled=false`。
- 已通过 `/common/upload` 上传测试文件到 COS，接口返回 `code=200`。
- 返回持久化资源路径：`/profile/upload/2026/06/04/cos-upload-test_20260604101715A001.txt`
- 返回 COS host：`fenxiao-1306508427.cos.ap-guangzhou.myqcloud.com`
- 已通过返回的临时访问 URL 下载校验，HTTP 状态码 `200`。

## 未验证原因

- 无。真实上传和下载校验已完成。
