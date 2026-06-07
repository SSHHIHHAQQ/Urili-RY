import { jsx, jsxs } from "react/jsx-runtime";
import { PageContainer } from "@ant-design/pro-components";
import { useAccess } from "@umijs/max";
import { Checkbox, Empty, Form, Modal, Select, Space, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  addLogisticsChannelPairing,
  addSkuPairing,
  addUpstreamConnection,
  addWarehousePairing,
  authorizeUpstreamConnection,
  getUpstreamConnectionList,
  syncUpstreamConnection,
  updateUpstreamConnection,
  updateUpstreamConnectionOrder,
  updateUpstreamCredentials,
  updateUpstreamStatus
} from "@/services/integration/upstreamSystem";
import { getOfficialWarehouseList } from "@/services/warehouse/warehouse";
import { SEARCHABLE_SELECT_PROPS } from "@/utils/selectSearch";
import { message } from "@/utils/feedback";
import ConnectionModal from "./components/ConnectionModal";
import ConnectionSidebar from "./components/ConnectionSidebar";
import ConnectionSummary from "./components/ConnectionSummary";
import PairingModal from "./components/PairingModal";
import SyncTabs from "./components/SyncTabs";
import { pairingRoleText, syncTypeText } from "./constants";
import { resultOk } from "./helpers";
import "./style.css";
import styles from "./style.module.css";
const connectionPageSize = 200;
const warehouseOptionPageSize = 1e3;
const defaultManualSyncTypes = ["WAREHOUSE", "LOGISTICS_CHANNEL", "SKU"];
const syncTypeOptions = [
  {
    label: "\u4ED3\u5E93",
    value: "WAREHOUSE",
    permission: "integration:upstream:sync"
  },
  {
    label: "\u7269\u6D41\u6E20\u9053",
    value: "LOGISTICS_CHANNEL",
    permission: "integration:upstream:sync"
  },
  {
    label: "SKU\u4FE1\u606F",
    value: "SKU",
    permission: "integration:upstream:sync"
  },
  {
    label: "SKU\u4ED3\u5E93\u5C3A\u5BF8\u91CD\u91CF",
    value: "SKU_DIMENSION",
    permission: "integration:upstream:dimensionSync"
  },
  {
    label: "SKU\u5E93\u5B58",
    value: "INVENTORY",
    permission: "integration:upstream:inventorySync"
  }
];
const formatSyncItemResult = (item) => {
  const label = syncTypeText[item.syncType || ""] || item.syncType || "\u540C\u6B65\u9879";
  if (item.status === "FAILED") {
    return `${label}\u5931\u8D25\uFF1A${item.errorMessage || "-"}`;
  }
  const changedTotal = (item.insertedCount || 0) + (item.changedCount || 0);
  return `${label}\uFF1A\u62C9\u53D6${item.pulledCount || item.count || 0}\uFF0C\u65B0\u589E${item.insertedCount || 0}\uFF0C\u53D8\u66F4${item.changedCount || 0}\uFF0C\u505C\u7528${item.disabledCount || 0}\uFF0C\u672A\u53D8${item.unchangedCount || 0}\uFF0C\u5199\u5165${changedTotal}`;
};
const formatSyncResult = (data) => {
  if (data?.items?.length) {
    return data.items.map(formatSyncItemResult).join("\uFF1B");
  }
  return `\u4ED3\u5E93 ${data?.warehouseCount || 0}\uFF0C\u6E20\u9053 ${data?.logisticsChannelCount || 0}\uFF0CSKU ${data?.skuCount || 0}\uFF0C\u5C3A\u5BF8\u91CD\u91CF ${data?.skuDimensionCount || 0}\uFF0C\u5E93\u5B58 ${data?.warehouseStockCount || 0}`;
};
function UpstreamSystemPage() {
  const access = useAccess();
  const warehouseActionRef = useRef(null);
  const logisticsActionRef = useRef(null);
  const skuActionRef = useRef(null);
  const dimensionActionRef = useRef(null);
  const inventoryActionRef = useRef(null);
  const logActionRef = useRef(null);
  const [connections, setConnections] = useState([]);
  const [loadingConnections, setLoadingConnections] = useState(false);
  const [selectedConnection, setSelectedConnection] = useState();
  const [connectionModal, setConnectionModal] = useState({ open: false, mode: "create" });
  const [pairingModal, setPairingModal] = useState({
    open: false
  });
  const [syncModal, setSyncModal] = useState({
    open: false,
    syncTypes: defaultManualSyncTypes,
    submitting: false
  });
  const [warehouseOptions, setWarehouseOptions] = useState([]);
  const [warehouseOptionsLoading, setWarehouseOptionsLoading] = useState(false);
  const fetchConnections = useCallback(async (preferredCode) => {
    setLoadingConnections(true);
    try {
      const resp = await getUpstreamConnectionList({
        pageNum: 1,
        pageSize: connectionPageSize
      });
      if (resp.code !== 200) {
        message.error(resp.msg || "\u4E3B\u4ED3\u63A5\u5165\u52A0\u8F7D\u5931\u8D25");
        return [];
      }
      const rows = resp.rows || [];
      setConnections(rows);
      setSelectedConnection((current) => {
        const nextCode = preferredCode || current?.connectionCode;
        return rows.find((item) => item.connectionCode === nextCode) || rows[0];
      });
      return rows;
    } finally {
      setLoadingConnections(false);
    }
  }, []);
  const loadWarehouseOptions = useCallback(
    async (connectionCode, pairingRole) => {
      if (!connectionCode) {
        setWarehouseOptions([]);
        return;
      }
      setWarehouseOptionsLoading(true);
      try {
        const warehouseResp = await getOfficialWarehouseList({
          pageNum: 1,
          pageSize: warehouseOptionPageSize,
          status: "0"
        });
        if (warehouseResp.code !== 200) {
          message.error(warehouseResp.msg || "\u7CFB\u7EDF\u4ED3\u5E93\u52A0\u8F7D\u5931\u8D25");
          setWarehouseOptions([]);
          return;
        }
        const options = (warehouseResp.rows || []).filter((item) => item.warehouseCode).filter(
          (item) => pairingRole === "QUOTE" ? !item.quoteWarehousePairingId : !item.warehousePairingId
        ).map((item) => {
          const warehouseCode = item.warehouseCode || "";
          const warehouseName = item.warehouseName || "";
          return {
            label: `${warehouseCode} / ${warehouseName}`,
            value: warehouseCode,
            warehouseCode,
            warehouseName,
            name: warehouseName,
            code: warehouseCode,
            searchText: `${warehouseCode} ${warehouseName} ${item.countryCode || ""}`
          };
        });
        setWarehouseOptions(options);
      } finally {
        setWarehouseOptionsLoading(false);
      }
    },
    []
  );
  useEffect(() => {
    fetchConnections();
  }, [fetchConnections]);
  const selectedCode = selectedConnection?.connectionCode || "";
  const selectedPairingRole = selectedConnection?.settlementType === "self-operated-receivable" ? "QUOTE" : "FULFILLMENT";
  const selectedPairingRoleLabel = pairingRoleText[selectedPairingRole] || selectedPairingRole;
  useEffect(() => {
    if (pairingModal.open && pairingModal.type === "warehouse") {
      loadWarehouseOptions(selectedCode, selectedPairingRole);
    }
  }, [loadWarehouseOptions, pairingModal, selectedCode, selectedPairingRole]);
  const reloadTabs = () => {
    warehouseActionRef.current?.reload();
    logisticsActionRef.current?.reload();
    skuActionRef.current?.reload();
    dimensionActionRef.current?.reload();
    inventoryActionRef.current?.reload();
    logActionRef.current?.reload();
  };
  const reloadCurrent = async (preferredCode) => {
    const targetCode = preferredCode || selectedCode;
    await fetchConnections(targetCode);
    if (!targetCode || targetCode === selectedCode) {
      reloadTabs();
    }
  };
  const handleAuthorize = async (record) => {
    const hide = message.loading("\u6B63\u5728\u6821\u9A8C\u6388\u6743");
    const ok = resultOk(
      await authorizeUpstreamConnection(record.connectionCode),
      "\u6388\u6743\u6821\u9A8C\u901A\u8FC7"
    );
    hide();
    if (ok) {
      await reloadCurrent(record.connectionCode);
    }
  };
  const handleSync = (record) => {
    const allowedDefaults = defaultManualSyncTypes.filter((type) => {
      const option = syncTypeOptions.find((item) => item.value === type);
      return option ? access.hasPerms(option.permission) : false;
    });
    setSyncModal({
      open: true,
      record,
      syncTypes: allowedDefaults,
      submitting: false
    });
  };
  const submitSync = async () => {
    const record = syncModal.record;
    if (!record) return;
    if (syncModal.syncTypes.length === 0) {
      message.warning("\u8BF7\u9009\u62E9\u540C\u6B65\u5185\u5BB9");
      return;
    }
    setSyncModal((current) => ({ ...current, submitting: true }));
    const resp = await syncUpstreamConnection(record.connectionCode, {
      syncTypes: syncModal.syncTypes
    });
    setSyncModal((current) => ({ ...current, submitting: false }));
    if (resp.code === 200) {
      const hasFailed = resp.data?.items?.some((item) => item.status === "FAILED");
      const feedback = `\u540C\u6B65\u5B8C\u6210\uFF1A${formatSyncResult(resp.data)}`;
      if (hasFailed) {
        message.warning(feedback);
      } else {
        message.success(feedback);
      }
      setSyncModal({
        open: false,
        syncTypes: defaultManualSyncTypes,
        submitting: false
      });
      await reloadCurrent(record.connectionCode);
    } else {
      message.error(resp.msg);
    }
  };
  const handleToggleStatus = async (record) => {
    const nextStatus = record.status === "ENABLED" ? "DISABLED" : "ENABLED";
    const ok = resultOk(
      await updateUpstreamStatus(record.connectionCode, nextStatus),
      "\u72B6\u6001\u5DF2\u66F4\u65B0"
    );
    if (ok) {
      await reloadCurrent(record.connectionCode);
    }
  };
  const saveOrder = async (connectionCodes) => {
    const ok = resultOk(
      await updateUpstreamConnectionOrder(connectionCodes),
      "\u6392\u5E8F\u5DF2\u4FDD\u5B58"
    );
    if (ok) {
      await fetchConnections(selectedCode);
    }
    return ok;
  };
  const logisticsWarehouseOptions = pairingModal.open && pairingModal.type === "logistics" ? pairingModal.row.warehouseItems.map((item) => ({
    label: item.systemWarehouseCode ? `${item.warehouseCode} -> ${item.systemWarehouseCode} / ${item.systemWarehouseName || "-"}` : `${item.warehouseCode}\uFF08\u8BF7\u5148\u914D\u5BF9${selectedPairingRoleLabel}\u4ED3\uFF09`,
    value: item.warehouseCode,
    disabled: !item.systemWarehouseCode,
    searchText: `${item.warehouseCode} ${item.systemWarehouseCode || ""} ${item.systemWarehouseName || ""}`
  })) : [];
  return /* @__PURE__ */ jsxs(PageContainer, { children: [
    /* @__PURE__ */ jsxs("div", { className: styles.workspace, children: [
      /* @__PURE__ */ jsx(
        ConnectionSidebar,
        {
          access,
          connections,
          loading: loadingConnections,
          onCreate: () => setConnectionModal({ open: true, mode: "create" }),
          onSaveOrder: saveOrder,
          onSelect: setSelectedConnection,
          selectedCode
        }
      ),
      selectedConnection ? /* @__PURE__ */ jsxs("div", { className: styles.detailPane, children: [
        /* @__PURE__ */ jsx(
          ConnectionSummary,
          {
            access,
            connection: selectedConnection,
            onAuthorize: () => handleAuthorize(selectedConnection),
            onCredential: () => setConnectionModal({
              open: true,
              mode: "credential",
              record: selectedConnection
            }),
            onEdit: () => setConnectionModal({
              open: true,
              mode: "edit",
              record: selectedConnection
            }),
            onSync: () => handleSync(selectedConnection),
            onToggleStatus: () => handleToggleStatus(selectedConnection)
          }
        ),
        /* @__PURE__ */ jsx(
          SyncTabs,
          {
            access,
            dimensionActionRef,
            inventoryActionRef,
            logActionRef,
            logisticsActionRef,
            onSkuSynced: () => fetchConnections(selectedCode),
            selectedConnection,
            setPairingModal,
            skuActionRef,
            warehouseActionRef
          },
          selectedCode
        )
      ] }) : /* @__PURE__ */ jsx("div", { className: styles.emptyPane, children: /* @__PURE__ */ jsx(Empty, { description: "\u6682\u65E0\u4E3B\u4ED3\u63A5\u5165" }) })
    ] }),
    /* @__PURE__ */ jsx(
      ConnectionModal,
      {
        mode: connectionModal.mode,
        open: connectionModal.open,
        record: connectionModal.record,
        onCancel: () => setConnectionModal({ open: false, mode: "create" }),
        onSubmit: async (values) => {
          const modalRecord = connectionModal.record;
          if (connectionModal.mode !== "create" && !modalRecord?.connectionCode) {
            return false;
          }
          const hide = message.loading("\u6B63\u5728\u4FDD\u5B58");
          try {
            let resp;
            if (connectionModal.mode === "create") {
              resp = await addUpstreamConnection(values);
            } else if (connectionModal.mode === "edit") {
              if (!modalRecord) return false;
              resp = await updateUpstreamConnection(modalRecord.connectionCode, {
                masterWarehouseName: values.masterWarehouseName,
                settlementType: values.settlementType,
                remark: values.remark
              });
            } else {
              if (!modalRecord) return false;
              resp = await updateUpstreamCredentials(
                modalRecord.connectionCode,
                values
              );
            }
            const ok = resultOk(resp, "\u4FDD\u5B58\u6210\u529F");
            if (ok) {
              setConnectionModal({ open: false, mode: "create" });
              await fetchConnections(
                modalRecord?.connectionCode || values.connectionCode
              );
            }
            return ok;
          } finally {
            hide();
          }
        }
      }
    ),
    /* @__PURE__ */ jsx(
      Modal,
      {
        title: "\u9009\u62E9\u540C\u6B65\u5185\u5BB9",
        open: syncModal.open,
        confirmLoading: syncModal.submitting,
        okText: "\u5F00\u59CB\u540C\u6B65",
        onCancel: () => setSyncModal({
          open: false,
          syncTypes: defaultManualSyncTypes,
          submitting: false
        }),
        onOk: submitSync,
        children: /* @__PURE__ */ jsxs(Space, { direction: "vertical", size: 12, style: { width: "100%" }, children: [
          /* @__PURE__ */ jsx(Typography.Text, { type: "secondary", children: "\u5C3A\u5BF8\u91CD\u91CF\u4E3A\u9650\u901F\u540C\u6B65\uFF0C\u5E93\u5B58\u4E3A\u4E0A\u6E38\u5E93\u5B58\u5FEB\u7167\u3002" }),
          /* @__PURE__ */ jsx(
            Checkbox.Group,
            {
              value: syncModal.syncTypes,
              onChange: (values) => setSyncModal((current) => ({
                ...current,
                syncTypes: values.map(String)
              })),
              children: /* @__PURE__ */ jsx(Space, { direction: "vertical", children: syncTypeOptions.filter((option) => access.hasPerms(option.permission)).map((option) => /* @__PURE__ */ jsx(Checkbox, { value: option.value, children: option.label }, option.value)) })
            }
          )
        ] })
      }
    ),
    /* @__PURE__ */ jsx(
      PairingModal,
      {
        open: pairingModal.open,
        title: pairingModal.open && pairingModal.type === "warehouse" ? "\u4ED3\u5E93\u914D\u5BF9" : pairingModal.open && pairingModal.type === "logistics" ? "\u7269\u6D41\u6E20\u9053\u914D\u5BF9" : "SKU\u914D\u5BF9",
        upstreamLabel: pairingModal.open && pairingModal.type === "warehouse" ? "\u9886\u661F\u4ED3\u5E93" : pairingModal.open && pairingModal.type === "logistics" ? "\u9886\u661F\u6E20\u9053" : "\u9886\u661FmasterSku",
        upstreamValue: pairingModal.open && pairingModal.type === "warehouse" ? `${pairingModal.row.warehouseCode} / ${pairingModal.row.warehouseName}` : pairingModal.open && pairingModal.type === "logistics" ? `${pairingModal.row.channelCode} / ${pairingModal.row.channelName}` : pairingModal.open ? `${pairingModal.row.masterSku} / ${pairingModal.row.masterProductName}` : "",
        codeLabel: pairingModal.open && pairingModal.type === "sku" ? "\u7CFB\u7EDFSKU" : pairingModal.open && pairingModal.type === "warehouse" ? "\u7CFB\u7EDF\u4ED3\u5E93\u4EE3\u7801" : "\u7CFB\u7EDF\u6E20\u9053\u4EE3\u7801",
        nameLabel: pairingModal.open && pairingModal.type === "sku" ? "\u7CFB\u7EDFSKU\u540D\u79F0" : pairingModal.open && pairingModal.type === "warehouse" ? "\u7CFB\u7EDF\u4ED3\u5E93\u540D\u79F0" : "\u7CFB\u7EDF\u6E20\u9053\u540D\u79F0",
        codeName: pairingModal.open && pairingModal.type === "sku" ? "systemSku" : pairingModal.open && pairingModal.type === "warehouse" ? "systemWarehouseCode" : "systemChannelCode",
        nameName: pairingModal.open && pairingModal.type === "sku" ? "systemSkuName" : pairingModal.open && pairingModal.type === "warehouse" ? "systemWarehouseName" : "systemChannelName",
        showCustomerName: pairingModal.open && pairingModal.type === "sku",
        extraItems: pairingModal.open && pairingModal.type === "logistics" ? /* @__PURE__ */ jsx(
          Form.Item,
          {
            name: "upstreamWarehouseCode",
            label: "\u9886\u661F\u4ED3\u5E93",
            rules: [{ required: true, message: "\u8BF7\u9009\u62E9\u9886\u661F\u4ED3\u5E93" }],
            children: /* @__PURE__ */ jsx(
              Select,
              {
                ...SEARCHABLE_SELECT_PROPS,
                options: logisticsWarehouseOptions
              }
            )
          }
        ) : null,
        customPairingItems: pairingModal.open && pairingModal.type === "warehouse" ? /* @__PURE__ */ jsx(
          Form.Item,
          {
            name: "systemWarehouseCode",
            label: "\u7CFB\u7EDF\u4ED3\u5E93",
            rules: [{ required: true, message: "\u8BF7\u9009\u62E9\u7CFB\u7EDF\u4ED3\u5E93" }],
            children: /* @__PURE__ */ jsx(
              Select,
              {
                ...SEARCHABLE_SELECT_PROPS,
                loading: warehouseOptionsLoading,
                options: warehouseOptions,
                placeholder: `\u8BF7\u9009\u62E9\u8981\u7ED1\u5B9A\u4E3A${selectedPairingRoleLabel}\u4ED3\u7684\u7CFB\u7EDF\u4ED3\u5E93`
              }
            )
          }
        ) : null,
        onCancel: () => setPairingModal({ open: false }),
        onSubmit: async (values) => {
          if (!pairingModal.open) return false;
          const hide = message.loading("\u6B63\u5728\u914D\u5BF9");
          let resp;
          if (pairingModal.type === "warehouse") {
            const systemWarehouse = warehouseOptions.find(
              (item) => item.value === values.systemWarehouseCode
            );
            if (!systemWarehouse) {
              hide();
              message.error("\u8BF7\u9009\u62E9\u7CFB\u7EDF\u4ED3\u5E93");
              return false;
            }
            resp = await addWarehousePairing(selectedCode, {
              remark: values.remark,
              pairingRole: selectedPairingRole,
              upstreamWarehouseCode: pairingModal.row.warehouseCode,
              systemWarehouseCode: systemWarehouse.warehouseCode,
              systemWarehouseName: systemWarehouse.warehouseName
            });
          } else if (pairingModal.type === "logistics") {
            const warehouseContext = pairingModal.row.warehouseItems.find(
              (item) => item.warehouseCode === values.upstreamWarehouseCode
            );
            if (!warehouseContext?.systemWarehouseCode) {
              hide();
              message.error(`\u8BF7\u5148\u914D\u5BF9${selectedPairingRoleLabel}\u4ED3`);
              return false;
            }
            resp = await addLogisticsChannelPairing(selectedCode, {
              ...values,
              pairingRole: selectedPairingRole,
              systemWarehouseCode: warehouseContext.systemWarehouseCode,
              upstreamChannelCode: pairingModal.row.channelCode
            });
          } else {
            resp = await addSkuPairing(selectedCode, {
              ...values,
              masterSku: pairingModal.row.masterSku
            });
          }
          hide();
          const ok = resultOk(resp, "\u914D\u5BF9\u6210\u529F");
          if (ok) {
            setPairingModal({ open: false });
            warehouseActionRef.current?.reload();
            logisticsActionRef.current?.reload();
            skuActionRef.current?.reload();
          }
          return ok;
        }
      }
    )
  ] });
}
export {
  UpstreamSystemPage as default
};
