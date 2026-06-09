import fs from 'node:fs';
import path from 'node:path';

process.env.NODE_ENV = 'test';

const { configUmiAlias, createConfig } = await import('@umijs/max/test.js');

const uiRoot = process.cwd();
const umiTestExportsPath = path.join(uiRoot, 'src', '.umi-test', 'exports.ts');

await configUmiAlias({
  ...createConfig({
    target: 'browser',
  }),
});

if (!fs.existsSync(umiTestExportsPath)) {
  throw new Error(`Umi test exports were not generated: ${path.relative(uiRoot, umiTestExportsPath)}`);
}
