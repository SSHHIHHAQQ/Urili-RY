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
    expect(source).not.toContain('setSessionRows([]);');
  });
});
