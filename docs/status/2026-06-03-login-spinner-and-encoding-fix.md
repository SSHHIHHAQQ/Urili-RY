# Login Spinner And Encoding Fix

Date: 2026-06-03

Workspace: `E:\Urili-Ruoyi`

## Scope

This record covers the local RuoYi React validation stack:

- Backend: `E:\Urili-Ruoyi\RuoYi-Vue`
- Frontend: `E:\Urili-Ruoyi\react-ui`
- MySQL and Redis from root `docker-compose.yml`

No code was migrated from `E:\Urili`.

## Symptoms

- After login, the UI stayed on a loading spinner.
- The visible page looked dark because the account center was stuck in `PageLoading`.
- Some Chinese text displayed as garbled characters.
- Top-level menu labels still showed technical route names like `system`, `monitor`, and `tool`.

## Causes

1. `react-ui/src/pages/User/Center/index.tsx` wrapped the `getUserInfo()` response as `{ data }`, but the page reads `userInfo.user`. That made `currentUser` stay empty and kept the page loading forever.
2. The local RuoYi seed SQL had been imported into MySQL with the wrong client charset. The SQL file itself contained correct Chinese text, but database rows had mojibake.
3. Dynamic route patching in `react-ui/src/services/session.ts` reused static route shells without copying backend menu metadata such as `name`, `icon`, visibility, and authority fields.

## Fixes

- Fixed account center user info state handling and normalized empty avatars:
  - `E:\Urili-Ruoyi\react-ui\src\pages\User\Center\index.tsx`
- Preserved backend menu metadata while patching React routes:
  - `E:\Urili-Ruoyi\react-ui\src\services\session.ts`
- Added an explicit UTF-8 client charset directive to the RuoYi seed SQL:
  - `E:\Urili-Ruoyi\RuoYi-Vue\sql\ry_20260417.sql`

The local `ry-vue` database was dropped and recreated, then `01-ry.sql` and `02-quartz.sql` were reimported with `--default-character-set=utf8mb4`. Redis local cache was flushed after the database rebuild.

## Verification

Commands/checks completed:

```powershell
npm run tsc
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/captchaImage' -UseBasicParsing
docker exec urili-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -ppassword ry-vue --execute="select user_name,nick_name,hex(nick_name) from sys_user where user_name='admin';"
```

Results:

- `npm run tsc` passes.
- Backend captcha endpoint returns HTTP 200.
- Database now stores `admin` nickname as `若依` with UTF-8 hex `E88BA5E4BE9D`.
- Database now stores department name `研发部门` with UTF-8 hex `E7A094E58F91E983A8E997A8`.
- Browser login with `admin/admin123` reaches `/account/center`.
- Account center no longer stays on the spinner.
- Sidebar top-level labels display `系统管理`, `系统监控`, `系统工具`, and `若依官网`.

Playwright screenshot from the verified login state:

```text
E:\Urili-Ruoyi\.playwright-cli\page-2026-06-03T05-48-03-931Z.png
```

## Current Runtime

- Backend: `http://127.0.0.1:8080`
- Frontend: `http://127.0.0.1:8001`
- MySQL: `localhost:3306`
- Redis: `localhost:6379`

Listening ports were confirmed for `8080`, `8001`, `3306`, and `6379`.

## Remaining Work

- Ant Design still logs a non-blocking deprecation warning for `List` in the account center.
- Ant Design Pro and RuoYi visible branding still need a separate URILI branding pass.
- `react-ui` is currently not a Git repository, so frontend changes are not tracked by local Git.
