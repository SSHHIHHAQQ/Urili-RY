import PartnerManagementPage, { type PartnerModuleConfig } from '@/components/PartnerManagement/PartnerManagementPage';
import {
  addAdminSeller,
  addAdminSellerAccount,
  changeAdminSellerStatus,
  createAdminSellerDirectLogin,
  forceLogoutAdminSellerAccountSessions,
  forceLogoutAdminSellerSessions,
  getAdminSeller,
  getAdminSellerAccounts,
  getAdminSellerDeptTree,
  getAdminSellerList,
  resetAdminSellerAccountDefaultPassword,
  resetAdminSellerOwnerPassword,
  updateAdminSeller,
  updateAdminSellerAccount,
} from '@/services/seller/seller';

const sellerConfig: PartnerModuleConfig = {
  moduleKey: 'seller',
  title: '卖家管理',
  label: '卖家',
  idField: 'sellerId',
  noField: 'sellerNo',
  codeField: 'sellerCode',
  nameField: 'sellerName',
  shortNameField: 'sellerShortName',
  typeField: 'sellerType',
  levelField: 'sellerLevel',
  ownerIdField: 'sellerId',
  accountIdField: 'sellerAccountId',
  balanceTitle: '分销账户余额',
  levelDictType: 'seller_level',
  services: {
    list: getAdminSellerList,
    get: getAdminSeller,
    add: addAdminSeller,
    update: updateAdminSeller,
    changeStatus: changeAdminSellerStatus,
    getAccounts: getAdminSellerAccounts,
    addAccount: addAdminSellerAccount,
    updateAccount: updateAdminSellerAccount,
    getDeptTree: getAdminSellerDeptTree,
    resetAccountDefaultPassword: resetAdminSellerAccountDefaultPassword,
    forceLogoutSubject: forceLogoutAdminSellerSessions,
    forceLogoutAccount: forceLogoutAdminSellerAccountSessions,
    resetOwnerPassword: resetAdminSellerOwnerPassword,
    directLogin: createAdminSellerDirectLogin,
  },
};

export default function SellerPage() {
  return <PartnerManagementPage config={sellerConfig} />;
}
