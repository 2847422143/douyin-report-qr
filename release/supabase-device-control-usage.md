# Supabase 设备级控制版使用说明

## 文件

- 用户端 APK：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\outputs\douyin-report-qr-android-debug.apk`
- 管理端 APK：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\outputs\douyin-report-admin-android-debug.apk`

## 云端

Supabase 项目：

```text
https://wnaqpyoxpgzvcfupoloe.supabase.co
```

当前已使用的表：

- `devices`
- `device_configs`
- `global_config`

## 必须创建的删除设备函数

管理端的「删除设备记录」需要这个 Supabase RPC 函数。打开 Supabase 的 SQL Editor，先执行下面这整段 SQL。注意：必须从 `drop function` 开始整段执行，不要只执行后面的 `select public.admin_delete_device(...)` 测试语句。

```sql
drop function if exists public.admin_delete_device(text, text);

create or replace function public.admin_delete_device(
  target_device_id text,
  admin_secret text
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  deleted_devices integer := 0;
  deleted_configs integer := 0;
begin
  if admin_secret <> '73eb719f9818422d9ab28867d313be1a' then
    raise exception 'unauthorized';
  end if;

  delete from public.device_configs
  where device_id = target_device_id;
  get diagnostics deleted_configs = row_count;

  delete from public.devices
  where device_id = target_device_id;
  get diagnostics deleted_devices = row_count;

  return json_build_object(
    'ok', true,
    'device_id', target_device_id,
    'deleted_devices', deleted_devices,
    'deleted_configs', deleted_configs
  );
end;
$$;

grant execute on function public.admin_delete_device(text, text) to anon;
grant execute on function public.admin_delete_device(text, text) to authenticated;

notify pgrst, 'reload schema';
```

如果没有执行这段 SQL，管理端删除设备时会提示操作失败，或者设备记录不会真正从 `devices` 表消失。

如果管理端提示 `PGRST202`，说明 Supabase REST 还没有识别到这个函数。请重新执行上面完整 SQL，尤其要包含最后一行：

```sql
notify pgrst, 'reload schema';
```

执行后可以在 SQL Editor 里测试函数是否存在：

```sql
select public.admin_delete_device(
  'codex-delete-test'::text,
  '73eb719f9818422d9ab28867d313be1a'::text
);
```

如果返回 `{"ok": true, ...}`，说明函数已经生效。

如果测试时仍然提示 `function public.admin_delete_device(...) does not exist`，先执行下面 SQL 检查函数是否真的创建成功：

```sql
select
  n.nspname as schema_name,
  p.proname as function_name,
  pg_get_function_identity_arguments(p.oid) as arguments
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where p.proname = 'admin_delete_device';
```

如果查询结果为空，说明上面的创建函数 SQL 没有成功执行，需要重新执行完整创建 SQL。

## 用户端逻辑

用户端第一次打开会自动生成随机 `device_id`，并登记到 `devices` 表。

用户端每 5 秒刷新一次配置：

1. 先查 `device_configs` 里是否有本设备单独配置。
2. 如果有，就用单独配置。
3. 如果没有，就用 `global_config` 的全局配置。

## 管理端逻辑

管理端打开后会显示：

- 全局开关
- 全局提示语，下拉选择，不需要手动输入
- 已登记设备列表
- 一键全部暂停
- 一键全部开启
- 清空所有单独配置

点击某台设备，可以给这台设备设置：

- 是否允许生成
- 单独提示语，下拉选择，不需要手动输入

也可以删除单独配置，让它恢复使用全局配置。

如果某台手机已经不用了，也可以点击这台设备后选择「删除设备记录」。这个操作会从云端删除这台设备的登记记录和单独配置；如果这台手机以后重新打开用户端 APK，它会重新登记并再次出现在列表里。

批量按钮说明：

- 一键全部暂停：给所有已登记设备写入单独配置，全部暂停生成。
- 一键全部开启：给所有已登记设备写入单独配置，全部允许生成。
- 清空所有单独配置：删除所有设备的单独配置，让所有设备重新跟随全局开关。

## 测试步骤

1. 安装新版用户端 APK。
2. 打开用户端一次，等待几秒。
3. 安装并打开新版管理端 APK。
4. 点「刷新设备列表」。
5. 应该能看到刚打开过的手机。
6. 点这台设备，设置为暂停生成并保存。
7. 回到用户端，等待 5 秒左右。
8. 点击生成按钮，应显示对应维护提示。

当前可选提示语：

- 功能可用
- 服务维护中
- 系统升级中，请稍后再试
- 今日暂停生成
- 当前功能暂不可用
- 请稍后再试
