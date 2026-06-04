import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Tabs } from 'antd';
import AttributeLibrary from './components/AttributeLibrary';
import CategoryAttributeTemplate from './components/CategoryAttributeTemplate';

export default function ProductAttributePage() {
  const access = useAccess();

  return (
    <PageContainer title={false}>
      <Tabs
        items={[
          {
            key: 'attribute',
            label: '属性库',
            children: <AttributeLibrary access={access} />,
          },
          {
            key: 'categoryAttribute',
            label: '类目属性模板',
            children: <CategoryAttributeTemplate access={access} />,
          },
        ]}
      />
    </PageContainer>
  );
}
