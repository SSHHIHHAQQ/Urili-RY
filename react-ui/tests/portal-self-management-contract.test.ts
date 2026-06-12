import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');

function readUiSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function extractInterfaceBody(source: string, interfaceName: string) {
  const match = source.match(new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n  \\}`));
  if (!match) {
    throw new Error(`Missing interface ${interfaceName}`);
  }
  return match[1];
}

describe('portal self-management contract', () => {
  const page = readUiSource('src/pages/Portal/Home/PortalSelfManagement.tsx');
  const home = readUiSource('src/pages/Portal/Home/index.tsx');
  const routes = readUiSource('config/routes.ts');
  const types = readUiSource('src/types/seller-buyer/party.d.ts');

  it('keeps the self-management surface inside the terminal portal service boundary', () => {
    expect(page).toContain("import { PORTAL_SERVICE, type PortalTerminal } from '../terminal';");
    expect(page).toContain('const service = PORTAL_SERVICE[terminal];');
    expect(page).not.toContain("from '@umijs/max'");
    expect(page).not.toContain('request<');
    expect(page).not.toContain('/api/seller');
    expect(page).not.toContain('/api/buyer');
    expect(page).not.toContain('sellerId');
    expect(page).not.toContain('buyerId');
    expect(page).not.toContain('subjectId');
  });

  it('gates portal self-management actions through terminal-scoped permissions', () => {
    expect(page).toContain('matchPermission(permissions, `${terminal}:${permission}`)');
    for (const permission of [
      'account:list',
      'account:add',
      'account:edit',
      'account:role:query',
      'account:role:edit',
      'dept:list',
      'dept:add',
      'dept:edit',
      'dept:remove',
      'role:list',
      'role:query',
      'role:add',
      'role:edit',
      'role:remove',
      'account:loginLog:list',
      'account:operLog:list',
    ]) {
      expect(page).toContain(`'${permission}'`);
    }
    expect(page).toContain('const canAssignAccountRoles = canViewRoles && canQueryAccountRole && canEditAccountRole;');
    expect(page).toContain('const canCreateRole = canAddRole && canQueryRole;');
    expect(page).toContain('{canAssignAccountRoles ? (');
    expect(page).toContain('extra={canCreateRole ? <Button type="primary" onClick={openRoleCreate}>');
  });

  it('covers the minimal account, role, department, audit, and session-adjacent portal loop', () => {
    for (const call of [
      'service.createAccount',
      'service.updateAccount',
      'service.getAccountRoles',
      'service.assignAccountRoles',
      'service.createDept',
      'service.updateDept',
      'service.deleteDept',
      'service.getRoleMenus',
      'service.getRole',
      'service.createRole',
      'service.updateRole',
      'service.deleteRole',
      'service.getLoginLogs',
      'service.getOperLogs',
    ]) {
      expect(page).toContain(call);
    }

    expect(home).toContain("portalPermission(terminal, 'account:session:list')");
    expect(home).toContain('PORTAL_SERVICE[currentTerminal].getSessions({ pageNum: 1, pageSize })');
    expect(home).toContain('<PortalSelfManagement');
    expect(home).toContain('activeView={activeView}');
    expect(home).toContain('accounts={data.accounts || []}');
    expect(home).toContain('depts={data.depts || []}');
    expect(home).toContain('roles={data.roles || []}');
    expect(home).toContain('permissions={permissions}');
    expect(home).toContain('onChanged={handleRefresh}');
  });

  it('adds portal shell routes and keeps them outside the admin remote-menu layout', () => {
    for (const terminal of ['seller', 'buyer']) {
      for (const view of [
        'workbench',
        'accounts',
        'roles',
        'depts',
        'sessions',
        'loginLogs',
        'operLogs',
      ]) {
        expect(routes).toContain(`path: '/${terminal}/portal/${view}'`);
      }
    }
    expect(routes).toContain("path: '/buyer/portal/product-center'");
    expect(home).toContain("productCenter: '商品中心'");
    expect(home).toContain("permission: 'product:center:list'");
    expect(home).toContain('<BuyerProductCenter permissions={permissions} />');
    expect(home).toContain("name: '组织权限'");
    expect(home).toContain('<ProLayout');
    expect(home).toContain('<SelectLang key="SelectLang" />');
    expect(home).toContain("import defaultSettings from '../../../../config/defaultSettings';");
    expect(home).toContain("import HeaderDropdown from '@/components/HeaderDropdown';");
    expect(home).toContain("'个人中心'");
    expect(home).not.toContain('Question');
    expect(home).not.toContain('getRemoteMenu');
    expect(home).not.toContain('getRoutersInfo');
  });

  it('keeps portal online sessions as a read-only menu page', () => {
    expect(home).toContain("sessions: '在线会话'");
    expect(home).toContain('不提供强制下线操作');
    expect(home).toContain("permission: 'account:session:list'");
    expect(home).not.toContain('forceLogout');
    expect(home).not.toContain('deleteSession');
    expect(home).not.toContain('强制踢出');
    expect(page).not.toContain('forceLogout');
    expect(page).not.toContain('deleteSession');
    expect(page).not.toContain('强制踢出');
  });

  it('keeps the current portal home entry disconnected from frozen business surfaces except buyer product center', () => {
    for (const frozenFragment of [
      'SellerOwnDistributionProductList',
      'BuyerDistributionProductList',
      'SellerProductSchemaPreview',
      'BuyerProductSchemaPreview',
      'DistributionProduct',
      'ProductSchema',
      'product/distribution',
      'product/categories',
      ':product:distribution',
      ':product:category',
      ':product:schema',
      ':order:',
      ':inventory:',
      ':logistics:',
      ':finance:',
      ':fulfillment:',
      ':integration:',
    ]) {
      expect(home).not.toContain(frozenFragment);
      expect(page).not.toContain(frozenFragment);
    }

    expect(routes).toContain("path: '/seller/portal'");
    expect(routes).toContain("path: '/buyer/portal'");
    expect(routes).toContain("component: './Portal/Home'");
    expect(routes).toContain("path: '/buyer/portal/product-center'");
    expect(routes).not.toContain("path: '/seller/portal/product");
    expect(routes).not.toContain("path: '/buyer/portal/product/");
    expect(routes).not.toContain("path: '/seller/portal/order");
    expect(routes).not.toContain("path: '/buyer/portal/order");
  });

  it('uses the platform-provided role menu template instead of exposing menu definition controls', () => {
    expect(page).toContain('service.getRoleMenus()');
    expect(page).toMatch(/\.getRoleMenus\(\s*record\.roleId\s*\)/);
    expect(page).toContain('roleForm.setFieldsValue({ status:');
    expect(page).toContain('menuIds: menuResponse.checkedKeys || []');
    expect(page).not.toContain('createMenu');
    expect(page).not.toContain('updateMenu');
    expect(page).not.toContain('deleteMenu');
    expect(page).not.toContain('menuDefinition');
  });

  it('keeps portal-visible own audit and session types free of internal audit fields', () => {
    const forbiddenFields = [
      'sellerId',
      'buyerId',
      'subjectId',
      'accountId',
      'sellerAccountId',
      'buyerAccountId',
      'terminal',
      'tokenId',
      'directLogin',
      'directLoginTicketId',
      'actingAdminId',
      'actingAdminName',
      'directLoginReason',
      'operParam',
      'jsonResult',
    ];

    for (const interfaceName of [
      'PortalOwnLoginLogProfile',
      'PortalOwnOperLogProfile',
      'PortalOwnSessionProfile',
    ]) {
      const body = extractInterfaceBody(types, interfaceName);
      for (const field of forbiddenFields) {
        expect(body).not.toContain(`${field}?:`);
      }
    }
  });
});
