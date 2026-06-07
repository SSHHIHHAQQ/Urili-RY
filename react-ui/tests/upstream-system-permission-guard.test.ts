import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

describe('upstream system permission guard', () => {
  it('keeps manual sync permission gates mirrored in ts and js pages', () => {
    const sources = [
      readSource('src/pages/UpstreamSystem/index.tsx'),
      readSource('src/pages/UpstreamSystem/index.js'),
    ];

    for (const source of sources) {
      expect(source).toContain('syncTypeOptions');
      expect(source).toMatch(/permission:\s*['"]integration:upstream:sync['"]/);
      expect(source).toMatch(/permission:\s*['"]integration:upstream:dimensionSync['"]/);
      expect(source).toMatch(/permission:\s*['"]integration:upstream:inventorySync['"]/);
      expect(source).toMatch(/access\.hasPerms\(option\.permission\)/);
      expect(source).toMatch(/syncTypes:\s*allowedDefaults/);
      expect(source).toMatch(/syncTypes:\s*syncModal\.syncTypes/);
      expect(source).toContain('dimensionActionRef');
      expect(source).toContain('inventoryActionRef');
      expect(source).toContain('getOfficialWarehouseList');
      expect(source).not.toMatch(/await syncUpstreamConnection\(record\.connectionCode\)/);
    }
  });
});
