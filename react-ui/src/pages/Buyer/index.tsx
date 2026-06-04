import PartnerManagementPage, { type PartnerModuleConfig } from '@/components/PartnerManagement/PartnerManagementPage';
import {
  addAdminBuyer,
  addAdminBuyerAccount,
  changeAdminBuyerStatus,
  createAdminBuyerDirectLogin,
  getAdminBuyer,
  getAdminBuyerAccounts,
  getAdminBuyerList,
  resetAdminBuyerAccountDefaultPassword,
  resetAdminBuyerAccountPassword,
  resetAdminBuyerOwnerPassword,
  updateAdminBuyer,
} from '@/services/buyer/buyer';

const buyerConfig: PartnerModuleConfig = {
  moduleKey: 'buyer',
  title: '买家管理',
  label: '买家',
  idField: 'buyerId',
  noField: 'buyerNo',
  codeField: 'buyerCode',
  nameField: 'buyerName',
  shortNameField: 'buyerShortName',
  typeField: 'buyerType',
  levelField: 'buyerLevel',
  accountIdField: 'buyerAccountId',
  ownerIdField: 'buyerId',
  balanceTitle: '账户余额',
  showRechargePlaceholder: true,
  levelDictType: 'buyer_level',
  accountRoleDictType: 'buyer_account_role',
  services: {
    list: getAdminBuyerList,
    get: getAdminBuyer,
    add: addAdminBuyer,
    update: updateAdminBuyer,
    changeStatus: changeAdminBuyerStatus,
    listAccounts: getAdminBuyerAccounts,
    addAccount: addAdminBuyerAccount,
    resetPassword: resetAdminBuyerAccountPassword,
    resetDefaultPassword: resetAdminBuyerAccountDefaultPassword,
    resetOwnerPassword: resetAdminBuyerOwnerPassword,
    directLogin: createAdminBuyerDirectLogin,
  },
};

export default function BuyerPage() {
  return <PartnerManagementPage config={buyerConfig} />;
}
