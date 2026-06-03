import PartnerManagementPage, { type PartnerModuleConfig } from '@/components/PartnerManagement/PartnerManagementPage';
import {
  addAdminSeller,
  addAdminSellerAccount,
  changeAdminSellerStatus,
  getAdminSeller,
  getAdminSellerAccounts,
  getAdminSellerList,
  resetAdminSellerAccountPassword,
  updateAdminSeller,
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
  accountIdField: 'sellerAccountId',
  ownerIdField: 'sellerId',
  levelDictType: 'seller_level',
  accountRoleDictType: 'seller_account_role',
  services: {
    list: getAdminSellerList,
    get: getAdminSeller,
    add: addAdminSeller,
    update: updateAdminSeller,
    changeStatus: changeAdminSellerStatus,
    listAccounts: getAdminSellerAccounts,
    addAccount: addAdminSellerAccount,
    resetPassword: resetAdminSellerAccountPassword,
  },
};

export default function SellerPage() {
  return <PartnerManagementPage config={sellerConfig} />;
}
