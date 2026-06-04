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

  export interface PortalDeptListResult {
    code: number;
    msg: string;
    data: PortalDept[];
  }

  export interface PortalDeptTreeResult {
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
}
