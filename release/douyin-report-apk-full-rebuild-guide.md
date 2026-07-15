# 抖音举报二维码双 APK 完整复刻说明

最后整理日期：2026-07-10  
项目类型：Android 原生 Java 双 APK + Supabase 云端控制  

## 1. 当前交付文件

当前已经生成好的 APK：

- 用户端 APK：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\outputs\douyin-report-qr-android-debug.apk`
- 管理端 APK：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\outputs\douyin-report-admin-android-debug.apk`

完整源码压缩包：

- `C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\outputs\douyin-report-apk-full-project-source.zip`

源码目录：

- 用户端源码：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\work\douyin-report-android`
- 管理端源码：`C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\work\douyin-report-admin-android`

## 2. 整体功能

### 用户端 APK

用户端用于生成抖音视频或图文作品的举报二维码。

核心功能：

- 输入抖音分享文本或完整链接。
- 自动提取 `video` 或 `note` 作品 ID。
- 自动生成抖音举报页 deeplink。
- 生成二维码。
- 支持复制 deeplink。
- 支持保存二维码图片。
- 支持直接尝试打开抖音举报页。
- 首次打开自动生成本机 `device_id`。
- 自动登记设备到 Supabase 的 `devices` 表。
- 每 5 秒读取云端开关。
- 如果云端禁止本设备生成，点击生成时提示维护/限制文案。

用户端包名：

```text
com.example.douyinreportqr
```

### 管理端 APK

管理端用于远程控制所有已经安装用户端的设备。

核心功能：

- 查看已经登记的设备列表。
- 全局开启或暂停生成。
- 设置全局提示语。
- 单独控制某台设备是否允许生成。
- 单独设置某台设备的提示语。
- 一键全部暂停。
- 一键全部开启。
- 清空所有单独配置，让全部设备跟随全局开关。
- 删除设备记录。
- 设备详情弹窗和提示语弹窗已做圆角背景、边框和自定义选项 UI。

管理端包名：

```text
com.example.douyinreportadmin
```

## 3. 当前云端配置

Supabase 项目：

```text
https://wnaqpyoxpgzvcfupoloe.supabase.co
```

REST API Base：

```text
https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1
```

Publishable key：

```text
sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5
```

管理端删除设备 RPC 口令：

```text
73eb719f9818422d9ab28867d313be1a
```

注意：如果以后重做项目，建议更换 Supabase 项目、publishable key 和 RPC 口令。

## 4. Android 项目结构

源码压缩包里包含两个项目：

```text
work/
  douyin-report-android/
    build.gradle
    settings.gradle
    local.properties
    app/
      build.gradle
      src/main/AndroidManifest.xml
      src/main/java/com/example/douyinreportqr/
        MainActivity.java
        AdminAuth.java
        IsoClock.java
        RemoteFeatureConfig.java
        ReportTypePolicy.java
        SimpleQr.java
      src/main/res/drawable/app_icon.xml
      src/main/res/values/strings.xml
      src/main/res/values/styles.xml

  douyin-report-admin-android/
    build.gradle
    settings.gradle
    local.properties
    gradle.properties
    app/
      build.gradle
      src/main/AndroidManifest.xml
      src/main/java/com/example/douyinreportadmin/
        MainActivity.java
        IsoClock.java
        JsonText.java
        SimpleTextWatcher.java
        GistConfig.java
        GistPayload.java
      src/main/res/drawable/app_icon.xml
      src/main/res/values/strings.xml
      src/main/res/values/styles.xml
```

说明：

- 用户端主逻辑集中在 `MainActivity.java`。
- 管理端主逻辑集中在 `MainActivity.java`。
- `GistConfig.java`、`GistPayload.java` 是早期 Gist 方案遗留文件，当前 Supabase 管理端主流程不依赖它们，可以保留也可以清理。
- 用户端依赖 ZXing 生成二维码。
- 管理端无第三方依赖。

## 5. Gradle 配置

用户端 `app/build.gradle`：

```gradle
plugins {
    id "com.android.application"
}

android {
    namespace "com.example.douyinreportqr"
    compileSdk 36

    defaultConfig {
        applicationId "com.example.douyinreportqr"
        minSdk 23
        targetSdk 36
        versionCode 1
        versionName "1.0"
    }
}

