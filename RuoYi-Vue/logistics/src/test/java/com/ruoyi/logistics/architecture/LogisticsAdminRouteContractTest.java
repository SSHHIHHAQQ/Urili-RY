package com.ruoyi.logistics.architecture;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class LogisticsAdminRouteContractTest
{
    @Test
    public void adminControllerMustGuardAllLogisticsCarrierActions() throws IOException
    {
        String controller = readBackend(
            "ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/AdminLogisticsCarrierController.java");

        assertContains(controller, "@RequestMapping(\"/logistics/admin/carriers\")");
        for (String permission : new String[] {
            "logistics:carrier:list",
            "logistics:carrier:query",
            "logistics:carrier:add",
            "logistics:carrier:edit",
            "logistics:carrier:credential",
            "logistics:carrier:sync",
            "logistics:carrier:channel",
            "logistics:carrier:label",
            "logistics:carrier:log"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + permission + "')");
        }

        assertContains(controller,
            "@PreAuthorize(\"@ss.hasPermi('logistics:carrier:label')\")\n"
                + "    @Log(title = \"物流商报价\", businessType = BusinessType.OTHER,\n"
                + "            excludeParamNames = { \"recipientAddress\", \"shipperAddress\", \"boxes\" })\n"
                + "    @PostMapping(\"/quote\")");
        assertContains(controller,
            "@PreAuthorize(\"@ss.hasPermi('logistics:carrier:label')\")\n"
                + "    @Log(title = \"物流商创建面单\", businessType = BusinessType.INSERT,\n"
                + "            excludeParamNames = { \"recipientAddress\", \"shipperAddress\", \"boxes\" })\n"
                + "    @PostMapping(\"/labels\")");
        assertContains(controller,
            "@Log(title = \"AGG56物流商授权\", businessType = BusinessType.UPDATE,\n"
                + "            excludeParamNames = { \"appToken\", \"appKey\", \"appTokenCiphertext\", \"appKeyCiphertext\" })");
    }

    @Test
    public void logisticsCarrierSqlMustKeepGuardedMenuAndPermissionContract() throws IOException
    {
        String sql = readBackend("sql/20260610_logistics_carrier_management.sql");

        assertContains(sql, "@confirm_logistics_carrier_management");
        assertContains(sql, "APPLY_LOGISTICS_CARRIER_MANAGEMENT");
        assertContains(sql, "signal sqlstate '45000'");
        assertContains(sql, "call assert_logistics_carrier_menu_guard();");
        assertContains(sql, "call assert_logistics_carrier_management_completed();");
        assertContains(sql, "component = 'Logistics/Carrier/index'");
        assertContains(sql, "perms = 'logistics:carrier:list'");

        for (String permission : new String[] {
            "logistics:carrier:query",
            "logistics:carrier:add",
            "logistics:carrier:edit",
            "logistics:carrier:credential",
            "logistics:carrier:sync",
            "logistics:carrier:channel",
            "logistics:carrier:label",
            "logistics:carrier:log"
        })
        {
            assertContains(sql, "'" + permission + "'");
        }
    }

    @Test
    public void logisticsCarrierAccountRefactorSqlMustGuardAccountIdBackfillContract() throws IOException
    {
        String sql = readBackend("sql/20260610_logistics_carrier_account_refactor.sql");

        assertContains(sql, "set names utf8mb4;");
        assertContains(sql, "@confirm_logistics_carrier_account_refactor");
        assertContains(sql, "APPLY_LOGISTICS_CARRIER_ACCOUNT_REFACTOR");
        assertContains(sql, "signal sqlstate '45000'");
        assertContains(sql, "where table_schema = database()");
        assertContains(sql, "prepare stmt from @ddl;");
        assertContains(sql, "call assert_logistics_account_id_backfilled();");

        for (String table : new String[] {
            "logistics_carrier_connection",
            "logistics_agg56_connection",
            "logistics_carrier_channel_candidate",
            "logistics_carrier_channel_mapping",
            "logistics_label_order",
            "logistics_carrier_request_log"
        })
        {
            assertContains(sql, table);
        }

        for (String indexName : new String[] {
            "uk_logistics_connection_account_id",
            "uk_logistics_agg56_account",
            "idx_logistics_candidate_account_status",
            "idx_logistics_mapping_account_system",
            "idx_logistics_label_account_provider_order",
            "idx_logistics_request_log_account_time"
        })
        {
            assertContains(sql, indexName);
        }

        assertContains(sql, "drop procedure if exists add_logistics_column_if_missing;");
        assertContains(sql, "drop procedure if exists add_logistics_index_if_missing;");
    }

    @Test
    public void logisticsCarrierChannelRenameSqlMustGuardUtf8AndNarrowTargets() throws IOException
    {
        String sql = readBackend("sql/20260610_logistics_carrier_channel_rename.sql");

        assertContains(sql, "set names utf8mb4;");
        assertContains(sql, "@confirm_logistics_carrier_channel_rename");
        assertContains(sql, "APPLY_LOGISTICS_CARRIER_CHANNEL_RENAME");
        assertContains(sql, "signal sqlstate '45000'");
        assertContains(sql, "call assert_logistics_carrier_channel_rename_completed();");
        assertContains(sql, "dict_type = 'logistics_channel_status'");
        assertContains(sql, "where menu_id in (2514, 2515)");
        assertContains(sql, "replace(remark, '候选渠道', '物流商渠道')");
        assertContains(sql, "comment = '物流商渠道表'");
    }

    @Test
    public void adminControllerMustGuardAllSystemChannelActions() throws IOException
    {
        String controller = readBackend(
            "ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/AdminLogisticsSystemChannelController.java");

        assertContains(controller, "@RequestMapping(\"/logistics/admin/system-channels\")");
        for (String permission : new String[] {
            "logistics:systemChannel:list",
            "logistics:systemChannel:query",
            "logistics:systemChannel:add",
            "logistics:systemChannel:edit",
            "logistics:systemChannel:status",
            "logistics:systemChannel:binding",
            "logistics:systemChannel:rule"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + permission + "')");
        }

        assertContains(controller, "@GetMapping({\"\", \"/list\"})");
        assertContains(controller, "@GetMapping(\"/{systemChannelCode}/warehouses/list\")");
        assertContains(controller, "@PutMapping(\"/{systemChannelCode}/order-setting\")");
        assertNotContains(controller, "buyer-scope");
        assertNotContains(controller, "platform-mappings");
        assertNotContains(controller, "options/buyers");
        assertNotContains(controller, "logistics:systemChannel:platformMapping");
    }

    @Test
    public void systemLogisticsChannelSqlMustKeepGuardedMenuAndSchemaContract() throws IOException
    {
        String sql = readBackend("sql/20260610_system_logistics_channel_management.sql");

        assertContains(sql, "@confirm_system_logistics_channel_management");
        assertContains(sql, "APPLY_SYSTEM_LOGISTICS_CHANNEL_MANAGEMENT");
        assertContains(sql, "signal sqlstate '45000'");
        assertContains(sql, "call assert_system_logistics_channel_menu_guard();");
        assertContains(sql, "call assert_system_logistics_channel_management_completed();");
        assertContains(sql, "component = 'Channel/System/index'");
        assertContains(sql, "perms = 'logistics:systemChannel:list'");

        for (String table : new String[] {
            "logistics_system_channel_warehouse",
            "logistics_system_channel_order_setting"
        })
        {
            assertContains(sql, "create table if not exists " + table);
        }

        for (String field : new String[] {
            "fulfillment_mode",
            "signature_services",
            "shipper_address_mode",
            "external_shipper_code",
            "destination_countries"
        })
        {
            assertContains(sql, field);
        }
        assertContains(sql, "logistics_system_channel_fulfillment_mode");
        assertContains(sql, "CARRIER_LABELING");
        assertContains(sql, "DIRECT_FULFILLMENT_WAREHOUSE");
        assertNotContains(sql, "service_level");
        assertNotContains(sql, "logistics_system_channel_buyer_scope");
        assertNotContains(sql, "logistics_platform_channel_mapping");
        assertNotContains(sql, "logistics_channel_buyer_scope_mode");
        assertNotContains(sql, "logistics_platform_kind");

        for (String permission : new String[] {
            "logistics:systemChannel:query",
            "logistics:systemChannel:add",
            "logistics:systemChannel:edit",
            "logistics:systemChannel:status",
            "logistics:systemChannel:binding",
            "logistics:systemChannel:rule"
        })
        {
            assertContains(sql, "'" + permission + "'");
        }
        assertNotContains(sql, "logistics:systemChannel:platformMapping");
    }

    @Test
    public void systemChannelFulfillmentModeMustControlCarrierMappingRule() throws IOException
    {
        String request = readLogistics(
            "src/main/java/com/ruoyi/logistics/domain/request/LogisticsSystemChannelRequest.java");
        String domain = readLogistics("src/main/java/com/ruoyi/logistics/domain/LogisticsSystemChannel.java");
        String service = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsSystemChannelServiceImpl.java");
        String mapper = readLogistics("src/main/resources/mapper/logistics/LogisticsSystemChannelMapper.xml");

        assertContains(request, "private String fulfillmentMode;");
        assertContains(domain, "private String fulfillmentMode;");
        assertContains(mapper, "fulfillment_mode");
        assertContains(mapper, "and sc.fulfillment_mode = #{fulfillmentMode}");
        assertContains(service, "FULFILLMENT_MODE_CARRIER_LABELING");
        assertContains(service, "FULFILLMENT_MODE_DIRECT_WAREHOUSE");
        assertContains(service, "channel.setFulfillmentMode(normalizeFulfillmentMode(request.getFulfillmentMode()))");
        assertContains(service, "直推履约仓渠道不需要维护物流商映射");
    }

    @Test
    public void warehouseShipperAddressMustStayOptionalForConfiguredBindings() throws IOException
    {
        String service = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsSystemChannelServiceImpl.java");

        assertContains(service, "String externalShipperCode = trimOptional(request.getExternalShipperCode())");
        assertContains(service, "binding.setExternalShipperCode(externalShipperCode)");
        assertContains(service, "binding.setShipperAddressLine1(trimOptional(request.getShipperAddressLine1()))");
        assertNotContains(service, "使用外部发货地址编码时，外部物流商发货地址编码不能为空");
        assertNotContains(service, "发货联系人不能为空");
        assertNotContains(service, "发货地址1不能为空");
    }

    @Test
    public void adminControllerMustGuardAllCustomerChannelActions() throws IOException
    {
        String controller = readBackend(
            "ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/AdminLogisticsCustomerChannelController.java");

        assertContains(controller, "@RequestMapping(\"/logistics/admin/customer-channels\")");
        for (String permission : new String[] {
            "logistics:customerChannel:list",
            "logistics:customerChannel:query",
            "logistics:customerChannel:add",
            "logistics:customerChannel:edit",
            "logistics:customerChannel:status",
            "logistics:customerChannel:binding",
            "logistics:customerChannel:buyer"
        })
        {
            assertContains(controller, "@ss.hasPermi('" + permission + "')");
        }

        assertContains(controller, "@GetMapping({\"\", \"/list\"})");
        assertContains(controller, "@GetMapping(\"/{customerChannelCode}/system-mappings/list\")");
        assertContains(controller, "@GetMapping(\"/{customerChannelCode}/quote-channel-mappings/list\")");
        assertContains(controller, "@PostMapping(\"/{customerChannelCode}/quote-channel-mappings\")");
        assertContains(controller, "@DeleteMapping(\"/{customerChannelCode}/quote-channel-mappings/{mappingId}\")");
        assertContains(controller, "LogisticsCustomerChannelQuoteMappingRequest");
        assertContains(controller, "@PutMapping(\"/{customerChannelCode}/buyer-scope\")");
        assertContains(controller, "@GetMapping(\"/options/system-channels\")");
        assertContains(controller, "@GetMapping(\"/options/buyers\")");
        assertNotContains(controller, "logistics:systemChannel");
        assertNotContains(controller, "/api/seller/");
        assertNotContains(controller, "/api/buyer/");
    }

    @Test
    public void customerLogisticsChannelSqlMustKeepGuardedMenuAndSchemaContract() throws IOException
    {
        String sql = readBackend("sql/20260610_customer_logistics_channel_management.sql");

        assertContains(sql, "@confirm_customer_logistics_channel_management");
        assertContains(sql, "APPLY_CUSTOMER_LOGISTICS_CHANNEL_MANAGEMENT");
        assertContains(sql, "signal sqlstate '45000'");
        assertContains(sql, "call assert_customer_logistics_channel_menu_guard();");
        assertContains(sql, "call assert_customer_logistics_channel_management_completed();");
        assertContains(sql, "component = 'Channel/Customer/index'");
        assertContains(sql, "perms = 'logistics:customerChannel:list'");
        assertContains(sql, "channel:customer:query");
        assertContains(sql, "channel:customer:add");

        for (String table : new String[] {
            "logistics_customer_channel",
            "logistics_customer_channel_system_mapping",
            "logistics_customer_channel_quote_mapping",
            "logistics_customer_channel_buyer_scope"
        })
        {
            assertContains(sql, "create table if not exists " + table);
        }

        for (String field : new String[] {
            "channel_type",
            "label_upload_required",
            "platform_label_fetch",
            "customer_label_upload_supported",
            "buyer_scope_mode",
            "system_channel_name_snapshot",
            "buyer_code_snapshot",
            "buyer_short_name_snapshot"
        })
        {
            assertContains(sql, field);
        }

        for (String dictType : new String[] {
            "logistics_customer_channel_type",
            "logistics_label_upload_required",
            "logistics_platform_label_fetch",
            "logistics_customer_label_upload_support",
            "logistics_customer_channel_scope_mode"
        })
        {
            assertContains(sql, dictType);
        }

        for (String permission : new String[] {
            "logistics:customerChannel:query",
            "logistics:customerChannel:add",
            "logistics:customerChannel:edit",
            "logistics:customerChannel:status",
            "logistics:customerChannel:binding",
            "logistics:customerChannel:buyer"
        })
        {
            assertContains(sql, "'" + permission + "'");
        }
        assertNotContains(sql, "logistics_platform_channel_mapping");
        assertNotContains(sql, "logistics_system_channel_buyer_scope");
    }

    @Test
    public void customerChannelServiceMustEnforceLabelAndBuyerScopeRules() throws IOException
    {
        String service = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsCustomerChannelServiceImpl.java");

        assertContains(service, "CHANNEL_TYPE_WAREHOUSE_LABEL");
        assertContains(service, "CHANNEL_TYPE_THIRD_PARTY_LABEL");
        assertContains(service, "channel.setLabelUploadRequired(LABEL_UPLOAD_NOT_REQUIRED)");
        assertContains(service, "channel.setPlatformLabelFetch(PLATFORM_LABEL_NOT_FETCH)");
        assertContains(service, "channel.setCustomerLabelUploadSupported(CUSTOMER_LABEL_UNSUPPORTED)");
        assertContains(service, "需要上传物流面单时，平台面单获取和客户上传面单至少开启一个");
        assertContains(service, "BUYER_SCOPE_ALL");
        assertContains(service, "BUYER_SCOPE_INCLUDE");
        assertContains(service, "BUYER_SCOPE_EXCLUDE");
        assertContains(service, "buyerMapper.selectBuyerById(buyerId)");
        assertContains(service, "systemChannelMapper.selectSystemChannelByCode(systemChannelCode)");
        assertContains(service, "UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE");
        assertContains(service, "UpstreamSystemConstants.PAIRING_ROLE_QUOTE");
        assertContains(service, "upstreamSystemService.selectLogisticsChannelSyncList");
        assertContains(service, "customerChannelMapper.deleteQuoteMapping");
        assertContains(service, "channel.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);");
        assertContains(service, "mapping.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);");
        assertNotContains(service, "request.getDisplayOrder()");
        assertNotContains(service, "sellerMapper");
    }

    @Test
    public void channelCreatesMustInitializeLastUpdateAuditFields() throws IOException
    {
        String systemService = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsSystemChannelServiceImpl.java");
        String customerService = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsCustomerChannelServiceImpl.java");
        String systemMapper = readLogistics("src/main/resources/mapper/logistics/LogisticsSystemChannelMapper.xml");
        String customerMapper = readLogistics("src/main/resources/mapper/logistics/LogisticsCustomerChannelMapper.xml");

        assertContains(systemService, "channel.setCreateBy(username);");
        assertContains(systemService, "channel.setUpdateBy(username);");
        assertContains(customerService, "channel.setCreateBy(username);");
        assertContains(customerService, "channel.setUpdateBy(username);");
        assertContains(systemMapper, "display_order, create_by, create_time, update_by, update_time, remark");
        assertContains(customerMapper, "display_order, create_by, create_time, update_by, update_time, remark");
        assertContains(systemMapper, "#{displayOrder}, #{createBy}, sysdate(), #{updateBy}, sysdate(), #{remark}");
        assertContains(customerMapper, "#{displayOrder}, #{createBy}, sysdate(), #{updateBy}, sysdate(), #{remark}");
    }

    @Test
    public void carrierRequestLogListMustKeepPageHelperOnLogQuery() throws IOException
    {
        String service = readLogistics(
            "src/main/java/com/ruoyi/logistics/service/impl/LogisticsCarrierServiceImpl.java");
        int methodStart = service.indexOf("public List<LogisticsCarrierRequestLog> selectRequestLogList");
        int methodEnd = service.indexOf("\n    private void fillProviderExtension", methodStart);
        assertTrue("selectRequestLogList method must exist", methodStart >= 0 && methodEnd > methodStart);
        String method = service.substring(methodStart, methodEnd);

        assertContains(method, "return logisticsCarrierMapper.selectRequestLogList(carrierAccountId);");
        assertNotContains(method, "selectConnectionByAccountId");
        assertNotContains(method, "requireProviderConnection");
    }

    private static String readBackend(String relativePath) throws IOException
    {
        return Files.readString(backendRoot().resolve(relativePath), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
    }

    private static String readLogistics(String relativePath) throws IOException
    {
        return Files.readString(backendRoot().resolve("logistics").resolve(relativePath), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
    }

    private static Path backendRoot()
    {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null)
        {
            if (Files.exists(current.resolve("ruoyi-admin")) && Files.exists(current.resolve("pom.xml")))
            {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate RuoYi-Vue backend root");
    }

    private static void assertContains(String source, String expected)
    {
        assertTrue("Expected source to contain: " + expected, source.contains(expected));
    }

    private static void assertNotContains(String source, String unexpected)
    {
        assertTrue("Expected source not to contain: " + unexpected, !source.contains(unexpected));
    }
}
