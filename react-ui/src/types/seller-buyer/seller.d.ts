declare namespace API.Partner {
  export interface Seller {
    sellerId?: number;
    sellerNo?: string;
    sellerCode?: string;
    sellerName?: string;
    sellerShortName?: string;
    sellerType?: string;
    sellerLevel?: string;
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

  export interface SellerListParams {
    sellerNo?: string;
    sellerCode?: string;
    sellerName?: string;
    sellerShortName?: string;
    username?: string;
    companyName?: string;
    phone?: string;
    sellerLevel?: string;
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

  export interface SellerPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Seller[];
  }

  export interface SellerInfoResult {
    code: number;
    msg: string;
    data: Seller;
  }

  export interface SellerAccount extends PortalAccountBase {
    sellerAccountId?: number;
    sellerId?: number;
    lockStatus?: string;
    lockReason?: string;
  }

  export interface SellerAccountPayload extends PortalAccountPayload {
    sellerAccountId?: number;
    sellerId?: number;
    lockStatus?: string;
    lockReason?: string;
  }

  export interface SellerAccountListResult {
    code: number;
    msg: string;
    data: SellerAccount[];
  }
}
