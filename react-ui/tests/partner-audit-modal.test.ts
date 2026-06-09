import { buildAuditParams } from '@/components/PartnerManagement/PartnerAuditModal';
import { readFileSync } from 'fs';
import { join } from 'path';

jest.mock('@ant-design/pro-components', () => ({
  ProTable: jest.fn(() => null),
}));

jest.mock('@umijs/max', () => ({
  useAccess: jest.fn(() => ({
    hasPerms: () => true,
  })),
}));

describe('partner audit modal params', () => {
  it('adds subjectId and accountId for account scoped login or oper audit', () => {
    expect(buildAuditParams(
      { status: '0', timeRange: ['2026-06-01', '2026-06-02'] },
      1,
      10,
      101,
      10001,
      'subjectId',
      'accountId',
    )).toEqual({
      status: '0',
      pageNum: 1,
      pageSize: 10,
      subjectId: 101,
      accountId: 10001,
      'params[beginTime]': '2026-06-01',
      'params[endTime]': '2026-06-02',
    });
  });

  it('adds targetSubjectId and targetAccountId for account scoped direct-login tickets', () => {
    expect(buildAuditParams(
      { status: 'USED' },
      2,
      20,
      202,
      20002,
      'targetSubjectId',
      'targetAccountId',
    )).toEqual({
      status: 'USED',
      pageNum: 2,
      pageSize: 20,
      targetSubjectId: 202,
      targetAccountId: 20002,
    });
  });

  it('does not send a bare accountId without subject scope', () => {
    expect(buildAuditParams(
      { status: '0' },
      1,
      10,
      undefined,
      10001,
      'subjectId',
      'accountId',
    )).toEqual({
      status: '0',
      pageNum: 1,
      pageSize: 10,
    });
  });
});

describe('partner audit modal admin control fields', () => {
  const source = readFileSync(join(process.cwd(), 'src/components/PartnerManagement/PartnerAuditModal.tsx'), 'utf8');

  it('keeps acting admin visible in login and oper audit lists', () => {
    const actingAdminColumns = source.match(/dataIndex: 'actingAdminName'/g) || [];

    expect(actingAdminColumns.length).toBeGreaterThanOrEqual(2);
    expect(source).toContain("title: '后台操作人'");
  });

  it('keeps direct-login audit details visible for login and oper audit rows', () => {
    expect(source).toContain('record.actingAdminId');
    expect(source).toContain('record.directLoginTicketId');
    expect(source).toContain('record.directLoginReason');
  });
});
