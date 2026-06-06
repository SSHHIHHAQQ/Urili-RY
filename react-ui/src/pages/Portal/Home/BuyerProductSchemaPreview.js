import { jsx as _jsx } from "react/jsx-runtime";
import { getBuyerPortalProductCategories, getBuyerPortalProductSchema, } from '@/services/portal/session';
import { PortalProductSchemaPreview } from './SellerProductSchemaPreview';
const BuyerProductSchemaPreview = () => (_jsx(PortalProductSchemaPreview, { title: "\u5546\u54C1\u6D4F\u89C8\u51C6\u5907", getCategories: getBuyerPortalProductCategories, getSchema: getBuyerPortalProductSchema }));
export default BuyerProductSchemaPreview;
