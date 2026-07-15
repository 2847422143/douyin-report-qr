package com.example.douyinreportqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String PREFS = "douyin_report_admin";
    private static final String KEY_ADMIN_HASH = "admin_hash";
    private static final String KEY_LOCAL_ENABLED = "local_enabled";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String DEFAULT_ADMIN_ACCOUNT = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final String SUPABASE_REST_URL = "https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1";
    private static final String SUPABASE_KEY = "sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5";
    private static final long FEATURE_REFRESH_MS = 5000L;
    private static final String REPORT_MORTISE_ID = "9879902d-907f-46f3-85e0-678c416f366a";
    private static final String REPORT_H5_BASE = "https://api.amemv.com/falcon/fe_douyin_security_react/mortise/" + REPORT_MORTISE_ID + "/";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s，。]+");
    private static final Pattern OBJECT_PATTERN = Pattern.compile("(?:douyin\\.com/(video|note)/|iesdouyin\\.com/share/(video|note)/)(\\d{16,22})");
    private static final Pattern SEC_UID_PATTERN = Pattern.compile("\"sec_uid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern QUERY_VALUE_PATTERN = Pattern.compile("[?&]([a-zA-Z0-9_]+)=([^&]*)");

    private EditText input;
    private RadioGroup typeGroup;
    private TextView statusView;
    private TextView objectView;
    private TextView ownerView;
    private TextView linkView;
    private ImageView qrView;
    private Button generateButton;
    private Button copyButton;
    private Button saveButton;
    private Button openButton;
    private Bitmap currentQr;
    private String currentDeepLink = "";
    private SharedPreferences prefs;
    private TextView gateView;
    private RemoteFeatureConfig featureConfig = RemoteFeatureConfig.localDefault();
    private int adminTapCount = 0;
    private final Handler featureHandler = new Handler(Looper.getMainLooper());
    private final Runnable featureRefreshTask = new Runnable() {
        @Override
        public void run() {
            refreshFeatureGate(false);
            featureHandler.postDelayed(this, FEATURE_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureAdminDefaults();
        setContentView(buildUi());
        refreshFeatureGate(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        featureHandler.removeCallbacks(featureRefreshTask);
        featureHandler.post(featureRefreshTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        featureHandler.removeCallbacks(featureRefreshTask);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(0), dp(0), dp(0), dp(28));
        root.setBackgroundColor(rgb(246, 247, 249));
        scroll.addView(root);

        root.addView(topBar());

        LinearLayout inputCard = card();
        root.addView(inputCard, pageCardParams(14));

        inputCard.addView(label("作品链接或分享文案", 15, true, rgb(23, 32, 42)));
        input = new EditText(this);
        input.setMinLines(5);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("粘贴抖音分享文案、短链、完整链接或作品 ID");
        input.setTextColor(rgb(23, 32, 42));
        input.setTextSize(14);
        input.setBackground(box(Color.WHITE, rgb(217, 224, 232), 8));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        inputCard.addView(input, matchWrapTop(8));

        TextView typeTitle = label("作品类型", 15, true, rgb(23, 32, 42));
        typeTitle.setPadding(0, dp(14), 0, 0);
        inputCard.addView(typeTitle);

        typeGroup = new RadioGroup(this);
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        typeGroup.addView(radio("自动", 1001, true));
        typeGroup.addView(radio("视频", 1002, false));
        typeGroup.addView(radio("图文/图集", 1003, false));
        inputCard.addView(typeGroup, matchWrapTop(4));

        generateButton = primaryButton("生成举报二维码");
        generateButton.setOnClickListener(v -> generate());
        inputCard.addView(generateButton, matchWrapTop(12));

        statusView = label("等待输入", 13, false, rgb(95, 107, 122));
        statusView.setPadding(0, dp(12), 0, 0);
        inputCard.addView(statusView);

        gateView = label("本机模式：功能可用", 13, false, rgb(15, 118, 110));
        gateView.setPadding(0, dp(8), 0, 0);
        inputCard.addView(gateView);

        LinearLayout resultCard = card();
        root.addView(resultCard, pageCardParams(14));

        TextView resultTitle = label("生成结果", 18, true, rgb(23, 32, 42));
        resultCard.addView(resultTitle);

        qrView = new ImageView(this);
        qrView.setBackground(box(Color.WHITE, rgb(217, 224, 232), 8));
        qrView.setPadding(dp(12), dp(12), dp(12), dp(12));
        qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        resultCard.addView(qrView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(330)));

        objectView = infoLine("作品：-");
        ownerView = infoLine("作者加密 ID：-");
        resultCard.addView(objectView);
        resultCard.addView(ownerView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        copyButton = secondaryButton("复制举报链接");
        saveButton = secondaryButton("保存二维码到相册");
        openButton = primaryButton("打开抖音举报页");
        copyButton.setOnClickListener(v -> copyDeepLink());
        saveButton.setOnClickListener(v -> saveQr());
        openButton.setOnClickListener(v -> openDouyinReport());
        actions.addView(openButton, matchWrapTop(10));
        actions.addView(copyButton, matchWrapTop(8));
        actions.addView(saveButton, matchWrapTop(8));
        resultCard.addView(actions);

        linkView = label("举报链接生成后显示在这里", 12, false, rgb(95, 107, 122));
        linkView.setTextIsSelectable(true);
        linkView.setPadding(0, dp(12), 0, 0);
        resultCard.addView(linkView);

        setActionsEnabled(false);
        return scroll;
    }

    private void generate() {
        if (!featureConfig.enabled) {
            show("服务维护中：" + featureConfig.message);
            return;
        }
        String source = input.getText().toString().trim();
        if (source.isEmpty()) {
            show("请先粘贴抖音链接或分享文案");
            return;
        }
        setBusy(true, "正在检查功能状态...");
        new Thread(() -> {
            try {
                RemoteFeatureConfig config = loadFeatureConfig();
                if (!config.enabled) {
                    runOnUiThread(() -> applyFeatureConfig(config, "服务维护中：" + config.message));
                    return;
                }
                runOnUiThread(() -> applyFeatureConfig(config, "正在解析作品..."));
                WorkInfo info = resolveWorkInfo(source);
                String override = selectedTypeOverride();
                if (override != null) info.objectType = override;
                String deepLink = buildReportDeepLink(info);
                Bitmap qr = SimpleQr.encode(deepLink, 960);
                runOnUiThread(() -> showResult(info, deepLink, qr));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false, "解析失败：" + e.getMessage());
                    setActionsEnabled(false);
                });
            }
        }).start();
    }

    private void showResult(WorkInfo info, String deepLink, Bitmap qr) {
        currentDeepLink = deepLink;
        currentQr = qr;
        qrView.setImageBitmap(qr);
        objectView.setText("作品：" + displayType(info.objectType) + " / " + info.objectId);
        ownerView.setText(info.secOwnerId.isEmpty() ? "作者加密 ID：未获取到" : "作者加密 ID：已带入");
        linkView.setText(deepLink);
        setBusy(false, "已生成。请先扫码验证，或点击打开抖音举报页。");
        setActionsEnabled(true);
    }

    private WorkInfo resolveWorkInfo(String source) throws Exception {
        WorkInfo direct = extractObject(source);
        String firstUrl = extractFirstUrl(source);
        if (direct != null && firstUrl == null) return direct;
        if (firstUrl == null) throw new Exception("没有找到链接");

        HttpResult result = fetch(firstUrl);
        WorkInfo info = direct != null ? direct : extractObject(result.finalUrl);
        if (info == null) info = extractObject(result.body);
        if (info == null) throw new Exception("没有找到 video 或 note ID");

        info.resolvedUrl = result.finalUrl;
        info.outerParams = extractQueryValues(result.finalUrl);
        Matcher secMatcher = SEC_UID_PATTERN.matcher(result.body);
        if (secMatcher.find()) info.secOwnerId = secMatcher.group(1);
        return info;
    }

    private HttpResult fetch(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        try (InputStream stream = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
            return new HttpResult(connection.getURL().toString(), out.toString(StandardCharsets.UTF_8.name()));
        }
    }

    private WorkInfo extractObject(String text) {
        String value = text == null ? "" : text.trim();
        if (value.matches("^\\d{16,22}$")) return new WorkInfo("video", value);
        Matcher matcher = OBJECT_PATTERN.matcher(value);
        if (!matcher.find()) return null;
        String type = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return new WorkInfo(type, matcher.group(3));
    }

    private String extractFirstUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) return null;
        return matcher.group().replaceAll("[ :;,.，。]+$", "");
    }

    private List<String[]> extractQueryValues(String url) {
        List<String[]> values = new ArrayList<>();
        Matcher matcher = QUERY_VALUE_PATTERN.matcher(url == null ? "" : url);
        while (matcher.find()) values.add(new String[]{matcher.group(1), matcher.group(2)});
        return values;
    }

    private String selectedTypeOverride() {
        int id = typeGroup.getCheckedRadioButtonId();
        if (id == 1002) return "video";
        if (id == 1003) return "images";
        return null;
    }

    private String displayType(String objectType) {
        if ("note".equals(objectType) || "images".equals(objectType)) return "图文/图集";
        return "视频";
    }

    private String buildReportDeepLink(WorkInfo info) throws Exception {
        List<String[]> reportParams = new ArrayList<>();
        reportParams.add(new String[]{"report_type", ReportTypePolicy.toReportType(info.objectType)});
        reportParams.add(new String[]{"object_id", info.objectId});
        if (!info.secOwnerId.isEmpty()) reportParams.add(new String[]{"sec_owner_id", info.secOwnerId});
        reportParams.add(new String[]{"enter_from", "aweme_reflow"});
        reportParams.add(new String[]{"hide_nav_bar", "1"});
        reportParams.add(new String[]{"should_full_screen", "1"});
        String reportH5 = REPORT_H5_BASE + "?" + formEncode(reportParams);

        List<String[]> outer = new ArrayList<>();
        addIfPresent(outer, info, "video_share_track_ver");
        addIfPresent(outer, info, "did");
        addIfPresent(outer, info, "mid");
        addIfPresent(outer, info, "ts");
        addIfPresent(outer, info, "share_track_info");
        addIfPresent(outer, info, "region");
        addIfPresent(outer, info, "share_sign");
        addIfPresent(outer, info, "tt_from");
        addIfPresent(outer, info, "with_sec_did");
        outer.add(new String[]{"refer", "web"});
        outer.add(new String[]{"from", "webview"});
        outer.add(new String[]{"from_ssr", "1"});
        outer.add(new String[]{"from_aid", "1128"});
        addIfPresent(outer, info, "titleType");
        addIfPresent(outer, info, "utm_source");
        addIfPresent(outer, info, "utm_medium");
        addIfPresent(outer, info, "activity_info");
        addIfPresent(outer, info, "timestamp");
        addIfPresent(outer, info, "share_version");
        addIfPresent(outer, info, "ug_share_id");
        addIfPresent(outer, info, "u_code");
        addIfPresent(outer, info, "iid");
        addIfPresent(outer, info, "utm_campaign");
        outer.add(new String[]{"app", "aweme"});
        outer.add(new String[]{"scene_from", "share_reflow"});
        outer.add(new String[]{"host", "www.iesdouyin.com"});
        outer.add(new String[]{"group_id", info.objectId});
        outer.add(new String[]{"browser_name", "safari"});
        outer.add(new String[]{"is_edenx", "1"});
        outer.add(new String[]{"forbid_pasteboard", "1"});
        outer.add(new String[]{"gd_label", "click_schema_ug_filter_v1_click_schema_lhft_48148317a"});
        outer.add(new String[]{"launch_h5_method", "click_wap_rf_video_report"});
        outer.add(new String[]{"url", reportH5});
        outer.add(new String[]{"hide_nav_bar", "1"});
        outer.add(new String[]{"should_full_screen", "1"});
        outer.add(new String[]{"enter_from", "aweme_reflow"});
        return "snssdk1128://webview?" + formEncode(outer);
    }

    private void addIfPresent(List<String[]> params, WorkInfo info, String key) {
        String value = info.queryValue(key);
        if (value != null) params.add(new String[]{key, value});
    }

    private String formEncode(List<String[]> params) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) builder.append("&");
            builder.append(URLEncoder.encode(params.get(i)[0], "UTF-8"));
            builder.append("=");
            builder.append(URLEncoder.encode(params.get(i)[1], "UTF-8").replace("+", "%20"));
        }
        return builder.toString();
    }

    private void copyDeepLink() {
        if (currentDeepLink.isEmpty()) {
            show("请先生成二维码");
            return;
        }
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("douyin report link", currentDeepLink));
        show("已复制举报链接");
    }

    private void saveQr() {
        if (currentQr == null) {
            show("请先生成二维码");
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "douyin-report-qr-" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("无法创建图片");
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                currentQr.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }
            show("已保存到相册");
        } catch (Exception e) {
            show("保存失败：" + e.getMessage());
        }
    }

    private void openDouyinReport() {
        if (currentDeepLink.isEmpty()) {
            show("请先生成二维码");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentDeepLink));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            show("无法打开抖音。请复制链接或保存二维码后再试。");
        }
    }

    private void ensureAdminDefaults() {
        if (!prefs.contains(KEY_ADMIN_HASH)) {
            prefs.edit()
                    .putString(KEY_ADMIN_HASH, AdminAuth.hash(DEFAULT_ADMIN_ACCOUNT, DEFAULT_ADMIN_PASSWORD))
                    .putBoolean(KEY_LOCAL_ENABLED, true)
                    .apply();
        }
    }

    private void refreshFeatureGate(boolean showStatus) {
        new Thread(() -> {
            RemoteFeatureConfig config = loadFeatureConfig();
            runOnUiThread(() -> applyFeatureConfig(config, showStatus ? config.message : statusView.getText().toString()));
        }).start();
    }

    private RemoteFeatureConfig loadFeatureConfig() {
        if (!prefs.getBoolean(KEY_LOCAL_ENABLED, true)) {
            return RemoteFeatureConfig.unavailable("本机管理员已暂停生成");
        }
        try {
            String deviceId = deviceId();
            registerDevice(deviceId);
            RemoteFeatureConfig specific = loadDeviceSpecificConfig(deviceId);
            return specific != null ? specific : loadGlobalConfig();
        } catch (Exception e) {
            return RemoteFeatureConfig.unavailable("远程配置不可用，请稍后再试");
        }
    }

    private String deviceId() {
        String value = prefs.getString(KEY_DEVICE_ID, "");
        if (value.isEmpty()) {
            value = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, value).apply();
        }
        return value;
    }

    private void registerDevice(String deviceId) throws Exception {
        String label = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        String body = "{"
                + "\"device_id\":\"" + jsonEscape(deviceId) + "\","
                + "\"label\":\"" + jsonEscape(label) + "\","
                + "\"app_version\":\"1.0\","
                + "\"last_seen\":\"" + jsonEscape(IsoClock.now()) + "\""
                + "}";
        supabaseRequest("POST", "/devices?on_conflict=device_id", body, "resolution=merge-duplicates,return=minimal");
    }

    private RemoteFeatureConfig loadDeviceSpecificConfig(String deviceId) throws Exception {
        String body = supabaseRequest("GET", "/device_configs?device_id=eq." + urlEncode(deviceId) + "&select=enabled,message", null, null).body;
        if (body == null || body.trim().equals("[]")) return null;
        return RemoteFeatureConfig.parse(body);
    }

    private RemoteFeatureConfig loadGlobalConfig() throws Exception {
        String body = supabaseRequest("GET", "/global_config?id=eq.1&select=enabled,message", null, null).body;
        return RemoteFeatureConfig.parse(body);
    }

    private HttpResult supabaseRequest(String method, String path, String body, String prefer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SUPABASE_REST_URL + path).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("apikey", SUPABASE_KEY);
        connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
        connection.setRequestProperty("Accept", "application/json");
        if (prefer != null) connection.setRequestProperty("Prefer", prefer);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String responseBody = readAll(stream);
        if (code < 200 || code >= 300) throw new Exception(responseBody);
        return new HttpResult(connection.getURL().toString(), responseBody);
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void applyFeatureConfig(RemoteFeatureConfig config, String message) {
        featureConfig = config;
        generateButton.setEnabled(true);
        gateView.setText(config.enabled ? "功能状态：" + config.message : "服务维护中：" + config.message);
        gateView.setTextColor(config.enabled ? rgb(15, 118, 110) : rgb(190, 24, 93));
        statusView.setText(message);
        if (!config.enabled) setActionsEnabled(false);
    }

    private void setBusy(boolean busy, String message) {
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "解析中..." : "生成举报二维码");
        statusView.setText(message);
    }

    private void setActionsEnabled(boolean enabled) {
        copyButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        openButton.setEnabled(enabled);
    }

    private void show(String message) {
        statusView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleAdminTap() {
        adminTapCount++;
        if (adminTapCount >= 10) {
            adminTapCount = 0;
            showAdminLoginDialog();
        }
    }

    private void showAdminLoginDialog() {
        LinearLayout form = dialogForm();
        EditText account = dialogInput("管理员账号");
        EditText password = dialogInput("管理员密码");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(account);
        form.addView(password, matchWrapTop(10));

        new AlertDialog.Builder(this)
                .setTitle("管理员入口")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("登录", (dialog, which) -> {
                    String savedHash = prefs.getString(KEY_ADMIN_HASH, "");
                    if (AdminAuth.verify(account.getText().toString().trim(), password.getText().toString(), savedHash)) {
                        showAdminPanelDialog();
                    } else {
                        show("管理员账号或密码不正确");
                    }
                })
                .show();
    }

    private void showAdminPanelDialog() {
        LinearLayout form = dialogForm();
        CheckBox localEnabled = new CheckBox(this);
        localEnabled.setText("允许本机生成举报二维码");
        localEnabled.setTextSize(14);
        localEnabled.setChecked(prefs.getBoolean(KEY_LOCAL_ENABLED, true));
        form.addView(localEnabled);

        TextView help = label("远程配置地址已内置，用户端不可修改。", 12, false, rgb(95, 107, 122));
        help.setPadding(0, dp(8), 0, 0);
        form.addView(help);

        Button passwordButton = secondaryButton("修改管理员密码");
        passwordButton.setOnClickListener(v -> showChangePasswordDialog());
        form.addView(passwordButton, matchWrapTop(12));

        new AlertDialog.Builder(this)
                .setTitle("管理员设置")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    prefs.edit()
                            .putBoolean(KEY_LOCAL_ENABLED, localEnabled.isChecked())
                            .apply();
                    show("管理员设置已保存");
                    refreshFeatureGate(true);
                })
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout form = dialogForm();
        EditText account = dialogInput("新管理员账号");
        EditText password = dialogInput("新管理员密码");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(account);
        form.addView(password, matchWrapTop(10));

        new AlertDialog.Builder(this)
                .setTitle("修改管理员密码")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newAccount = account.getText().toString().trim();
                    String newPassword = password.getText().toString();
                    if (newAccount.isEmpty() || newPassword.length() < 6) {
                        show("账号不能为空，密码至少 6 位");
                        return;
                    }
                    prefs.edit().putString(KEY_ADMIN_HASH, AdminAuth.hash(newAccount, newPassword)).apply();
                    show("管理员密码已修改");
                })
                .show();
    }

    private LinearLayout topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(22), dp(18), dp(18));
        bar.setBackgroundColor(rgb(17, 24, 39));

        TextView icon = label("举", 20, true, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(box(rgb(20, 184, 166), rgb(20, 184, 166), 14));
        icon.setOnClickListener(v -> handleAdminTap());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        bar.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        TextView title = label("举报二维码", 22, true, Color.WHITE);
        TextView subtitle = label("视频走视频举报，图文走图集举报", 13, false, rgb(209, 213, 219));
        copy.addView(title);
        copy.addView(subtitle);
        bar.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return bar;
    }

    private LinearLayout.LayoutParams pageCardParams(int topDp) {
        LinearLayout.LayoutParams params = matchWrapTop(topDp);
        params.leftMargin = dp(18);
        params.rightMargin = dp(18);
        return params;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(box(Color.WHITE, rgb(217, 224, 232), 10));
        return layout;
    }

    private LinearLayout dialogForm() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(4), dp(8), dp(4), 0);
        return layout;
    }

    private EditText dialogInput(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(14);
        view.setSingleLine(true);
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        view.setBackground(box(Color.WHITE, rgb(217, 224, 232), 8));
        return view;
    }

    private TextView infoLine(String text) {
        TextView view = label(text, 13, false, rgb(95, 107, 122));
        view.setPadding(0, dp(10), 0, 0);
        return view;
    }

    private TextView label(String text, int sp, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackground(box(rgb(15, 118, 110), rgb(15, 118, 110), 8));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(rgb(23, 32, 42));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setBackground(box(Color.WHITE, rgb(217, 224, 232), 8));
        return button;
    }

    private RadioButton radio(String text, int id, boolean checked) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(text);
        button.setTextColor(rgb(23, 32, 42));
        button.setTextSize(14);
        button.setChecked(checked);
        return button;
    }

    private GradientDrawable box(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int rgb(int r, int g, int b) {
        return Color.rgb(r, g, b);
    }

    private static class WorkInfo {
        String objectType;
        final String objectId;
        String secOwnerId = "";
        String resolvedUrl = "";
        List<String[]> outerParams = new ArrayList<>();

        WorkInfo(String objectType, String objectId) {
            this.objectType = objectType;
            this.objectId = objectId;
        }

        String queryValue(String key) {
            for (String[] pair : outerParams) {
                if (pair[0].equals(key)) return pair[1];
            }
            return null;
        }
    }

    private static class HttpResult {
        final String finalUrl;
        final String body;

        HttpResult(String finalUrl, String body) {
            this.finalUrl = finalUrl;
            this.body = body;
        }
    }
}
