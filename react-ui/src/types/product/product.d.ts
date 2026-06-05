declare namespace API.Product {
  export interface Category {
    categoryId?: number;
    parentId?: number;
    ancestors?: string;
    categoryCode?: string;
    categoryName?: string;
    categoryLevel?: number;
    fullPath?: string;
    publishEnabled?: string;
    sortOrder?: number;
    schemaVersion?: number;
    status?: string;
    childrenCount?: number;
    keyword?: string;
    leafOnly?: boolean;
    loadingPlaceholder?: boolean;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
    children?: Category[];
  }

  export interface Attribute {
    attributeId?: number;
    attributeCode?: string;
    attributeName?: string;
    attributeType?: string;
    optionSource?: string;
    dictType?: string;
    unit?: string;
    valuePrecision?: number;
    status?: string;
    keyword?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
    options?: AttributeOption[];
  }

  export interface AttributeOption {
    optionId?: number;
    attributeId?: number;
    optionCode?: string;
    optionLabel?: string;
    sortOrder?: number;
    defaultFlag?: string;
    status?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface CategoryAttribute {
    categoryAttributeId?: number;
    categoryId?: number;
    categoryName?: string;
    sourceCategoryName?: string;
    attributeId?: number;
    attributeCode?: string;
    attributeName?: string;
    attributeType?: string;
    optionSource?: string;
    dictType?: string;
    unit?: string;
    valuePrecision?: number;
    options?: AttributeOption[];
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
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface ImportMessage {
    rowNum?: number;
    action?: string;
    status?: string;
    message?: string;
  }

  export interface ImportResult {
    totalCount?: number;
    createCount?: number;
    updateCount?: number;
    skipCount?: number;
    errorCount?: number;
    passed?: boolean;
    messages?: ImportMessage[];
  }

  export interface ListResult<T> {
    code: number;
    msg: string;
    rows: T[];
  }

  export interface PageResult<T> {
    code: number;
    msg: string;
    total: number;
    rows: T[];
  }

  export interface InfoResult<T> {
    code: number;
    msg: string;
    data: T;
  }

  export interface ImportResultResponse extends InfoResult<ImportResult> {}
}
