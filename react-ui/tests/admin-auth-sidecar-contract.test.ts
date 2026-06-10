import fs from 'fs';
import path from 'path';
import {
  resolveAdminRedirect,
  resolveAdminRedirectFromSearch,
} from '@/utils/adminRedirect';

const sidecars = [
  {
    file: 'src/app.js',
    source: 'src/app.tsx',
    expected:
      "export {\n"
      + "  getInitialState,\n"
      + "  layout,\n"
      + "  rootContainer,\n"
      + "  onRouteChange,\n"
      + "  patchClientRoutes,\n"
      + "  render,\n"
      + "  request,\n"
      + "} from './app.tsx';\n",
  },
  {
    file: 'src/access.js',
    source: 'src/access.ts',
    expected: "export { default } from './access.ts';\nexport * from './access.ts';\n",
  },
  {
    file: 'src/requestErrorConfig.js',
    source: 'src/requestErrorConfig.ts',
    expected: "export * from './requestErrorConfig.ts';\n",
  },
  {
    file: 'src/services/session.js',
    source: 'src/services/session.ts',
    expected: "export * from './session.ts';\n",
  },
  {
    file: 'src/services/portal/session.js',
    source: 'src/services/portal/session.ts',
    expected: "export * from './session.ts';\n",
  },
  {
    file: 'src/services/seller/seller.js',
    source: 'src/services/seller/seller.ts',
    expected: "export * from './seller.ts';\n",
  },
  {
    file: 'src/services/buyer/buyer.js',
    source: 'src/services/buyer/buyer.ts',
    expected: "export * from './buyer.ts';\n",
  },
  {
    file: 'src/utils/portalPaths.js',
    source: 'src/utils/portalPaths.ts',
    expected: "export * from './portalPaths.ts';\n",
  },
  {
    file: 'src/utils/portalRequest.js',
    source: 'src/utils/portalRequest.ts',
    expected: "export * from './portalRequest.ts';\n",
  },
  {
    file: 'src/utils/portalDirectLoginMessage.js',
    source: 'src/utils/portalDirectLoginMessage.ts',
    expected: "export * from './portalDirectLoginMessage.ts';\n",
  },
  {
    file: 'src/utils/remoteMenuStorage.js',
    source: 'src/utils/remoteMenuStorage.ts',
    expected: "export * from './remoteMenuStorage.ts';\n",
  },
  {
    file: 'src/wrappers/RemoteMenuRouteGuard.js',
    source: 'src/wrappers/RemoteMenuRouteGuard.tsx',
    expected: "export { default } from './RemoteMenuRouteGuard.tsx';\nexport * from './RemoteMenuRouteGuard.tsx';\n",
  },
  {
    file: 'config/proxy.js',
    source: 'config/proxy.ts',
    expected: "export { default } from './proxy.ts';\n",
  },
  {
    file: 'config/routes.js',
    source: 'config/routes.ts',
    expected: "export { default } from './routes.ts';\n",
  },
  {
    file: 'src/utils/initialStateModel.js',
    source: 'src/utils/initialStateModel.ts',
    expected: "export * from './initialStateModel.ts';\n",
  },
  {
    file: 'src/utils/adminRedirect.js',
    source: 'src/utils/adminRedirect.ts',
    expected: "export * from './adminRedirect.ts';\n",
  },
  {
    file: 'src/utils/permission.js',
    source: 'src/utils/permission.ts',
    expected: "export * from './permission.ts';\n",
  },
  {
    file: 'src/pages/User/Login/index.js',
    source: 'src/pages/User/Login/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/Portal/terminal.js',
    source: 'src/pages/Portal/terminal.ts',
    expected: "export * from './terminal.ts';\n",
  },
  {
    file: 'src/pages/Portal/Home/index.js',
    source: 'src/pages/Portal/Home/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/Seller/Portal/index.js',
    source: 'src/pages/Seller/Portal/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/Buyer/Portal/index.js',
    source: 'src/pages/Buyer/Portal/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/Portal/Login/index.js',
    source: 'src/pages/Portal/Login/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/Portal/DirectLogin/index.js',
    source: 'src/pages/Portal/DirectLogin/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/components/RightContent/AvatarDropdown.js',
    source: 'src/components/RightContent/AvatarDropdown.tsx',
    expected: "export * from './AvatarDropdown.tsx';\n",
  },
  {
    file: 'src/pages/Welcome.js',
    source: 'src/pages/Welcome.tsx',
    expected: "export { default } from './Welcome.tsx';\n",
  },
  {
    file: 'src/components/index.js',
    source: 'src/components/index.ts',
    expected: "export * from './index.ts';\n",
  },
  {
    file: 'src/services/system/auth.js',
    source: 'src/services/system/auth.ts',
    expected: "export * from './auth.ts';\n",
  },
  {
    file: 'src/services/system/menu.js',
    source: 'src/services/system/menu.ts',
    expected: "export * from './menu.ts';\n",
  },
  {
    file: 'src/pages/System/Menu/index.js',
    source: 'src/pages/System/Menu/index.tsx',
    expected: "export { default } from './index.tsx';\n",
  },
  {
    file: 'src/pages/System/Menu/edit.js',
    source: 'src/pages/System/Menu/edit.tsx',
    expected: "export { default } from './edit.tsx';\n",
  },
  {
    file: 'src/pages/System/Role/authUser.js',
    source: 'src/pages/System/Role/authUser.tsx',
    expected: "export { default } from './authUser.tsx';\n",
  },
  {
    file: 'src/services/ant-design-pro/login.js',
    source: 'src/services/ant-design-pro/login.ts',
    expected: "export * from './login.ts';\n",
  },
];

