declare namespace API.Partner {
  export interface Buyer {
    buyerId?: number;
    buyerNo?: string;
    buyerCode?: string;
    buyerName?: string;
    buyerShortName?: string;
    buyerType?: string;
    buyerLevel?: string;
    status?: string;
    username?: string;
    companyName?: string;
    legalId?: string;
    businessLicenseNo?: string;
    countryCode?: string;
    stateProvince?: string;
    state?: string;
    city?: string;
    postalCode?: string;
    addressLine1?: string;
    addressLine2?: string;
    address1?: string;
    address2?: string;
    contactName?: string;
    contactPhone?: string;
    contactEmail?: string;
    phone?: string;
    email?: string;
    attachment?: PartyAttachment | null;
    attachmentFileName?: string;
    attachmentMimeType?: string;
    attachmentSizeBytes?: number;
    attachmentFileUrl?: string;
    accountCount?: number;
    accountBalance?: number | string;
    balanceCurrency?: string;
    lastLoginTime?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface BuyerListParams {
    buyerNo?: string;
    buyerCode?: string;
    buyerName?: string;
    buyerShortName?: string;
    username?: string;
    companyName?: string;
    phone?: string;
    buyerLevel?: string;
    status?: string;
    countryCode?: string;
    city?: string;
    balanceMin?: string;
    balanceMax?: string;
    createTimeRange?: string[];
    lastLoginTimeRange?: string[];
    pageNum?: number;
    pageSize?: number;
    current?: number;
  }

  export interface BuyerPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Buyer[];
  }

  export interface BuyerInfoResult {
    code: number;
    msg: string;
    data: Buyer;
  }

  export interface BuyerAccount extends PortalAccountBase {
    buyerAccountId?: number;
    buyerId?: number;
  }

  export interface BuyerAccountListResult {
    code: number;
    msg: string;
    data: BuyerAccount[];
  }
}
