declare namespace API.Partner {
  export interface PartyAttachment {
    fileName: string;
    mimeType: string;
    sizeBytes: number;
    fileUrl: string;
    dataUrl?: string;
  }

  export interface PortalAccountBase {
    userId?: number;
    accountRole?: string;
    status?: string;
    userName?: string;
    nickName?: string;
    password?: string;
    email?: string;
    phonenumber?: string;
    userStatus?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface DirectLoginResult {
    token: string;
    loginUrl: string;
    expireMinutes: number;
    expireTime: string;
    userId: number;
    username: string;
  }

  export interface DirectLoginApiResult {
    code: number;
    msg: string;
    data: DirectLoginResult;
  }
}