dependencies {
    implementation "com.google.zxing:core:3.5.3"
}
```

管理端 `app/build.gradle`：

```gradle
plugins {
    id "com.android.application"
}

android {
    namespace "com.example.douyinreportadmin"
    compileSdk 36

    defaultConfig {
        applicationId "com.example.douyinreportadmin"
        minSdk 23
        targetSdk 36
        versionCode 1
        versionName "1.0"
    }
}
```

本机当前使用：

```text
JDK: D:\JDK17
Android SDK: D:\AndroidSDK
Gradle: C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z\work\gradle-8.11.1\bin\gradle.bat
```

## 6. 构建命令

在 PowerShell 中执行。

### 构建用户端 APK

```powershell
$root = 'C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z'
$env:JAVA_HOME = 'D:\JDK17'
$env:Path = 'D:\JDK17\bin;' + $env:Path
& "$root\work\gradle-8.11.1\bin\gradle.bat" -p "$root\work\douyin-report-android" compileDebugJavaWithJavac assembleDebug
Copy-Item -Force "$root\work\douyin-report-android\app\build\outputs\apk\debug\app-debug.apk" "$root\outputs\douyin-report-qr-android-debug.apk"
```

### 构建管理端 APK

```powershell
$root = 'C:\Users\xuecheng.li\Documents\Codex\2026-06-30\ni-z'
$env:JAVA_HOME = 'D:\JDK17'
$env:Path = 'D:\JDK17\bin;' + $env:Path
& "$root\work\gradle-8.11.1\bin\gradle.bat" -p "$root\work\douyin-report-admin-android" compileDebugJavaWithJavac assembleDebug
Copy-Item -Force "$root\work\douyin-report-admin-android\app\build\outputs\apk\debug\app-debug.apk" "$root\outputs\douyin-report-admin-android-debug.apk"
```

## 7. Supabase 完整 SQL

打开 Supabase SQL Editor，按顺序执行下面 SQL。

### 7.1 建表和初始配置

```sql
create table if not exists public.devices (
  device_id text primary key,
  label text,
  app_version text,
  first_seen timestamptz not null default now(),
  last_seen timestamptz not null default now()
);

create table if not exists public.device_configs (
  device_id text primary key references public.devices(device_id) on delete cascade,
  enabled boolean not null default true,
  message text not null default '功能可用',
  updated_at timestamptz not null default now()
);

create table if not exists public.global_config (
  id integer primary key,
  enabled boolean not null default true,
  message text not null default '功能可用',
  updated_at timestamptz not null default now()
);

insert into public.global_config (id, enabled, message, updated_at)
values (1, true, '功能可用', now())
on conflict (id) do nothing;
```

### 7.2 RLS 和策略

当前 APK 使用 publishable key 直接访问 Supabase REST，所以需要给表开放对应权限。

```sql
alter table public.devices enable row level security;
alter table public.device_configs enable row level security;
alter table public.global_config enable row level security;

drop policy if exists devices_select_policy on public.devices;
drop policy if exists devices_insert_policy on public.devices;
drop policy if exists devices_update_policy on public.devices;

create policy devices_select_policy
on public.devices for select
to anon, authenticated
using (true);

create policy devices_insert_policy
on public.devices for insert
to anon, authenticated
with check (true);

create policy devices_update_policy
on public.devices for update
to anon, authenticated
using (true)
with check (true);

drop policy if exists device_configs_select_policy on public.device_configs;
drop policy if exists device_configs_insert_policy on public.device_configs;
drop policy if exists device_configs_update_policy on public.device_configs;
drop policy if exists device_configs_delete_policy on public.device_configs;

create policy device_configs_select_policy
on public.device_configs for select
to anon, authenticated
using (true);

create policy device_configs_insert_policy
on public.device_configs for insert
to anon, authenticated
with check (true);

create policy device_configs_update_policy
on public.device_configs for update
to anon, authenticated
using (true)
with check (true);

create policy device_configs_delete_policy
on public.device_configs for delete
to anon, authenticated
using (true);

drop policy if exists global_config_select_policy on public.global_config;
drop policy if exists global_config_update_policy on public.global_config;

create policy global_config_select_policy
on public.global_config for select
to anon, authenticated
using (true);

create policy global_config_update_policy
on public.global_config for update
to anon, authenticated
using (true)
with check (true);

