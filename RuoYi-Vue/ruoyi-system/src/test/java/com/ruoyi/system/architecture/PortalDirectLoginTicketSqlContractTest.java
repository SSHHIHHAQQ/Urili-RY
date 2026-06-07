package com.ruoyi.system.architecture;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PortalDirectLoginTicketSqlContractTest
{
    @Test
    public void directLoginTicketMigrationMustGuardExpectedColumnsAndIndexesWithoutDirtyPlaceholders() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/20260604_portal_direct_login_ticket.sql");
        String sql = readText(path);
        List<String> violations = new ArrayList<>();

        requireContains(violations, path.getFileName().toString(), sql, "create table if not exists portal_direct_login_ticket");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure add_column_if_missing");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure recreate_index_if_mismatch");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure assert_index_definition");
        requireContains(violations, path.getFileName().toString(), sql,
                "create procedure assert_no_invalid_direct_login_ticket_rows");
        requireContains(violations, path.getFileName().toString(), sql, "create procedure assert_column_exists");
        requireContains(violations, path.getFileName().toString(), sql,
                "create procedure modify_portal_direct_login_ticket_columns_if_needed");
        requireContains(violations, path.getFileName().toString(), sql,
                "call assert_column_exists('portal_direct_login_ticket', 'ticket_id'");
        requireNotContains(violations, path.getFileName().toString(), sql,
                "update portal_direct_login_ticket set token_hash = concat('legacy-', ticket_id)");
        requireNotContains(violations, path.getFileName().toString(), sql,
                "update portal_direct_login_ticket set terminal = ''");
        requireNotContains(violations, path.getFileName().toString(), sql,
                "update portal_direct_login_ticket set target_subject_id = 0");
        requireNotContains(violations, path.getFileName().toString(), sql,
                "update portal_direct_login_ticket set expire_time = coalesce(create_time, sysdate())");
        requireContains(violations, path.getFileName().toString(), sql,
                "or token_hash like 'legacy-%'");
        requireContains(violations, path.getFileName().toString(), sql,
                "call assert_no_invalid_direct_login_ticket_rows()");
        requireContains(violations, path.getFileName().toString(), sql,
                "portal_direct_login_ticket expected columns are required before ticket column modify");
        requireContains(violations, path.getFileName().toString(), sql,
                "lower(c.data_type) <> expected.expected_type");
        requireContains(violations, path.getFileName().toString(), sql,
                "coalesce(c.character_maximum_length, -1) <> expected.expected_length");
        requireContains(violations, path.getFileName().toString(), sql,
                "c.is_nullable <> expected.expected_nullable");
        requireContains(violations, path.getFileName().toString(), sql,
                "coalesce(c.column_default, '<NULL>') <> coalesce(expected.expected_default, '<NULL>')");
        requireContains(violations, path.getFileName().toString(), sql,
                "modify token_hash varchar(64) not null");
        requireContains(violations, path.getFileName().toString(), sql,
                "modify expire_time datetime not null");
        requireContains(violations, path.getFileName().toString(), sql,
                "call modify_portal_direct_login_ticket_columns_if_needed()");
        requireContains(violations, path.getFileName().toString(), sql,
                "drop procedure if exists modify_portal_direct_login_ticket_columns_if_needed");
        requireNotContains(violations, path.getFileName().toString(), sql,
                "alter table portal_direct_login_ticket\n  modify terminal varchar(20) not null");

        for (String column : Arrays.asList(
                "terminal",
                "target_subject_id",
                "target_subject_no",
                "target_account_id",
                "target_user_name",
                "acting_admin_id",
                "acting_admin_name",
                "reason",
                "token_hash",
                "expire_time",
                "used_time",
                "used_ip",
                "status",
                "create_by",
                "create_time",
                "update_by",
                "update_time",
                "remark"))
        {
            requireContains(violations, path.getFileName().toString(), sql,
                    "call add_column_if_missing('portal_direct_login_ticket', '" + column + "'");
        }

        requireContains(violations, path.getFileName().toString(), sql,
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash',\n"
                        + "  'token_hash', 0");
        requireContains(violations, path.getFileName().toString(), sql,
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_target',\n"
                        + "  'terminal,target_subject_id,target_account_id', 1");
        requireContains(violations, path.getFileName().toString(), sql,
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_admin_time',\n"
                        + "  'acting_admin_id,create_time', 1");
        requireContains(violations, path.getFileName().toString(), sql,
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_status_expire',\n"
                        + "  'status,expire_time', 1");
        requireContains(violations, path.getFileName().toString(), sql,
                "call assert_index_definition('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash'");
        requireContains(violations, path.getFileName().toString(), sql,
                "call assert_index_definition('portal_direct_login_ticket', 'idx_portal_direct_login_ticket_target'");
        assertAppearsBefore(violations, path.getFileName().toString(), sql,
                "call assert_no_invalid_direct_login_ticket_rows()",
                "call modify_portal_direct_login_ticket_columns_if_needed()");
        assertAppearsBefore(violations, path.getFileName().toString(), sql,
                "call modify_portal_direct_login_ticket_columns_if_needed()",
                "call recreate_index_if_mismatch('portal_direct_login_ticket', 'uk_portal_direct_login_ticket_hash'");

        requireContains(violations, path.getFileName().toString(), sql,
                "unique key uk_portal_direct_login_ticket_hash (token_hash)");
        requireContains(violations, path.getFileName().toString(), sql,
                "key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id)");
        requireContains(violations, path.getFileName().toString(), sql,
                "key idx_portal_direct_login_ticket_status_expire (status, expire_time)");

        if (!violations.isEmpty())
        {
            fail("portal_direct_login_ticket SQL must guard the audited ticket schema without dirty placeholders:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void directLoginTicketBootstrapSeedMustMatchAuditedTicketSchema() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path path = backendRoot.resolve("sql/seller_buyer_management_seed.sql");
        String sql = readText(path);
        List<String> violations = new ArrayList<>();

        requireContains(violations, path.getFileName().toString(), sql,
                "create table if not exists portal_direct_login_ticket");
        for (String column : Arrays.asList(
                "ticket_id",
                "terminal",
                "target_subject_id",
                "target_subject_no",
                "target_account_id",
                "target_user_name",
                "acting_admin_id",
                "acting_admin_name",
                "reason",
                "token_hash",
                "expire_time",
                "used_time",
                "used_ip",
                "status",
                "create_by",
                "create_time",
                "update_by",
                "update_time",
                "remark"))
        {
            requireContains(violations, path.getFileName().toString(), sql, column);
        }
        requireContains(violations, path.getFileName().toString(), sql,
                "unique key uk_portal_direct_login_ticket_hash (token_hash)");
        requireContains(violations, path.getFileName().toString(), sql,
                "key idx_portal_direct_login_ticket_target (terminal, target_subject_id, target_account_id)");
        requireContains(violations, path.getFileName().toString(), sql,
                "key idx_portal_direct_login_ticket_admin_time (acting_admin_id, create_time)");
        requireContains(violations, path.getFileName().toString(), sql,
                "key idx_portal_direct_login_ticket_status_expire (status, expire_time)");
        requireContains(violations, path.getFileName().toString(), sql,
                "comment = '管理端免密代入审计票据表'");

        if (!violations.isEmpty())
        {
            fail("seller_buyer_management_seed.sql must bootstrap portal_direct_login_ticket with the audited schema:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    public void directLoginTicketMapperMustKeepOneTimeUseAndExpiryGuards() throws IOException
    {
        Path backendRoot = findBackendRoot();
        Path mapper = backendRoot.resolve(
                "ruoyi-system/src/main/resources/mapper/system/PortalDirectLoginTicketMapper.xml");
        String xml = readText(mapper);
        List<String> violations = new ArrayList<>();

        String usedUpdate = extractXmlStatement(xml, "update", "markPortalDirectLoginTicketUsed");
        String expiredUpdate = extractXmlStatement(xml, "update", "markPortalDirectLoginTicketExpired");
        if (usedUpdate.isEmpty())
        {
            violations.add(mapper.getFileName() + " must define markPortalDirectLoginTicketUsed");
        }
        if (expiredUpdate.isEmpty())
        {
            violations.add(mapper.getFileName() + " must define markPortalDirectLoginTicketExpired");
        }

        requireContains(violations, "markPortalDirectLoginTicketUsed", usedUpdate, "set status = 'USED'");
        requireContains(violations, "markPortalDirectLoginTicketUsed", usedUpdate, "where ticket_id = #{ticketId}");
        requireContains(violations, "markPortalDirectLoginTicketUsed", usedUpdate, "and status = 'ISSUED'");
        requireContains(violations, "markPortalDirectLoginTicketUsed", usedUpdate, "and used_time is null");
        requireContains(violations, "markPortalDirectLoginTicketUsed", usedUpdate,
                "and expire_time &gt;= #{usedTime}");

        requireContains(violations, "markPortalDirectLoginTicketExpired", expiredUpdate, "set status = 'EXPIRED'");
        requireContains(violations, "markPortalDirectLoginTicketExpired", expiredUpdate,
                "where ticket_id = #{ticketId}");
        requireContains(violations, "markPortalDirectLoginTicketExpired", expiredUpdate, "and status = 'ISSUED'");
        requireContains(violations, "markPortalDirectLoginTicketExpired", expiredUpdate, "and used_time is null");

        Path support = backendRoot.resolve(
                "ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java");
        String supportSource = readText(support);
        requireContains(violations, support.getFileName().toString(), supportSource,
                "ticket.getExpireTime() == null || ticket.getExpireTime().before(now)");
        requireContains(violations, support.getFileName().toString(), supportSource,
                "payload.getExpireTime() == null || payload.getExpireTime().before(now)");
        requireContains(violations, support.getFileName().toString(), supportSource,
                "ticketMapper.markPortalDirectLoginTicketUsed(ticket.getTicketId(), now");
        requireContains(violations, support.getFileName().toString(), supportSource,
                "ticketMapper.markPortalDirectLoginTicketExpired(ticket.getTicketId(), SYSTEM_OPERATOR)");

        if (!violations.isEmpty())
        {
            fail("portal direct-login tickets must stay one-time, terminal-scoped, and expiration guarded:\n"
                    + String.join("\n", violations));
        }
    }

    private String extractXmlStatement(String xml, String tagName, String id)
    {
        String openToken = "<" + tagName + " id=\"" + id + "\"";
        int openStart = xml.indexOf(openToken);
        if (openStart < 0)
        {
            return "";
        }
        int bodyStart = xml.indexOf('>', openStart);
        if (bodyStart < 0)
        {
            return "";
        }
        String closeToken = "</" + tagName + ">";
        int closeStart = xml.indexOf(closeToken, bodyStart);
        if (closeStart < 0)
        {
            return "";
        }
        return xml.substring(bodyStart + 1, closeStart);
    }

    private String readText(Path path) throws IOException
    {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private void requireContains(List<String> violations, String fileName, String source, String expected)
    {
        if (!source.contains(expected))
        {
            violations.add(fileName + " must contain: " + expected);
        }
    }

    private void requireNotContains(List<String> violations, String fileName, String source, String forbidden)
    {
        if (source.contains(forbidden))
        {
            violations.add(fileName + " must not contain: " + forbidden);
        }
    }

    private void assertAppearsBefore(List<String> violations, String fileName, String source, String before,
            String after)
    {
        int beforeIndex = source.indexOf(before);
        int afterIndex = source.indexOf(after);
        if (beforeIndex < 0 || afterIndex < 0 || beforeIndex >= afterIndex)
        {
            violations.add(fileName + " must place `" + before + "` before `" + after + "`");
        }
    }

    private Path findBackendRoot()
    {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
                cwd,
                cwd.resolve("..").normalize(),
                cwd.resolve("RuoYi-Vue").normalize()
        };

        for (Path candidate : candidates)
        {
            if (Files.isDirectory(candidate.resolve("seller/src/main/java"))
                    && Files.isDirectory(candidate.resolve("buyer/src/main/java")))
            {
                return candidate;
            }
        }

        throw new AssertionError("Cannot locate RuoYi-Vue backend root from " + cwd);
    }
}
