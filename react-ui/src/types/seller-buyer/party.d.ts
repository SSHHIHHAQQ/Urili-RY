declare namespace API.Partner {
  export type PortalTerminal = 'seller' | 'buyer';

  export interface PartyAttachment {
    fileName: string;
    mimeType: string;
    sizeBytes: number;
    fileUrl: string;
    dataUrl?: string;
  }

  export interface PortalAccountBase {
    accountId?: number;
    deptId?: number;
    deptName?: string;
    accountRole?: string;
    status?: string;
    userName?: string;
    nickName?: string;
    password?: string;
    email?: string;
    phonenumber?: string;
    userStatus?: string;
    lastLoginIp?: string;
    lastLoginTime?: string;
    pwdUpdateTime?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface PortalDept {
    deptId?: number;
    subjectId?: number;
    parentId?: number;
    ancestors?: string;
    deptName?: string;
    parentName?: string;
    orderNum?: number;
    leader?: string;
    phone?: string;
    email?: string;
    status?: string;
    children?: PortalDept[];
  }

  export interface PortalTreeNode {
    id: number;
    label: string;
    children?: PortalTreeNode[];
  }

  export interface PortalMenu {
    menuId?: number;
    menuName?: string;
    parentName?: string;
    parentId?: number;
    orderNum?: number;
    path?: string;
    component?: string;
    query?: string;
    routeName?: string;
    isFrame?: string;
    isCache?: string;
    menuType?: string;
    visible?: string;
    status?: string;
    perms?: string;
    icon?: string;
    remark?: string;
    children?: PortalMenu[];
  }

  export interface PortalMenuListResult {
    code: number;
    msg: string;
    data: PortalMenu[];
  }

  export interface PortalMenuInfoResult {
    code: number;
    msg: string;
    data: PortalMenu;
  }

  export interface PortalDeptListResult {
    code: number;
    msg: string;
    data: PortalDept[];
  }

  export interface PortalDeptInfoResult {
    code: number;
    msg: string;
    data: PortalDept;
  }

  export interface PortalDeptTreeResult {
    code: number;
    msg: string;
    data: PortalTreeNode[];
  }

  export interface PortalRole {
    roleId?: number;
    subjectId?: number;
    roleName?: string;
    roleKey?: string;
    roleSort?: number;
    status?: string;
    remark?: string;
    menuIds?: number[];
  }

  export interface PortalRolePageResult {
    code: number;
    msg: string;
    total: number;
    rows: PortalRole[];
  }

  export interface PortalRoleInfoResult {
    code: number;
    msg: string;
    data: PortalRole;
  }

  export interface PortalAccountRoleResult {
    code: number;
    msg: string;
    roles: PortalRole[];
    checkedKeys: number[];
  }

  export interface PortalRoleMenuTreeResult {
    code: number;
    msg: string;
    menus: PortalTreeNode[];
    checkedKeys: number[];
  }

  export interface PortalMenuTreeResult {
    code: number;
    msg: string;
    data: PortalTreeNode[];
  }

  export interface DirectLoginResult {
    token: string;
    ticketId?: number;
    loginUrl: string;
    expireMinutes: number;
    expireTime: string;
    accountId: number;
    username: string;
  }

  export interface DirectLoginApiResult {
    code: number;
    msg: string;
    data: DirectLoginResult;
  }

  export interface PortalLoginParams {
    username?: string;
    password?: string;
    code?: string;
    uuid?: string;
  }

  export interface PortalLoginResultData {
    token: string;
    terminal: PortalTerminal;
    subjectId: number;
    subjectNo?: string;
    accountId: number;
    username: string;
    nickName?: string;
    expireMinutes?: number;
    expireTime?: string;
  }

  export interface PortalLoginApiResult {
    code: number;
    msg: string;
    data: PortalLoginResultData;
  }

  export interface PortalPasswordChangeParams {
    oldPassword: string;
    newPassword: string;
    confirmPassword: string;
  }

  export interface PortalPermissionInfo {
    terminal?: PortalTerminal;
    subjectId?: number;
    subjectNo?: string;
    accountId?: number;
    userName?: string;
    nickName?: string;
    roles?: string[];
    permissions?: string[];
  }

  export interface PortalPermissionInfoResult {
    code: number;
    msg: string;
    data: PortalPermissionInfo;
  }

  export interface PortalSubjectProfile {
    terminal?: PortalTerminal;
    subjectId?: number;
    subjectNo?: string;
    subjectCode?: string;
    subjectName?: string;
    subjectShortName?: string;
    subjectType?: string;
    subjectLevel?: string;
    status?: string;
    countryCode?: string;
    stateProvince?: string;
    city?: string;
    postalCode?: string;
    addressLine1?: string;
    addressLine2?: string;
    contactName?: string;
    contactPhone?: string;
    contactEmail?: string;
    attachment?: PartyAttachment;
    accountBalance?: number;
    balanceCurrency?: string;
  }

  export interface PortalSubjectProfileResult {
    code: number;
    msg: string;
    data: PortalSubjectProfile;
  }

  export interface PortalAccountProfile {
    terminal?: PortalTerminal;
    subjectId?: number;
    accountId?: number;
    deptId?: number;
    deptName?: string;
    accountRole?: string;
    status?: string;
    userName?: string;
    nickName?: string;
    email?: string;
    phonenumber?: string;
    lastLoginTime?: string;
    pwdUpdateTime?: string;
  }

  export interface PortalAccountProfileResult {
    code: number;
    msg: string;
    data: PortalAccountProfile;
  }

  export interface PortalAccountListResult {
    code: number;
    msg: string;
    data: PortalAccountProfile[];
  }

  export interface PortalDeptProfile {
    terminal?: PortalTerminal;
    subjectId?: number;
    deptId?: number;
    parentId?: number;
    parentName?: string;
    deptName?: string;
    orderNum?: number;
    leader?: string;
    phone?: string;
    email?: string;
    status?: string;
  }

  export interface PortalDeptProfileListResult {
    code: number;
    msg: string;
    data: PortalDeptProfile[];
  }

  export interface PortalRoleProfile {
    terminal?: PortalTerminal;
    subjectId?: number;
    roleId?: number;
    roleName?: string;
    roleKey?: string;
    roleSort?: number;
    status?: string;
  }

  export interface PortalRoleProfileListResult {
    code: number;
    msg: string;
    data: PortalRoleProfile[];
  }

  export interface PortalAuditPageResult<T> {
    code: number;
    msg: string;
    total: number;
    rows: T[];
  }

  export interface PortalLoginLog {
    infoId?: number;
    subjectId?: number;
    accountId?: number;
    userName?: string;
    ipaddr?: string;
    loginLocation?: string;
    browser?: string;
    os?: string;
    status?: string;
    msg?: string;
    loginTime?: string;
  }

  export interface PortalOperLog {
    operId?: number;
    subjectId?: number;
    accountId?: number;
    title?: string;
    businessType?: number;
    method?: string;
    requestMethod?: string;
    operName?: string;
    operUrl?: string;
    operIp?: string;
    operLocation?: string;
    operParam?: string;
    jsonResult?: string;
    status?: number;
    errorMsg?: string;
    operTime?: string;
    costTime?: number;
  }

  export interface PortalSessionProfile {
    terminal?: PortalTerminal;
    subjectId?: number;
    accountId?: number;
    userName?: string;
    loginIp?: string;
    loginTime?: string;
    expireTime?: string;
    logoutTime?: string;
    status?: string;
    current?: boolean;
  }

  export interface PortalProductCategory {
    categoryId?: number;
    parentId?: number;
    categoryCode?: string;
    categoryName?: string;
    categoryLevel?: number;
    publishEnabled?: string;
    sortOrder?: number;
    schemaVersion?: number;
    childrenCount?: number;
  }

  export interface PortalProductAttributeOption {
    optionCode?: string;
    optionLabel?: string;
    sortOrder?: number;
    defaultFlag?: string;
    status?: string;
  }

  export interface PortalProductCategorySchemaItem {
    categoryId?: number;
    sourceCategoryName?: string;
    attributeId?: number;
    attributeCode?: string;
    attributeName?: string;
    attributeType?: string;
    optionSource?: string;
    dictType?: string;
    unit?: string;
    ruleMode?: string;
    requiredFlag?: string;
    visibleFlag?: string;
    editableFlag?: string;
    filterableFlag?: string;
    groupCode?: string;
    sortOrder?: number;
    placeholder?: string;
    helpText?: string;
    validationRule?: string;
    status?: string;
    options?: PortalProductAttributeOption[];
  }

  export interface PortalProductCategoryListResult {
    code: number;
    msg: string;
    data: PortalProductCategory[];
  }

  export interface PortalProductSchemaResult {
    code: number;
    msg: string;
    data: PortalProductCategorySchemaItem[];
  }

  export interface PortalDirectLoginTicket {
    ticketId?: number;
    terminal?: string;
    targetSubjectId?: number;
    targetSubjectNo?: string;
    targetAccountId?: number;
    targetUserName?: string;
    actingAdminId?: number;
    actingAdminName?: string;
    reason?: string;
    expireTime?: string;
    usedTime?: string;
    usedIp?: string;
    status?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }
}