grant usage on schema public to anon, authenticated;
grant select, insert, update on public.devices to anon, authenticated;
grant select, insert, update, delete on public.device_configs to anon, authenticated;
grant select, update on public.global_config to anon, authenticated;
```

### 7.3 删除设备 RPC

管理端删除设备记录时调用这个函数。直接 DELETE `devices` 表在部分策略下会返回 200 但删除 0 行，所以用 `security definer` 函数更稳。

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

测试：

```sql
select public.admin_delete_device(
  'codex-delete-test'::text,
  '73eb719f9818422d9ab28867d313be1a'::text
);
```

如果提示 `PGRST202`，重新执行上面的函数 SQL，并确认执行了：

```sql
notify pgrst, 'reload schema';
```

### 7.4 超过 10 台设备自动限制

规则：

- 第 1 到第 10 台：默认跟随全局开关。
- 第 11 台开始：自动写入单独配置。
- 单独配置为 `enabled = false`。
- 提示语为 `设备数量受限，暂不能使用`。
- 已经存在的设备不会自动补改，只对新插入 `devices` 的设备生效。

```sql
drop trigger if exists trg_limit_new_devices on public.devices;
drop function if exists public.limit_new_device_after_10();

create or replace function public.limit_new_device_after_10()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  device_count integer := 0;
begin
  select count(*) into device_count
  from public.devices;

  if device_count > 10 then
    insert into public.device_configs (
      device_id,
      enabled,
      message,
      updated_at
    )
    values (
      new.device_id,
      false,
      '设备数量受限，暂不能使用',
      now()
    )
    on conflict (device_id) do update
    set
      enabled = false,
      message = '设备数量受限，暂不能使用',
      updated_at = now();
  end if;

  return new;
end;
$$;

create trigger trg_limit_new_devices
after insert on public.devices
for each row
execute function public.limit_new_device_after_10();

notify pgrst, 'reload schema';
```

## 8. 用户端关键逻辑

用户端核心常量位于：

```text
work\douyin-report-android\app\src\main\java\com\example\douyinreportqr\MainActivity.java
```

关键常量：

```java
private static final String SUPABASE_REST_URL = "https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1";
private static final String SUPABASE_KEY = "sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5";
private static final long FEATURE_REFRESH_MS = 5000L;
private static final String REPORT_MORTISE_ID = "9879902d-907f-46f3-85e0-678c416f366a";
private static final String REPORT_H5_BASE = "https://api.amemv.com/falcon/fe_douyin_security_react/mortise/" + REPORT_MORTISE_ID + "/";
```

举报 deeplink 基本格式：

```text
snssdk1128://webview?...&url=https%3A%2F%2Fapi.amemv.com%2Ffalcon%2Ffe_douyin_security_react%2Fmortise%2F{mortise_id}%2F%3Freport_type%3D{video|note}%26object_id%3D{作品ID}%26enter_from%3Daweme_reflow%26hide_nav_bar%3D1%26should_full_screen%3D1...
```

用户端远程配置读取顺序：

1. 读取 `/device_configs?device_id=eq.{device_id}&select=enabled,message`。
2. 如果有单独配置，优先使用。
3. 如果没有单独配置，读取 `/global_config?id=eq.1&select=enabled,message`。
4. 如果 `enabled=false`，点击生成时只提示 message，不生成二维码。

## 9. 管理端关键逻辑

管理端核心文件：

```text
work\douyin-report-admin-android\app\src\main\java\com\example\douyinreportadmin\MainActivity.java
```

关键常量：

```java
private static final String SUPABASE_REST_URL = "https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1";
private static final String SUPABASE_KEY = "sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5";
private static final String ADMIN_DELETE_SECRET = "73eb719f9818422d9ab28867d313be1a";
```

可选提示语：

```java
private static final String[] MESSAGE_OPTIONS = new String[]{
        "功能可用",
        "服务维护中",
        "系统升级中，请稍后再试",
        "今日暂停生成",
        "当前功能暂不可用",
        "请稍后再试"
};
```

管理端接口行为：

- 读取全局配置：`GET /global_config?id=eq.1&select=enabled,message`
- 保存全局配置：`PATCH /global_config?id=eq.1`
- 读取设备：`GET /devices?select=device_id,label,app_version,first_seen,last_seen&order=last_seen.desc`
- 读取单独配置：`GET /device_configs?select=device_id,enabled,message`
- 保存单独配置：`POST /device_configs?on_conflict=device_id`
- 删除单独配置：`DELETE /device_configs?device_id=eq.{device_id}`
- 删除设备记录：`POST /rpc/admin_delete_device`

## 10. 测试流程

1. Supabase SQL 全部执行成功。
2. 安装用户端 APK。
3. 打开用户端一次，等待 5 秒。
4. 安装管理端 APK。
5. 打开管理端，点击刷新设备列表。
6. 应看到新设备。
7. 在管理端关闭全局开关，保存。
8. 回到用户端，等待约 5 秒，点击生成，应提示维护文案。
9. 在管理端点击某台设备，单独允许生成。
10. 回到用户端，等待约 5 秒，再点击生成，应恢复生成二维码。
11. 测试第 11 台设备时，应自动被写入 `device_configs`，提示 `设备数量受限，暂不能使用`。

## 11. 给大模型的完整复刻提示词

以后可以把下面这段提示词和 `douyin-report-apk-full-project-source.zip` 一起给大模型：

```text
你是一名 Android 原生 Java 工程师。请基于我提供的源码包，复刻并维护一个双 APK 项目：

