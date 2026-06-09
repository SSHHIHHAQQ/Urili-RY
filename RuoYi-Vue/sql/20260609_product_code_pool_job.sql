-- Mall product SPU/SKU code pool scheduled maintenance job
-- Scope:
-- 1. Register the Quartz entrypoint in RuoYi sys_job.
-- 2. Trigger every day at 22:59.
-- 3. Do not trigger missed executions immediately.
-- 4. Disallow concurrent execution. The service also guards with a Redis lock.

set names utf8mb4;
set session group_concat_max_len = greatest(@@session.group_concat_max_len, 1048576);

set @confirm_product_code_pool_job := coalesce(@confirm_product_code_pool_job, '');
set @product_code_pool_job_expected_count := coalesce(@product_code_pool_job_expected_count, '');
set @product_code_pool_job_expected_signature := coalesce(@product_code_pool_job_expected_signature, '');

delimiter //

drop procedure if exists assert_product_code_pool_job_confirmed//
create procedure assert_product_code_pool_job_confirmed()
begin
  if coalesce(@confirm_product_code_pool_job, '')
      <> 'APPLY_PRODUCT_CODE_POOL_JOB' then
    signal sqlstate '45000' set message_text = 'set @confirm_product_code_pool_job = APPLY_PRODUCT_CODE_POOL_JOB before running this migration';
  end if;

  if coalesce(@product_code_pool_job_expected_count, '') not regexp '^[0-9]+$' then
    signal sqlstate '45000' set message_text = 'set @product_code_pool_job_expected_count after previewing exact product code pool sys_job row';
  end if;
  if coalesce(@product_code_pool_job_expected_signature, '') not regexp '^[0-9a-fA-F]{64}$' then
    signal sqlstate '45000' set message_text = 'set @product_code_pool_job_expected_signature after previewing exact product code pool sys_job row';
  end if;
end//

drop procedure if exists assert_product_code_pool_job_targets//
create procedure assert_product_code_pool_job_targets()
begin
  declare v_count int default 0;
  declare v_signature varchar(64) default '';

  select count(1),
         sha2(coalesce(group_concat(
           concat_ws(':',
             job_id,
             coalesce(job_name, ''),
             coalesce(job_group, ''),
             coalesce(invoke_target, ''),
             coalesce(cron_expression, ''),
             coalesce(misfire_policy, ''),
             coalesce(concurrent, ''),
             coalesce(status, '')
           )
           order by job_id separator '|'
         ), ''), 256)
    into v_count, v_signature
  from sys_job
  where invoke_target = 'productCodePoolTask.maintainPools';

  if v_count > 1 then
    signal sqlstate '45000' set message_text = 'product code pool sys_job invoke_target must be unique before upsert';
  end if;
  if v_count <> cast(@product_code_pool_job_expected_count as unsigned) then
    signal sqlstate '45000' set message_text = 'product code pool sys_job exact target count mismatch';
  end if;
  if lower(v_signature) <> lower(@product_code_pool_job_expected_signature) then
    signal sqlstate '45000' set message_text = 'product code pool sys_job exact target signature mismatch';
  end if;
end//

delimiter ;

call assert_product_code_pool_job_confirmed();
call assert_product_code_pool_job_targets();

start transaction;

update sys_job
set job_name = '商城商品编码池每日补充',
    job_group = 'SYSTEM',
    invoke_target = 'productCodePoolTask.maintainPools',
    cron_expression = '0 59 22 * * ?',
    misfire_policy = '3',
    concurrent = '1',
    status = '0',
    update_by = 'admin',
    update_time = now(),
    remark = '每天22:59检查Redis中的SPU/SKU编码池容量，低于阈值时自动补充。'
where invoke_target = 'productCodePoolTask.maintainPools';

insert into sys_job (
    job_name,
    job_group,
    invoke_target,
    cron_expression,
    misfire_policy,
    concurrent,
    status,
    create_by,
    create_time,
    remark
)
select
    '商城商品编码池每日补充',
    'SYSTEM',
    'productCodePoolTask.maintainPools',
    '0 59 22 * * ?',
    '3',
    '1',
    '0',
    'admin',
    now(),
    '每天22:59检查Redis中的SPU/SKU编码池容量，低于阈值时自动补充。'
where not exists (
    select 1
    from sys_job
    where invoke_target = 'productCodePoolTask.maintainPools'
);

commit;

drop procedure if exists assert_product_code_pool_job_confirmed;
drop procedure if exists assert_product_code_pool_job_targets;
