#!/usr/bin/env node

import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const envPath = path.join(repoRoot, '.env.local');
const sqlPath = path.join(repoRoot, 'RuoYi-Vue', 'sql', '20260612_buyer_product_center_menu_seed.sql');
const confirmEnv = 'BUYER_PRODUCT_CENTER_SQL_CONFIRM';
const confirmValue = 'APPLY_BUYER_PRODUCT_CENTER_MENU_SEED';

const args = new Set(process.argv.slice(2));

function printHelp() {
  console.log(`Usage:
  node scripts/buyer-product-center-sql-runner.mjs --precheck
  ${confirmEnv}=${confirmValue} node scripts/buyer-product-center-sql-runner.mjs --apply

Default behavior is read-only precheck. Apply mode requires --apply and the
explicit confirmation environment variable. The runner reads .env.local for the
current RUOYI_DB_* target but never prints the connection string, username, or
password.
`);
}

if (args.has('--help') || args.has('-h')) {
  printHelp();
  process.exit(0);
}

const apply = args.has('--apply');
const precheck = args.has('--precheck') || !apply;
if (apply && args.has('--precheck')) {
  throw new Error('Use either --precheck or --apply, not both.');
}
if (apply && process.env[confirmEnv] !== confirmValue) {
  throw new Error(`Apply mode requires ${confirmEnv}=${confirmValue}.`);
}
if (!fs.existsSync(sqlPath)) {
  throw new Error(`SQL file not found: ${path.relative(repoRoot, sqlPath)}`);
}

const env = readLocalEnv(envPath);
for (const key of ['RUOYI_DB_URL', 'RUOYI_DB_USERNAME', 'RUOYI_DB_PASSWORD']) {
  if (!env[key]) {
    throw new Error(`Missing ${key} in .env.local.`);
  }
}

const driverJar = findMysqlDriverJar();
const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'urili-buyer-product-center-sql-runner-'));
const sourcePath = path.join(tempDir, 'BuyerProductCenterSqlRunner.java');

try {
  fs.writeFileSync(sourcePath, javaSource(), 'utf8');
  const javaResult = spawnSync('java', [
    '-cp',
    driverJar,
    sourcePath,
    apply ? '--apply' : '--precheck',
    sqlPath,
  ], {
    cwd: repoRoot,
    encoding: 'utf8',
    env: {
      ...process.env,
      RUOYI_SQL_RUNNER_DB_URL: env.RUOYI_DB_URL,
      RUOYI_SQL_RUNNER_DB_USERNAME: env.RUOYI_DB_USERNAME,
      RUOYI_SQL_RUNNER_DB_PASSWORD: env.RUOYI_DB_PASSWORD,
    },
    windowsHide: true,
  });

  process.stdout.write(javaResult.stdout);
  process.stderr.write(redact(javaResult.stderr));
  if (javaResult.status !== 0) {
    process.exit(javaResult.status ?? 1);
  }
  if (precheck) {
    console.log('buyer product center SQL precheck completed without writes.');
  } else {
    console.log('buyer product center SQL seed applied with explicit confirmation.');
  }
} finally {
  fs.rmSync(tempDir, { recursive: true, force: true });
}

function readLocalEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error('.env.local is required for current datasource resolution.');
  }
  const result = {};
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || !trimmed.includes('=')) {
      continue;
    }
    const index = trimmed.indexOf('=');
    const key = trimmed.slice(0, index).trim();
    let value = trimmed.slice(index + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
}

function findMysqlDriverJar() {
  const home = os.homedir();
  const candidates = [
    path.join(home, '.m2', 'repository', 'com', 'mysql', 'mysql-connector-j'),
    path.join(home, '.m2', 'repository', 'mysql', 'mysql-connector-java'),
  ];
  const jars = [];
  for (const baseDir of candidates) {
    collectJars(baseDir, jars);
  }
  if (jars.length === 0) {
    throw new Error('MySQL JDBC driver was not found in the local Maven cache.');
  }
  jars.sort((left, right) => right.localeCompare(left, undefined, { numeric: true }));
  return jars[0];
}

function collectJars(dir, target) {
  if (!fs.existsSync(dir)) {
    return;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      collectJars(fullPath, target);
    } else if (/mysql-connector-(?:j|java)-.*\.jar$/.test(entry.name)) {
      target.push(fullPath);
    }
  }
}

function redact(text) {
  let value = text;
  for (const secret of [env.RUOYI_DB_URL, env.RUOYI_DB_USERNAME, env.RUOYI_DB_PASSWORD]) {
    if (secret) {
      value = value.split(secret).join('[redacted]');
    }
  }
  return value;
}

