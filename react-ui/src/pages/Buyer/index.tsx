import PartnerManagementPage, { type PartnerModuleConfig } from '@/components/PartnerManagement/PartnerManagementPage';
import {
  addAdminBuyer,
  addAdminBuyerAccount,
  changeAdminBuyerStatus,
  createAdminBuyerDirectLogin,
  forceLogoutAdminBuyerAccountSessions,
  forceLogoutAdminBuyerSessions,
  getAdminBuyer,
  getAdminBuyerAccounts,
  getAdminBuyerDeptTree,
  getAdminBuyerList,
  resetAdminBuyerAccountDefaultPassword,
  resetAdminBuyerOwnerPassword,
  updateAdminBuyer,
  updateAdminBuyerAccount,
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
  ownerIdField: 'buyerId',
  accountIdField: 'buyerAccountId',
  balanceTitle: '账户余额',
  showRechargePlaceholder: true,
  levelDictType: 'buyer_level',
  services: {
    list: getAdminBuyerList,
    get: getAdminBuyer,
    add: addAdminBuyer,
    update: updateAdminBuyer,
    changeStatus: changeAdminBuyerStatus,
    getAccounts: getAdminBuyerAccounts,
    addAccount: addAdminBuyerAccount,
    updateAccount: updateAdminBuyerAccount,
    getDeptTree: getAdminBuyerDeptTree,
    resetAccountDefaultPassword: resetAdminBuyerAccountDefaultPassword,
    forceLogoutSubject: forceLogoutAdminBuyerSessions,
    forceLogoutAccount: forceLogoutAdminBuyerAccountSessions,
    resetOwnerPassword: resetAdminBuyerOwnerPassword,
    directLogin: createAdminBuyerDirectLogin,
  },
};

export default function BuyerPage() {
  return <PartnerManagementPage config={buyerConfig} />;
}
