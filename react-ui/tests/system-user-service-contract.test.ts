import fs from 'node:fs';
import path from 'node:path';

const serviceTs = fs.readFileSync(path.resolve(__dirname, '../src/services/system/user.ts'), 'utf8');
const serviceJs = fs.readFileSync(path.resolve(__dirname, '../src/services/system/user.js'), 'utf8');

describe('system user service contract', () => {
  it('routes auth role APIs through the admin API proxy prefix', () => {
    for (const source of [serviceTs, serviceJs]) {
      expect(source).toContain("request('/api/system/user/authRole/' + userId");
      expect(source).toContain("request('/api/system/user/authRole'");
      expect(source).not.toContain("request('/system/user/authRole");
    }
  });
});
