# Git Initialization

Date: 2026-06-03

Workspace: `E:\Urili-Ruoyi`

## Scope

Initialize the standalone URILI RuoYi validation workspace as one root Git repository and push it to:

```text
https://github.com/SSHHIHHAQQ/Urili-RY.git
```

## Repository Shape

- Root repository branch: `main`
- Backend source included as normal files from `RuoYi-Vue/`
- Frontend source included as normal files from `react-ui/`
- The removed Vue frontend `RuoYi-Vue/ruoyi-ui` remains absent

`RuoYi-Vue` was originally a nested Git repository. Its `.git` directory was moved to a local ignored backup path so the root repository can track backend source files directly instead of committing a submodule/gitlink:

```text
E:\Urili-Ruoyi\.git-source-backups\RuoYi-Vue.git-7da12b0c-20260603
```

## Ignore Rules

Added root `.gitignore` entries for local runtime artifacts:

- `logs/`
- `output/`
- `.playwright-cli/`
- `.git-source-backups/`

Adjusted `react-ui/.gitignore` so necessary project files are not accidentally omitted:

- `package-lock.json` is no longer ignored.
- `src/pages/Tool/Build/` is explicitly unignored because the generic `build` ignore rule matched that source directory on Windows.

## Verification Before Commit

Completed checks before repository creation/push:

```powershell
npm run tsc
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/captchaImage' -UseBasicParsing
docker exec urili-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -ppassword ry-vue --execute="select user_name,nick_name,hex(nick_name) as hex_name from sys_user where user_name='admin';"
```

Results:

- TypeScript check passed.
- Backend captcha endpoint returned HTTP 200.
- Local database stores Chinese seed data as UTF-8.