const readSource = (file: string) =>
  fs.readFileSync(path.join(process.cwd(), file), 'utf8').replace(/\r\n/g, '\n');

const terminalPortalWrappers = [
  'src/pages/Seller/Portal/index.tsx',
  'src/pages/Buyer/Portal/index.tsx',
];

describe('admin auth runtime sidecars', () => {
  it.each(sidecars)('$file delegates to $source', ({ file, source, expected }) => {
    expect(fs.existsSync(path.join(process.cwd(), source))).toBe(true);
    expect(readSource(file)).toBe(expected);
  });

  it.each(terminalPortalWrappers)('$file delegates terminal page menus to the shared portal home', (file) => {
    expect(readSource(file)).toBe("export { default } from '@/pages/Portal/Home';\n");
  });

  it('uses RuoYi admin login endpoints instead of legacy Ant Design Pro mock auth', () => {
    const loginPage = readSource('src/pages/User/Login/index.tsx');
    const systemAuth = readSource('src/services/system/auth.ts');
    const legacyAuth = readSource('src/services/ant-design-pro/login.ts');
    const combined = [loginPage, systemAuth, legacyAuth].join('\n');

    expect(systemAuth).toContain("'/api/login'");
    expect(systemAuth).toContain("'/api/captchaImage'");
    expect(systemAuth).toContain("'/api/logout'");
    expect(legacyAuth).toBe("export { login, logout as outLogin } from '../system/auth';\n");
    [
      '/api/login/account',
      '/api/login/captcha',
      '/api/login/outLogin',
      'getFakeCaptcha',
      'getMobileCaptcha',
      'ProFormCaptcha',
      'MobileOutlined',
    ].forEach((legacyToken) => {
      expect(combined).not.toContain(legacyToken);
    });
  });

  it('keeps admin login redirect constrained to admin relative routes', () => {
    const loginPage = readSource('src/pages/User/Login/index.tsx');

    expect(loginPage).toContain('resolveAdminRedirectFromSearch(window.location.search)');
    expect(loginPage).not.toContain("urlParams.get('redirect') || '/'");
    expect(resolveAdminRedirect('https://evil.test')).toBe('/');
    expect(resolveAdminRedirect('//evil.test')).toBe('/');
    expect(resolveAdminRedirect('javascript:alert(1)')).toBe('/');
    expect(resolveAdminRedirect('/seller/portal/orders')).toBe('/');
    expect(resolveAdminRedirect('/buyer/login')).toBe('/');
    expect(resolveAdminRedirect('/user/login')).toBe('/');
    expect(resolveAdminRedirect('/system/user?page=1#row')).toBe('/system/user?page=1#row');
    expect(resolveAdminRedirectFromSearch('?redirect=%2Fsystem%2Frole%3Fpage%3D2')).toBe('/system/role?page=2');
  });
});
