import fs from 'fs';
import path from 'path';

describe('portal home error handling contract', () => {
  const source = fs.readFileSync(
    path.resolve(__dirname, '../src/pages/Portal/Home/index.tsx'),
    'utf8',
  );

  it('does not turn generic data loading failures into terminal logout redirects', () => {
    expect(source).toContain("message.error('门户数据加载失败，请稍后重试')");
    expect(source).not.toContain('clearPortalLogin(currentTerminal);');
    expect(source).not.toContain('history.replace(PORTAL_META[currentTerminal].loginPath)');
  });

  it('checks portal success codes before consuming home data and sessions', () => {
    expect(source).toContain('function assertPortalSuccess');
    expect(source).toContain("assertPortalSuccess(infoResponse, 'Portal info loading failed')");
    expect(source).toContain("assertPortalSuccess(subjectResponse, 'Portal subject loading failed')");
    expect(source).toContain("assertPortalSuccess(accountResponse, 'Portal account loading failed')");
    expect(source).toContain("'Portal accounts loading failed'");
    expect(source).toContain("'Portal depts loading failed'");
    expect(source).toContain("'Portal roles loading failed'");
    expect(source).toContain("'Portal sessions loading failed'");
  });

  it('loads the protected session list only when the terminal account has session permission', () => {
    expect(source).toContain("portalPermission(terminal, 'account:session:list')");
    expect(source).toContain('const canViewSessions = hasPortalPermission');
    expect(source).toMatch(/hasPortalPermission\(\s*permissions,\s*portalPermission\(terminal, 'account:session:list'\)\s*\)/);
    expect(source).toMatch(/loadSessions\(\s*terminal(?:\s*,[^)]*)?\)/);
  });

  it('does not refresh the protected session list without session permission', () => {
    expect(source).toContain('const clearSessions = useCallback');
    expect(source).toContain('const handleRefresh = () => {');
    expect(source).toContain('clearSessions();');
    expect(source).toContain('loadData(terminal);');
    expect(source).toContain('onClick={handleRefresh}');
    expect(source).not.toContain('loadData(terminal);\n              loadSessions(terminal);');
    const handleRefreshBody = source.slice(
      source.indexOf('const handleRefresh = () => {'),
      source.indexOf('return ('),
    );
    expect(handleRefreshBody).not.toMatch(/loadSessions\(\s*terminal(?:\s*,[^)]*)?\)/);
  });
});
