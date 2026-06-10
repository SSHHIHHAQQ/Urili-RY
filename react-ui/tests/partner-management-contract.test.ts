import fs from 'fs';
import path from 'path';
import {
  getAdminBuyerAccountSessions,
  getAdminBuyerSessions,
} from '@/services/buyer/buyer';
import {
  getAdminSellerAccountSessions,
  getAdminSellerSessions,
} from '@/services/seller/seller';
import { request } from '@umijs/max';

jest.mock('@umijs/max', () => ({
  request: jest.fn(),
}));

const uiRoot = path.resolve(__dirname, '..');
const mockedRequest = request as jest.Mock;

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

describe('partner management contract', () => {
  const accountModal = readSource('src/components/PartnerManagement/PartnerAccountModal.tsx');
  const managementPage = readSource('src/components/PartnerManagement/PartnerManagementPage.tsx');
  const sellerPage = readSource('src/pages/Seller/index.tsx');
  const buyerPage = readSource('src/pages/Buyer/index.tsx');
  const sellerService = readSource('src/services/seller/seller.ts');
  const buyerService = readSource('src/services/buyer/buyer.ts');
  const sessionParams = readSource('src/services/seller-buyer/sessionParams.ts');

  beforeEach(() => {
    mockedRequest.mockReset();
    mockedRequest.mockResolvedValue({ code: 200, rows: [], total: 0 });
  });

  it('keeps account role assignment gated by role query, account role query, and account role edit', () => {
    expect(accountModal).toContain('const canQueryRole = access.hasPerms(`${permPrefix}:role:query`);');
    expect(accountModal).toMatch(/const canAssignAccountRoles = canQueryRole\s*&&\s*access\.hasPerms\(accountPermissions\.roleQuery\)\s*&&\s*access\.hasPerms\(accountPermissions\.roleEdit\);/);
    expect(accountModal).toContain('hidden={!canAssignAccountRoles}');
    expect(sellerPage).toContain("roleQuery: 'seller:admin:account:role:query'");
    expect(sellerPage).toContain("roleEdit: 'seller:admin:account:role:edit'");
    expect(buyerPage).toContain("roleQuery: 'buyer:admin:account:role:query'");
    expect(buyerPage).toContain("roleEdit: 'buyer:admin:account:role:edit'");
  });

  it('keeps reset password as temporary password input instead of default password reset', () => {
    expect(accountModal).toContain('请输入临时密码。重置后该账号当前在线会话会立即失效。');
    expect(accountModal).toContain('label="临时密码"');
    expect(accountModal).toContain('请输入5-20位临时密码');
    expect(accountModal).toContain('config.services.resetAccountPassword(partnerId, accountId, normalizePassword(values.password) || \'\')');
    expect(accountModal).not.toContain('U12346');
    expect(accountModal).not.toContain('默认密码');
    expect(sellerService).not.toContain('resetDefaultPwd');
    expect(buyerService).not.toContain('resetDefaultPwd');
  });

  it('does not allow legacy data url attachments to be submitted again', () => {
    expect(managementPage).not.toContain('function isLegacyDataUrl');
    expect(managementPage).not.toContain('|| isLegacyDataUrl(fileUrl)');
    expect(managementPage).not.toContain('attachment?.dataUrl');
    expect(managementPage).toContain('if (isManagedAttachmentUrl(fileUrl))');
    expect(managementPage).toContain("throw new Error('UNSUPPORTED_ATTACHMENT_URL');");
  });

  it('keeps seller and buyer admin service paths isolated', () => {
    expect(sellerService).toContain('/api/seller/admin/sellers/list');
    expect(sellerService).toContain('/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/resetPwd');
    expect(sellerService).toContain('/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/directLogin');
    expect(sellerService).not.toContain('/api/buyer/');

    expect(buyerService).toContain('/api/buyer/admin/buyers/list');
    expect(buyerService).toContain('/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/resetPwd');
    expect(buyerService).toContain('/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/directLogin');
    expect(buyerService).not.toContain('/api/seller/');
  });

  it('keeps session list readonly permission separate from force logout', () => {
    expect(managementPage).toContain('access.hasPerms(`${permPrefix}:session:list`) && config.services.listSubjectSessions');
    expect(managementPage).toContain('access.hasPerms(`${permPrefix}:forceLogout`)');
    expect(accountModal).toContain('access.hasPerms(`${permPrefix}:session:list`) && config.services.listAccountSessions');
    expect(accountModal).toContain('access.hasPerms(`${permPrefix}:forceLogout`)');

    expect(sellerService).toContain('/sessions/list');
    expect(sellerService).toMatch(/forceLogoutAdminSellerSessions[\s\S]*method: 'DELETE'/);
    expect(sellerService).toMatch(/getAdminSellerSessions[\s\S]*method: 'GET'/);
    expect(buyerService).toContain('/sessions/list');
    expect(buyerService).toMatch(/forceLogoutAdminBuyerSessions[\s\S]*method: 'DELETE'/);
    expect(buyerService).toMatch(/getAdminBuyerSessions[\s\S]*method: 'GET'/);
  });

  it('keeps admin session list params limited to pagination', () => {
    expect(managementPage).toContain('params?: API.Partner.PartnerSessionPageParams');
    expect(sellerService).toContain('sanitizePartnerSessionPageParams');
    expect(buyerService).toContain('sanitizePartnerSessionPageParams');
    expect(sellerService).toContain('params?: API.Partner.PartnerSessionPageParams');
    expect(buyerService).toContain('params?: API.Partner.PartnerSessionPageParams');
    expect(sellerService).not.toContain('getAdminSellerSessions(sellerId: number, params?: Record<string, any>)');
    expect(sellerService).not.toContain('getAdminSellerAccountSessions(sellerId: number, sellerAccountId: number, params?: Record<string, any>)');
    expect(buyerService).not.toContain('getAdminBuyerSessions(buyerId: number, params?: Record<string, any>)');
    expect(buyerService).not.toContain('getAdminBuyerAccountSessions(buyerId: number, buyerAccountId: number, params?: Record<string, any>)');
    expect(sessionParams).toContain('result.pageNum = params.pageNum;');
    expect(sessionParams).toContain('result.pageSize = params.pageSize;');
    expect(sessionParams).not.toContain('...params');
  });

  it('strips non-pagination params from admin seller and buyer session list requests', async () => {
    const unsafeParams = {
      pageNum: 2,
      pageSize: 20,
      subjectId: 999,
      accountId: 888,
      ipaddr: '127.0.0.1',
      tokenId: 'other-terminal-token',
    } as any;

    await getAdminSellerSessions(11, unsafeParams);
    await getAdminSellerAccountSessions(11, 22, unsafeParams);
    await getAdminBuyerSessions(33, unsafeParams);
    await getAdminBuyerAccountSessions(33, 44, unsafeParams);

    expect(mockedRequest).toHaveBeenNthCalledWith(1, '/api/seller/admin/sellers/11/sessions/list', {
      method: 'GET',
      params: { pageNum: 2, pageSize: 20 },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(2, '/api/seller/admin/sellers/11/accounts/22/sessions/list', {
      method: 'GET',
      params: { pageNum: 2, pageSize: 20 },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(3, '/api/buyer/admin/buyers/33/sessions/list', {
      method: 'GET',
      params: { pageNum: 2, pageSize: 20 },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(4, '/api/buyer/admin/buyers/33/accounts/44/sessions/list', {
      method: 'GET',
      params: { pageNum: 2, pageSize: 20 },
    });
  });
});
