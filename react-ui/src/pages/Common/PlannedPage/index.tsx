import { PageContainer } from '@ant-design/pro-components';
import { Card, Typography } from 'antd';

const { Text } = Typography;

export default function PlannedPage() {
  return (
    <PageContainer>
      <Card>
        <Text type="secondary">功能规划中</Text>
      </Card>
    </PageContainer>
  );
}
