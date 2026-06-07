package com.ruoyi.integration.support;

/**
 * 上游系统常量。
 */
public final class UpstreamSystemConstants
{
    public static final String SYSTEM_KIND_LINGXING_WMS = "lingxing-wms";

    public static final String SYSTEM_KIND_LINGXING_WMS_LEGACY = "LINGXING_WMS";

    public static final String SETTLEMENT_TYPE_UPSTREAM_PAYABLE = "upstream-payable";

    public static final String SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE = "self-operated-receivable";

    public static final String PAIRING_ROLE_FULFILLMENT = "FULFILLMENT";

    public static final String PAIRING_ROLE_QUOTE = "QUOTE";

    public static final String STATUS_ENABLED = "ENABLED";

    public static final String STATUS_DISABLED = "DISABLED";

    public static final String STATUS_ACTIVE = "ACTIVE";

    public static final String STATUS_MISSING = "MISSING";

    public static final String CREDENTIAL_STATUS_CONFIGURED = "CONFIGURED";

    public static final String CREDENTIAL_STATUS_INVALID = "INVALID";

    public static final String SYNC_STATUS_NEVER = "NEVER";

    public static final String SYNC_STATUS_SYNCING = "SYNCING";

    public static final String SYNC_STATUS_FRESH = "FRESH";

    public static final String SYNC_STATUS_FAILED = "FAILED";

    public static final String SYNC_STATUS_SKIPPED = "SKIPPED";

    public static final String SYNC_TYPE_WAREHOUSE = "WAREHOUSE";

    public static final String SYNC_TYPE_LOGISTICS_CHANNEL = "LOGISTICS_CHANNEL";

    public static final String SYNC_TYPE_SKU = "SKU";

    public static final String SYNC_TYPE_SKU_DIMENSION = "SKU_DIMENSION";

    public static final String SYNC_TYPE_INVENTORY = "INVENTORY";

    public static final String SYNC_MODE_SCHEDULED = "SCHEDULED";

    public static final String SYNC_MODE_MANUAL = "MANUAL";

    public static final String SYNC_MODE_SELECTED = "SELECTED";

    public static final String OP_AUTH_CHECK = "AUTH_CHECK";

    public static final String OP_WAREHOUSE_SYNC = "WAREHOUSE_SYNC";

    public static final String OP_LOGISTICS_CHANNEL_SYNC = "LOGISTICS_CHANNEL_SYNC";

    public static final String OP_SKU_SYNC = "SKU_SYNC";

    public static final String OP_SKU_DIMENSION_SYNC = "SKU_DIMENSION_SYNC";

    public static final String OP_SKU_DIMENSION_FULL_SYNC = "SKU_DIMENSION_FULL_SYNC";

    public static final String OP_SKU_DIMENSION_SELECTED_SYNC = "SKU_DIMENSION_SELECTED_SYNC";

    public static final String OP_INVENTORY_SYNC = "INVENTORY_SYNC";

    public static final String OP_TASK_WAREHOUSE_SYNC = "TASK_WAREHOUSE_SYNC";

    public static final String OP_TASK_LOGISTICS_CHANNEL_SYNC = "TASK_LOGISTICS_CHANNEL_SYNC";

    public static final String OP_TASK_SKU_SYNC = "TASK_SKU_SYNC";

    public static final String OP_TASK_SKU_DIMENSION_SYNC = "TASK_SKU_DIMENSION_SYNC";

    public static final String OP_TASK_INVENTORY_SYNC = "TASK_INVENTORY_SYNC";

    public static final String DEFAULT_CAPABILITIES =
        "[\"WAREHOUSE_SYNC\",\"LOGISTICS_CHANNEL_SYNC\",\"SKU_SYNC\",\"SKU_DIMENSION_SYNC\",\"INVENTORY_SYNC\"]";

    private UpstreamSystemConstants()
    {
    }
}