1. 用户端 APK
- 包名：com.example.douyinreportqr
- Java 原生 Android。
- 使用 ZXing core 3.5.3 生成二维码。
- 输入抖音分享文本或 URL。
- 自动识别 douyin.com/video/{id} 和 douyin.com/note/{id}，也要尽量处理 v.douyin.com 短链跳转后的真实链接。
- 根据作品类型生成抖音举报页 deeplink。
- video 使用 report_type=video。
- note/images 使用 report_type=note。
- 显示二维码，支持保存图片、复制 deeplink、尝试打开抖音。
- 首次打开生成 UUID 作为 device_id，保存到 SharedPreferences。
- 调用 Supabase REST 登记设备到 devices 表。
- 每 5 秒读取远程配置，优先读 device_configs，没有单独配置再读 global_config。
- 如果 enabled=false，点击生成时提示 message，不生成二维码。

2. 管理端 APK
- 包名：com.example.douyinreportadmin
- Java 原生 Android。
- 连接同一个 Supabase 项目。
- 显示全局开关、全局提示语、设备列表。
- 提示语用固定选项，不允许手写。
- 设备详情弹窗要美观，有圆角背景、边框、设备信息区、删除设备记录按钮。
- 选择提示语弹窗要自定义圆角选项，选中项绿色边框和浅绿色背景。
- 支持保存全局配置。
- 支持单台设备开启/暂停。
- 支持删除单台设备的单独配置。
- 支持一键全部暂停、一键全部开启、清空所有单独配置。
- 支持删除设备记录，删除时调用 Supabase RPC：/rpc/admin_delete_device。

3. Supabase
- 使用三张表：devices、device_configs、global_config。
- devices 字段：device_id, label, app_version, first_seen, last_seen。
- device_configs 字段：device_id, enabled, message, updated_at。
- global_config 字段：id, enabled, message, updated_at。
- 创建 admin_delete_device(target_device_id text, admin_secret text) security definer 函数。
- 创建 limit_new_device_after_10() trigger，第 11 台设备开始自动写入 enabled=false, message='设备数量受限，暂不能使用'。

4. 构建
- compileSdk 36, minSdk 23, targetSdk 36。
- 输出两个 debug APK：
  - douyin-report-qr-android-debug.apk
  - douyin-report-admin-android-debug.apk

请先读取源码包里的两个 Android 项目，保持现有功能和 UI，再根据需要修改。不要用 Flutter 或 React Native，不要重写成网页。
```

## 12. 注意事项

- APK 只能用于 Android，iPhone 不能安装 APK。
- 抖音 deeplink 和举报 H5 地址属于外部平台行为，抖音未来改规则后可能需要更新。
- Supabase publishable key 暴露在 APK 内是正常的前端访问方式，但管理端删除口令写在 APK 内并不适合作为高安全方案；如果要商业化，应改成 Edge Function + 登录鉴权。
- 设备数量限制是对新插入设备生效，已有设备不会自动按数量重新排序或限制。
- 管理端手动开启受限设备后，只要这台设备不重新生成新的 `device_id`，触发器不会再次把它改回受限。
