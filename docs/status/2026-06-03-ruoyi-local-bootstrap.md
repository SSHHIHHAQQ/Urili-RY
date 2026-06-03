# RuoYi Local Bootstrap Status

Date: 2026-06-03

## Scope

This record covers the standalone RuoYi validation workspace at `E:\Urili-Ruoyi`.
It is separate from the current URILI workspace at `E:\Urili`.

## Repository Layout

- Backend: `E:\Urili-Ruoyi\RuoYi-Vue`
- React frontend: `E:\Urili-Ruoyi\react-ui`
- Removed legacy Vue frontend: `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-ui`
- Removed duplicated source clone wrapper: `E:\Urili-Ruoyi\ruoyi-react`

## Environment Changes

- Fixed global Maven settings at `E:\Maven\apache-maven-3.9.9\conf\settings.xml`.
- Backed up previous broken Maven settings to `E:\Maven\apache-maven-3.9.9\conf\settings.xml.bak-20260603-113410`.
- Added `docker-compose.yml` for local MySQL and Redis.
- Added `AGENTS.md` and `README.md` for this standalone workspace.

## Runtime

- MySQL: Docker Compose service `mysql`, container `urili-ruoyi-mysql`, port `3306`.
- Redis: Docker Compose service `redis`, container `urili-ruoyi-redis`, port `6379`.
- Backend: `http://127.0.0.1:8080`
- Frontend: `http://127.0.0.1:8001`

## Verification

Commands and checks completed:

```powershell
docker compose up -d
mvn -DskipTests install
npm install
npm run tsc
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/captchaImage'
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8001/api/captchaImage'
```

Results:

- Docker Compose MySQL and Redis containers are healthy.
- Imported RuoYi SQL successfully; database has 31 tables and 2 users.
- Backend captcha endpoint returns HTTP 200.
- Frontend proxy captcha endpoint returns HTTP 200.
- API login with `admin/admin123` succeeds and `getInfo` returns user `admin`, role `admin`, permission `*:*:*`.
- Browser login through the React frontend succeeds and reaches `/account/center`.
- Playwright console check on the login page and post-login page reports 0 errors after fixes.

## Fixes Applied

- `react-ui/src/app.tsx`
  - Guarded `getUserInfo` against missing `response.user`.
  - Normalized empty avatar values to a default avatar.
  - Avoided passing an empty avatar `src` into layout avatar props.

- `react-ui/src/pages/User/Login/index.tsx`
  - Avoided passing an empty captcha image `src` before the captcha request finishes.

## Known Remaining Work

- Replace Ant Design Pro/RuoYi visible branding with URILI branding.
- Decide the first URILI business module to migrate or rebuild on this base.
- Build and verify production artifacts later with frontend `npm run build` and backend tests.
- Initialize branch discipline or a git repository for `E:\Urili-Ruoyi` if this workspace becomes the real migration base.
