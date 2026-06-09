import { matchPerm, matchPermission, matchPerms } from '@/utils/permission';

describe('permission matching contract', () => {
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('matches exact permissions and keeps seller and buyer namespaces isolated', () => {
    expect(matchPermission(['seller:admin:list'], 'seller:admin:list')).toBe(true);
    expect(matchPermission(['seller:admin:list'], 'buyer:admin:list')).toBe(false);
    expect(matchPermission(['buyer:product:distribution:query'], 'seller:product:distribution:query')).toBe(false);
  });

  it('allows the RuoYi global wildcard without treating terminal prefixes as wildcards', () => {
    expect(matchPerm(['*:*:*'], 'buyer:admin:list')).toBe(true);
    expect(matchPerm(['seller:*:*'], 'seller:admin:list')).toBe(false);
    expect(matchPerm(['seller:admin:*'], 'seller:admin:list')).toBe(false);
  });

  it('fails closed for empty, undefined, or malformed permission requirements', () => {
    expect(matchPermission(undefined, 'seller:admin:list')).toBe(false);
    expect(matchPermission([], 'seller:admin:list')).toBe(false);
    expect(matchPermission(['seller:admin:list'], '')).toBe(false);
    expect(matchPermission(['seller:admin:list'], [])).toBe(false);
  });

  it('matches any listed required permission by exact code', () => {
    expect(matchPerms(['seller:admin:list'], ['buyer:admin:list', 'seller:admin:list'])).toBe(true);
    expect(matchPerms(['seller:admin:list'], ['buyer:admin:list', 'seller:admin:edit'])).toBe(false);
  });
});