function javaSource() {
  return String.raw`
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class BuyerProductCenterSqlRunner {
    private static final String CONFIRM_VALUE = "APPLY_BUYER_PRODUCT_CENTER_MENU_SEED";

    public static void main(String[] args) throws Exception {
        boolean apply = args.length > 0 && "--apply".equals(args[0]);
        String sqlPath = args.length > 1 ? args[1] : "";
        String url = requireEnv("RUOYI_SQL_RUNNER_DB_URL");
        String username = requireEnv("RUOYI_SQL_RUNNER_DB_USERNAME");
        String password = requireEnv("RUOYI_SQL_RUNNER_DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setReadOnly(!apply);
            if (apply) {
                applySeed(connection, sqlPath);
                runPrecheck(connection, "postcheck");
            } else {
                runPrecheck(connection, "precheck");
            }
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required runner environment: " + name);
        }
        return value;
    }

    private static void applySeed(Connection connection, String sqlPath) throws Exception {
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement confirm = connection.prepareStatement(
                    "set @confirm_buyer_product_center_menu_seed = ?")) {
                confirm.setString(1, CONFIRM_VALUE);
                confirm.execute();
            }
            for (String statement : splitSqlStatements(Files.readString(Path.of(sqlPath), StandardCharsets.UTF_8))) {
                if (isExecutableStatement(statement)) {
                    try (Statement jdbcStatement = connection.createStatement()) {
                        jdbcStatement.execute(statement);
                    }
                }
            }
            connection.commit();
        } catch (Exception error) {
            connection.rollback();
            throw error;
        }
    }

    private static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        String delimiter = ";";
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("delimiter ")) {
                addStatement(statements, current, delimiter);
                delimiter = trimmed.substring("delimiter ".length()).trim();
                continue;
            }
            current.append(line).append('\n');
            addStatement(statements, current, delimiter);
        }
        addStatement(statements, current, delimiter);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current, String delimiter) {
        String text = current.toString();
        String trimmed = text.trim();
        if (!trimmed.endsWith(delimiter)) {
            return;
        }
        String statement = trimmed.substring(0, trimmed.length() - delimiter.length()).trim();
        statements.add(statement);
        current.setLength(0);
    }

    private static boolean isExecutableStatement(String statement) {
        String stripped = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
        return !stripped.isEmpty();
    }

    private static void runPrecheck(Connection connection, String phase) throws Exception {
        Map<String, Long> counts = new HashMap<>();
        String[] queries = new String[] {
            "select 'current_database' as item, database() as value_text, null as rows_count, null as min_id, null as max_id",
            "select 'buyer_menu' as item, null as value_text, count(*) as rows_count, min(buyer_menu_id) as min_id, max(buyer_menu_id) as max_id from buyer_menu",
            "select 'buyer_owner_role' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_role where del_flag = '0' and status = '0' and role_key = 'owner'",
            "select 'buyer_product_center_page_menu' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_menu where perms = 'buyer:product:center:list' and parent_id = 0 and menu_type = 'C' and coalesce(path, '') = '/buyer/portal/product-center' and coalesce(component, '') = 'Buyer/ProductCenter/index' and coalesce(route_name, '') = 'BuyerProductCenter'",
            "select 'buyer_product_center_query_permission' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_menu child join buyer_menu parent on parent.buyer_menu_id = child.parent_id where parent.perms = 'buyer:product:center:list' and child.perms = 'buyer:product:center:query' and child.menu_type = 'F' and coalesce(child.path, '') = '' and coalesce(child.component, '') = '' and coalesce(child.route_name, '') = ''",
            "select 'buyer_owner_product_center_grants' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_role r join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id where r.del_flag = '0' and r.status = '0' and r.role_key = 'owner' and m.perms in ('buyer:product:center:list','buyer:product:center:query')",
            "select 'buyer_invalid_menu_perms' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_menu where coalesce(perms, '') = '' or coalesce(perms, '') like '%*%' or coalesce(perms, '') not like 'buyer:%' or coalesce(perms, '') like 'buyer:admin:%' or coalesce(perms, '') like 'seller:%'",
            "select 'buyer_menu_id_range_violations' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_menu where buyer_menu_id > 0 and (buyer_menu_id < 200000 or buyer_menu_id >= 300000)",
            "select 'buyer_invalid_page_components' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from buyer_menu where menu_type = 'C' and (coalesce(trim(component), '') = '' or lower(coalesce(trim(component), '')) not like 'buyer/%')",
            "select 'buyer_duplicate_menu_perms' as item, null as value_text, count(*) as rows_count, null as min_id, null as max_id from (select perms from buyer_menu group by perms having count(1) > 1) duplicate_buyer_menu_perms"
        };
        System.out.println("phase\titem\tvalue_text\trows_count\tmin_id\tmax_id");
        for (String sql : queries) {
            try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String item = rs.getString("item");
                    Object rowCount = rs.getObject("rows_count");
                    if (rowCount instanceof Number number) {
                        counts.put(item, number.longValue());
                    }
                    System.out.printf(Locale.ROOT, "%s\t%s\t%s\t%s\t%s\t%s%n",
                            phase,
                            item,
                            Objects.toString(rs.getString("value_text"), ""),
                            Objects.toString(rowCount, ""),
                            Objects.toString(rs.getObject("min_id"), ""),
                            Objects.toString(rs.getObject("max_id"), ""));
                }
            }
        }
        if ("postcheck".equals(phase)) {
            assertPostcheck(counts);
            System.out.println("postcheck exact buyer product center permission state verified.");
        }
    }

    private static void assertPostcheck(Map<String, Long> counts) {
        long buyerOwnerRoles = requireCount(counts, "buyer_owner_role");
        assertCount("buyer_product_center_page_menu", 1, requireCount(counts, "buyer_product_center_page_menu"));
        assertCount("buyer_product_center_query_permission", 1,
                requireCount(counts, "buyer_product_center_query_permission"));
        assertCount("buyer_owner_product_center_grants", buyerOwnerRoles * 2,
                requireCount(counts, "buyer_owner_product_center_grants"));
        assertCount("buyer_invalid_menu_perms", 0, requireCount(counts, "buyer_invalid_menu_perms"));
        assertCount("buyer_menu_id_range_violations", 0,
                requireCount(counts, "buyer_menu_id_range_violations"));
        assertCount("buyer_invalid_page_components", 0,
                requireCount(counts, "buyer_invalid_page_components"));
        assertCount("buyer_duplicate_menu_perms", 0, requireCount(counts, "buyer_duplicate_menu_perms"));
    }

    private static long requireCount(Map<String, Long> counts, String item) {
        Long value = counts.get(item);
        if (value == null) {
            throw new IllegalStateException("postcheck did not return count for " + item);
        }
        return value;
    }

    private static void assertCount(String item, long expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    item + " expected " + expected + " after seed apply, but was " + actual);
        }
    }
}
`;
}
