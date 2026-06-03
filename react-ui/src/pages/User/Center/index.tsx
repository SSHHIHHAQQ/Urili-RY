import {
  ClusterOutlined,
  MailOutlined,
  TeamOutlined,
  UserOutlined,
  MobileOutlined,
  ManOutlined,
} from '@ant-design/icons';
import { Card, Col, Divider, Row } from 'antd';
import React, { useEffect, useState } from 'react';
import styles from './Center.module.css';
import BaseInfo from './components/BaseInfo';
import ResetPassword from './components/ResetPassword';
import AvatarCropper from './components/AvatarCropper';
import { getUserInfo } from '@/services/session';
import { PageLoading } from '@ant-design/pro-components';

const operationTabList = [
  {
    key: 'base',
    tab: <span>基本资料</span>,
  },
  {
    key: 'password',
    tab: <span>重置密码</span>,
  },
];

export type tabKeyType = 'base' | 'password';

const DEFAULT_AVATAR = 'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png';

const sexMap: Record<string, string> = {
  '0': '男',
  '1': '女',
  '2': '未知',
};

function InfoItem({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value?: React.ReactNode;
}) {
  return (
    <div className={styles.infoItem}>
      <div className={styles.infoLabel}>
        {icon}
        {label}
      </div>
      <div className={styles.infoValue}>{value || '-'}</div>
    </div>
  );
}

const Center: React.FC = () => {
  const [tabKey, setTabKey] = useState<tabKeyType>('base');
  const [cropperModalOpen, setCropperModalOpen] = useState<boolean>(false);
  const [userInfo, setUserInfo] = useState<any>();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getUserInfo()
      .then((data) => {
        if (!data?.user) {
          setUserInfo(undefined);
          return;
        }
        setUserInfo({
          ...data,
          user: {
            ...data.user,
            avatar: data.user.avatar || DEFAULT_AVATAR,
          },
        });
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  if (loading) {
    return <PageLoading />;
  }

  const currentUser = userInfo?.user;

  const renderUserInfo = ({
    userName,
    phonenumber,
    email,
    sex,
    dept,
  }: Partial<API.CurrentUser>) => (
    <div className={styles.infoList}>
      <InfoItem icon={<UserOutlined />} label="用户名" value={userName} />
      <InfoItem icon={<ManOutlined />} label="性别" value={sexMap[`${sex}`] || '-'} />
      <InfoItem icon={<MobileOutlined />} label="电话" value={phonenumber} />
      <InfoItem icon={<MailOutlined />} label="邮箱" value={email} />
      <InfoItem icon={<ClusterOutlined />} label="部门" value={dept?.deptName} />
    </div>
  );

  const renderChildrenByTabKey = (tabValue: tabKeyType) => {
    if (tabValue === 'base') {
      return <BaseInfo values={currentUser} />;
    }
    if (tabValue === 'password') {
      return <ResetPassword />;
    }
    return null;
  };

  if (!currentUser) {
    return <PageLoading />;
  }

  return (
    <div>
      <Row gutter={[16, 24]}>
        <Col lg={8} md={24}>
          <Card title="个人信息" variant="borderless" loading={loading}>
            {!loading && (
              <div style={{ textAlign: 'center' }}>
                <div className={styles.avatarHolder} onClick={() => setCropperModalOpen(true)}>
                  <img alt="" src={currentUser.avatar} />
                </div>
                {renderUserInfo(currentUser)}
                <Divider dashed />
                <div className={styles.team}>
                  <div className={styles.teamTitle}>角色</div>
                  <Row gutter={36}>
                    {currentUser.roles &&
                      currentUser.roles.map((item: any) => (
                        <Col key={item.roleId} lg={24} xl={12}>
                          <TeamOutlined
                            style={{
                              marginRight: 8,
                            }}
                          />
                          {item.roleName}
                        </Col>
                      ))}
                  </Row>
                </div>
              </div>
            )}
          </Card>
        </Col>
        <Col lg={16} md={24}>
          <Card
            variant="borderless"
            tabList={operationTabList}
            activeTabKey={tabKey}
            onTabChange={(_tabKey: string) => {
              setTabKey(_tabKey as tabKeyType);
            }}
          >
            {renderChildrenByTabKey(tabKey)}
          </Card>
        </Col>
      </Row>
      <AvatarCropper
        onFinished={() => {
          setCropperModalOpen(false);
        }}
        open={cropperModalOpen}
        data={currentUser.avatar}
      />
    </div>
  );
};

export default Center;
