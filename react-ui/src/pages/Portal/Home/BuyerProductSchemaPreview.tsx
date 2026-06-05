import React from 'react';
import {
  getBuyerPortalProductCategories,
  getBuyerPortalProductSchema,
} from '@/services/portal/session';
import { PortalProductSchemaPreview } from './SellerProductSchemaPreview';

const BuyerProductSchemaPreview: React.FC = () => (
  <PortalProductSchemaPreview
    title="商品浏览准备"
    getCategories={getBuyerPortalProductCategories}
    getSchema={getBuyerPortalProductSchema}
  />
);

export default BuyerProductSchemaPreview;
