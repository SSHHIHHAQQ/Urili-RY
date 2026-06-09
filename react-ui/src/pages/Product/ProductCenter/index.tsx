import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import ProductCenterPage from '@/components/ProductCenter/ProductCenterPage';
import {
  getProductCenterList,
  getProductCenterProduct,
} from '@/services/product/productCenter';

export default function AdminProductCenterPage() {
  const access = useAccess();

  return (
    <PageContainer title={false}>
      <ProductCenterPage
        canList={access.hasPerms('product:center:list')}
        canQuery={access.hasPerms('product:center:query')}
        fetchList={getProductCenterList}
        fetchProduct={getProductCenterProduct}
        storageKey="admin-product-center"
      />
    </PageContainer>
  );
}
