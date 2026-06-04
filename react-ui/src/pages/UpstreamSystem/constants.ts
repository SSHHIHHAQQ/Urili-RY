export const connectionStatusText: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
};

export const syncItemStatusText: Record<string, string> = {
  ACTIVE: '正常',
  MISSING: '上游缺失',
};

export const pairingStatusText: Record<string, string> = {
  PAIRED: '已配对',
  UNASSIGNED: '未配对',
};

export const settlementOptions = [
  { label: '上游应付', value: 'UPSTREAM_PAYABLE' },
  { label: '平台垫付', value: 'PLATFORM_ADVANCE' },
];
