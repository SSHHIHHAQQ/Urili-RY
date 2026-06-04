package com.ruoyi.integration.support;

/**
 * 上游系统常量。
 */
public final class UpstreamSystemConstants
{
    public static final String SYSTEM_KIND_LINGXING_WMS = "LINGXING_WMS";

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

    public static final String DEFAULT_CAPABILITIES =
        "[\"WAREHOUSE_SYNC\",\"LOGISTICS_CHANNEL_SYNC\",\"SKU_SYNC\"]";

    private UpstreamSystemConstants()
    {
    }
}
