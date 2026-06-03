import React, { useEffect, useState } from 'react';
import {
  clearCacheAll,
  clearCacheKey,
  clearCacheName,
  getCacheValue,
  listCacheKey,
  listCacheName,
} from '@/services/monitor/cachelist';
import { Button, Card, Col, Form, Input, Popconfirm, Row, Table } from 'antd';
import styles from './index.module.css';
import { ReloadOutlined } from '@ant-design/icons';
import { ProForm } from '@ant-design/pro-components';
import { message } from '@/utils/feedback';

const { TextArea } = Input;

type CacheKeyRow = {
  index: number;
  cacheKey: string;
};

const CacheList: React.FC = () => {
  const [cacheNames, setCacheNames] = useState<API.Monitor.CacheContent[]>([]);
  const [currentCacheName, setCurrentCacheName] = useState('');
  const [cacheKeys, setCacheKeys] = useState<CacheKeyRow[]>([]);
  const [form] = Form.useForm();

  const getCacheNames = () => {
    listCacheName().then((res) => {
      if (res.code === 200) {
        setCacheNames(res.data || []);
      }
    });
  };

  useEffect(() => {
    getCacheNames();
  }, []);

  const getCacheKeys = (cacheName: string) => {
    if (!cacheName) {
      setCacheKeys([]);
      return;
    }

    listCacheKey(cacheName).then((res) => {
      if (res.code === 200) {
        const keysData = (res.data || []).map((item, index) => ({
          index: index + 1,
          cacheKey: item,
        }));
        setCacheKeys(keysData);
      }
    });
  };

  const onClearAll = async () => {
    clearCacheAll().then((res) => {
      if (res.code === 200) {
        message.success('清理全部缓存成功');
        setCacheKeys([]);
        setCurrentCacheName('');
        form.resetFields();
        getCacheNames();
      }
    });
  };

  const refreshCacheNames = () => {
    getCacheNames();
    message.success('刷新缓存列表成功');
  };

  const refreshCacheKeys = () => {
    if (!currentCacheName) {
      message.warning('请先选择缓存名称');
      return;
    }
    getCacheKeys(currentCacheName);
    message.success('刷新键名列表成功');
  };

  const columns = [
    {
      title: '缓存名称',
      dataIndex: 'cacheName',
      key: 'cacheName',
      render: (_: string, record: API.Monitor.CacheContent) => record.cacheName.replace(':', ''),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
    },
    {
      title: '操作',
      dataIndex: 'option',
      width: 90,
      render: (_: unknown, record: API.Monitor.CacheContent) => (
        <Button
          type="link"
          size="small"
          danger
          onClick={(event) => {
            event.stopPropagation();
            clearCacheName(record.cacheName).then((res) => {
              if (res.code === 200) {
                message.success(`清理缓存名称[${record.cacheName}]成功`);
                if (currentCacheName === record.cacheName) {
                  setCacheKeys([]);
                  form.resetFields();
                }
              }
            });
          }}
        >
          删除
        </Button>
      ),
    },
  ];

  const cacheKeysColumns = [
    {
      title: '序号',
      dataIndex: 'index',
      key: 'index',
      width: 80,
    },
    {
      title: '缓存键名',
      dataIndex: 'cacheKey',
      key: 'cacheKey',
      render: (_: string, record: CacheKeyRow) => record.cacheKey.replace(currentCacheName, ''),
    },
    {
      title: '操作',
      dataIndex: 'option',
      width: 90,
      render: (_: unknown, record: CacheKeyRow) => (
        <Button
          type="link"
          size="small"
          danger
          onClick={(event) => {
            event.stopPropagation();
            clearCacheKey(record.cacheKey).then((res) => {
              if (res.code === 200) {
                message.success(`清理缓存键名[${record.cacheKey}]成功`);
                getCacheKeys(currentCacheName);
                form.resetFields();
              }
            });
          }}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Row gutter={[24, 24]}>
        <Col span={8}>
          <Card
            title="缓存列表"
            extra={<Button icon={<ReloadOutlined />} onClick={refreshCacheNames} type="link" />}
            className={styles.card}
          >
            <Table
              rowKey="cacheName"
              dataSource={cacheNames}
              columns={columns}
              onRow={(record) => ({
                onClick: () => {
                  setCurrentCacheName(record.cacheName);
                  form.resetFields();
                  getCacheKeys(record.cacheName);
                },
              })}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title="键名列表"
            extra={<Button icon={<ReloadOutlined />} onClick={refreshCacheKeys} type="link" />}
            className={styles.card}
          >
            <Table
              rowKey="cacheKey"
              dataSource={cacheKeys}
              columns={cacheKeysColumns}
              onRow={(record) => ({
                onClick: () => {
                  if (!currentCacheName) return;
                  getCacheValue(currentCacheName, record.cacheKey).then((res) => {
                    if (res.code === 200) {
                      form.resetFields();
                      form.setFieldsValue({
                        cacheName: res.data.cacheName,
                        cacheKey: res.data.cacheKey,
                        cacheValue: res.data.cacheValue,
                        remark: res.data.remark,
                      });
                    }
                  });
                },
              })}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title="缓存内容"
            extra={
              <Popconfirm
                title="确认清理全部缓存？"
                description="这会删除 Redis 中所有缓存，包括当前登录 token。"
                onConfirm={onClearAll}
              >
                <Button icon={<ReloadOutlined />} type="link">
                  清理全部
                </Button>
              </Popconfirm>
            }
            className={styles.card}
          >
            <ProForm
              name="cacheContent"
              form={form}
              labelCol={{ span: 8 }}
              wrapperCol={{ span: 16 }}
              submitter={false}
              autoComplete="off"
            >
              <Form.Item label="缓存名称" name="cacheName">
                <Input />
              </Form.Item>
              <Form.Item label="缓存键名" name="cacheKey">
                <Input />
              </Form.Item>
              <Form.Item label="缓存内容" name="cacheValue">
                <TextArea autoSize={{ minRows: 2 }} />
              </Form.Item>
            </ProForm>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CacheList;
