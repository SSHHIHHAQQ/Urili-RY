declare namespace API.Partner {
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
