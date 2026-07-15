package com.example.douyinreportadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String SUPABASE_REST_URL = "https://wnaqpyoxpgzvcfupoloe.supabase.co/rest/v1";
    private static final String SUPABASE_KEY = "sb_publishable_QqLauvy5EGcEPCkEnYqL0Q_gL2YZAZ5";
    private static final String ADMIN_DELETE_SECRET = "73eb719f9818422d9ab28867d313be1a";
    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{([^{}]*)\\}");
    private static final String DEFAULT_MESSAGE = "功能可用";
    private static final String[] MESSAGE_OPTIONS = new String[]{
            "功能可用",
            "服务维护中",
            "系统升级中，请稍后再试",
            "今日暂停生成",
            "当前功能暂不可用",
            "请稍后再试"
    };

    private TextView statusView;
    private CheckBox globalEnabled;
    private MessageSelector globalMessage;
    private LinearLayout deviceList;
    private Button refreshButton;
    private Button saveGlobalButton;
    private Button pauseAllButton;
    private Button enableAllButton;
    private Button clearAllButton;
    private final List<DeviceInfo> devices = new ArrayList<>();
    private volatile Config currentGlobalConfig = defaultConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refreshAll();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(rgb(245, 247, 250));
        root.setPadding(0, 0, 0, dp(28));
        scroll.addView(root);

        root.addView(topBar());

        LinearLayout globalCard = card();
        root.addView(globalCard, pageCardParams(16));
        globalCard.addView(label("全局开关", 18, true, rgb(23, 32, 42)));
        globalEnabled = new CheckBox(this);
        globalEnabled.setText("允许默认生成");
        globalEnabled.setTextSize(15);
        globalEnabled.setTextColor(rgb(23, 32, 42));
        globalCard.addView(globalEnabled, matchWrapTop(8));
        globalCard.addView(label("全局提示语", 14, true, rgb(23, 32, 42)), matchWrapTop(10));
        globalMessage = messageSelector(DEFAULT_MESSAGE);
        globalCard.addView(globalMessage, matchWrapTop(6));
        saveGlobalButton = primaryButton("保存全局开关");
        saveGlobalButton.setOnClickListener(v -> {
            Config chosen = readGlobalSelection();
            runAsync("正在保存全局配置...", () -> {
                saveGlobalConfig(chosen);
                Config loaded = loadGlobalConfig();
                currentGlobalConfig = loaded;
                runOnUiThread(() -> applyGlobalConfig(loaded));
                return "已保存并读取全局线上配置。";
            });
        });
        globalCard.addView(saveGlobalButton, matchWrapTop(12));

        pauseAllButton = secondaryButton("一键全部暂停");
        pauseAllButton.setOnClickListener(v -> confirmBulk("一键全部暂停", "确定把所有已登记设备都设置为暂停生成吗？", () -> bulkSetAll(false, globalMessage.getMessage())));
        globalCard.addView(pauseAllButton, matchWrapTop(10));

        enableAllButton = secondaryButton("一键全部开启");
        enableAllButton.setOnClickListener(v -> confirmBulk("一键全部开启", "确定把所有已登记设备都设置为允许生成吗？", () -> bulkSetAll(true, DEFAULT_MESSAGE)));
        globalCard.addView(enableAllButton, matchWrapTop(8));

        clearAllButton = secondaryButton("清空所有单独配置");
        clearAllButton.setOnClickListener(v -> confirmBulk("清空所有单独配置", "确定让所有设备重新跟随全局开关吗？", this::clearAllSpecificConfigs));
        globalCard.addView(clearAllButton, matchWrapTop(8));

        LinearLayout devicesCard = card();
        root.addView(devicesCard, pageCardParams(14));
        devicesCard.addView(label("已登记设备", 18, true, rgb(23, 32, 42)));
        refreshButton = secondaryButton("刷新设备列表");
        refreshButton.setOnClickListener(v -> refreshAll());
        devicesCard.addView(refreshButton, matchWrapTop(10));
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        devicesCard.addView(deviceList, matchWrapTop(10));

        statusView = label("等待读取。", 13, false, rgb(95, 107, 122));
        statusView.setPadding(dp(18), dp(14), dp(18), 0);
        root.addView(statusView);
        return scroll;
    }

    private void refreshAll() {
        runAsync("正在读取线上设备和配置...", () -> {
            Config loaded = loadGlobalConfig();
            currentGlobalConfig = loaded;
            runOnUiThread(() -> applyGlobalConfig(loaded));
            devices.clear();
            devices.addAll(loadDevices(loaded));
            return "已读取线上状态，共 " + devices.size() + " 台设备。";
        });
    }

    private void runAsync(String startMessage, CheckedAction action) {
        setBusy(true, startMessage);
        new Thread(() -> {
            try {
                String message = action.run();
                runOnUiThread(() -> {
                    renderDevices();
                    setBusy(false, message);
                });
            } catch (Exception e) {
                runOnUiThread(() -> setBusy(false, "操作失败：" + e.getMessage()));
            }
        }).start();
    }

    private Config loadGlobalConfig() throws Exception {
        String body = supabaseRequest("GET", "/global_config?id=eq.1&select=enabled,message", null, null);
        return parseConfig(firstObject(body), true, DEFAULT_MESSAGE);
    }

    private void applyGlobalConfig(Config config) {
        globalEnabled.setChecked(config.enabled);
        globalMessage.setMessage(config.message);
    }

    private List<DeviceInfo> loadDevices(Config fallback) throws Exception {
        String devicesBody = supabaseRequest("GET", "/devices?select=device_id,label,app_version,first_seen,last_seen&order=last_seen.desc", null, null);
        String configsBody = supabaseRequest("GET", "/device_configs?select=device_id,enabled,message", null, null);
        List<DeviceInfo> result = parseDevices(devicesBody);
        for (DeviceInfo device : result) {
            Config config = findDeviceConfig(configsBody, device.deviceId);
            device.hasSpecificConfig = config != null;
            if (config != null) {
                device.enabled = config.enabled;
                device.message = config.message;
            } else {
                device.enabled = fallback.enabled;
                device.message = fallback.message;
            }
        }
        return result;
    }

    private void saveGlobalConfig(Config config) throws Exception {
        String body = "{\"enabled\":" + config.enabled
                + ",\"message\":\"" + jsonEscape(config.message) + "\""
                + ",\"updated_at\":\"" + IsoClock.now() + "\"}";
        supabaseRequest("PATCH", "/global_config?id=eq.1", body, "return=minimal");
    }

    private void saveDeviceConfig(DeviceInfo device, boolean enabled, String message) {
        runAsync("正在保存设备配置...", () -> {
            String body = "{\"device_id\":\"" + jsonEscape(device.deviceId) + "\","
                    + "\"enabled\":" + enabled + ","
                    + "\"message\":\"" + jsonEscape(message) + "\","
                    + "\"updated_at\":\"" + IsoClock.now() + "\"}";
            supabaseRequest("POST", "/device_configs?on_conflict=device_id", body, "resolution=merge-duplicates,return=minimal");
            Config loaded = loadGlobalConfig();
            currentGlobalConfig = loaded;
            devices.clear();
            devices.addAll(loadDevices(loaded));
            return "已保存设备配置：" + shortId(device.deviceId);
        });
    }

    private void deleteDeviceConfig(DeviceInfo device) {
        runAsync("正在删除设备单独配置...", () -> {
            supabaseRequest("DELETE", "/device_configs?device_id=eq." + urlEncode(device.deviceId), null, "return=minimal");
            Config loaded = loadGlobalConfig();
            currentGlobalConfig = loaded;
            devices.clear();
            devices.addAll(loadDevices(loaded));
            return "已恢复设备使用全局配置：" + shortId(device.deviceId);
        });
    }

    private void confirmDeleteDevice(DeviceInfo device, AlertDialog detailDialog) {
        new AlertDialog.Builder(this)
                .setTitle("删除设备记录")
                .setMessage("确定从云端删除这台设备吗？如果这台手机以后重新打开用户端 APK，它会再次出现在列表里。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定删除", (dialog, which) -> {
                    detailDialog.dismiss();
                    deleteDeviceRecord(device);
                })
                .show();
    }

    private void deleteDeviceRecord(DeviceInfo device) {
        runAsync("正在删除设备记录...", () -> {
            String body = "{\"target_device_id\":\"" + jsonEscape(device.deviceId) + "\","
                    + "\"admin_secret\":\"" + jsonEscape(ADMIN_DELETE_SECRET) + "\"}";
            supabaseRequest("POST", "/rpc/admin_delete_device", body, null);
            Config loaded = loadGlobalConfig();
            currentGlobalConfig = loaded;
            devices.clear();
            devices.addAll(loadDevices(loaded));
            return "已删除设备记录：" + shortId(device.deviceId);
        });
    }

    private void confirmBulk(String title, String message, CheckedAction action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> runAsync("正在执行批量操作...", action))
                .show();
    }

    private String bulkSetAll(boolean enabled, String selectedMessage) throws Exception {
        Config loaded = loadGlobalConfig();
        List<DeviceInfo> currentDevices = loadDevices(loaded);
        if (currentDevices.isEmpty()) return "还没有已登记设备。";

        String message = enabled ? DEFAULT_MESSAGE : normalizeMessage(selectedMessage);
        StringBuilder body = new StringBuilder("[");
        for (int i = 0; i < currentDevices.size(); i++) {
            DeviceInfo device = currentDevices.get(i);
            if (i > 0) body.append(",");
            body.append("{\"device_id\":\"").append(jsonEscape(device.deviceId)).append("\",")
                    .append("\"enabled\":").append(enabled).append(",")
                    .append("\"message\":\"").append(jsonEscape(message)).append("\",")
                    .append("\"updated_at\":\"").append(IsoClock.now()).append("\"}");
        }
        body.append("]");
        supabaseRequest("POST", "/device_configs?on_conflict=device_id", body.toString(), "resolution=merge-duplicates,return=minimal");
        Config latest = loadGlobalConfig();
        currentGlobalConfig = latest;
        devices.clear();
        devices.addAll(loadDevices(latest));
        return enabled ? "已一键开启全部设备。" : "已一键暂停全部设备。";
    }

    private String clearAllSpecificConfigs() throws Exception {
        supabaseRequest("DELETE", "/device_configs?device_id=not.is.null", null, "return=minimal");
        Config loaded = loadGlobalConfig();
        currentGlobalConfig = loaded;
        devices.clear();
        devices.addAll(loadDevices(loaded));
        return "已清空所有单独配置，全部设备将跟随全局开关。";
    }

    private String supabaseRequest(String method, String path, String body, String prefer) throws Exception {
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
        String response = readAll(stream);
        if (code < 200 || code >= 300) throw new Exception(response);
        return response;
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private List<DeviceInfo> parseDevices(String json) {
        List<DeviceInfo> result = new ArrayList<>();
        Matcher matcher = OBJECT_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            String object = matcher.group(1);
            String id = value(object, "device_id");
            if (id.isEmpty()) continue;
            DeviceInfo device = new DeviceInfo();
            device.deviceId = id;
            device.label = value(object, "label");
            device.appVersion = value(object, "app_version");
            device.firstSeen = value(object, "first_seen");
            device.lastSeen = value(object, "last_seen");
            result.add(device);
        }
        return result;
    }

    private Config findDeviceConfig(String json, String deviceId) {
        Matcher matcher = OBJECT_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            String object = matcher.group(1);
            if (deviceId.equals(value(object, "device_id"))) return parseConfig(object, true, DEFAULT_MESSAGE);
        }
        return null;
    }

    private Config parseConfig(String object, boolean defaultEnabled, String defaultMessage) {
        Config config = new Config();
        String enabled = value(object, "enabled");
        config.enabled = enabled.isEmpty() ? defaultEnabled : "true".equalsIgnoreCase(enabled);
        String message = value(object, "message");
        config.message = normalizeMessage(message.isEmpty() ? defaultMessage : message);
        return config;
    }

    private String firstObject(String json) {
        Matcher matcher = OBJECT_PATTERN.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String value(String object, String key) {
        Pattern quoted = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher quotedMatcher = quoted.matcher(object == null ? "" : object);
        if (quotedMatcher.find()) return jsonUnescape(quotedMatcher.group(1));
        Pattern literal = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false|null)");
        Matcher literalMatcher = literal.matcher(object == null ? "" : object);
        return literalMatcher.find() ? literalMatcher.group(1) : "";
    }

    private void renderDevices() {
        deviceList.removeAllViews();
        if (devices.isEmpty()) {
            TextView empty = label("还没有设备打开过用户端 APK。", 13, false, rgb(95, 107, 122));
            deviceList.addView(empty);
            return;
        }
        for (DeviceInfo device : devices) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(box(Color.WHITE, rgb(217, 224, 232), 8));
            row.addView(label(displayDeviceName(device), 15, true, rgb(23, 32, 42)));
            row.addView(label("ID: " + shortId(device.deviceId), 12, false, rgb(95, 107, 122)));
            row.addView(label("最近在线: " + device.lastSeen, 12, false, rgb(95, 107, 122)));
            row.addView(label((device.hasSpecificConfig ? "单独配置" : "使用全局") + " / " + (device.enabled ? "允许生成" : "暂停生成") + " / " + device.message, 12, false, device.enabled ? rgb(15, 118, 110) : rgb(190, 24, 93)));
            row.setOnClickListener(v -> showDeviceDialog(device));
            deviceList.addView(row, matchWrapTop(8));
        }
    }

    private void showDeviceDialog(DeviceInfo device) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(2), dp(8), dp(2), 0);

        LinearLayout configSection = dialogSection();
        configSection.addView(label("生成权限", 14, true, rgb(23, 32, 42)));
        CheckBox enabled = new CheckBox(this);
        enabled.setText("允许这台设备生成");
        enabled.setChecked(device.enabled);
        configSection.addView(enabled, matchWrapTop(6));
        configSection.addView(label("提示语", 14, true, rgb(23, 32, 42)), matchWrapTop(10));
        MessageSelector message = messageSelector(device.message);
        configSection.addView(message, matchWrapTop(6));
        form.addView(configSection);

        LinearLayout infoSection = dialogSection();
        infoSection.addView(label("设备信息", 14, true, rgb(23, 32, 42)));
        infoSection.addView(infoBox("设备 ID", device.deviceId), matchWrapTop(8));
        infoSection.addView(infoBox("最近在线", device.lastSeen), matchWrapTop(8));
        form.addView(infoSection, matchWrapTop(10));

        Button deleteDeviceButton = dangerButton("删除设备记录");
        form.addView(deleteDeviceButton, matchWrapTop(12));
        AlertDialog detailDialog = new AlertDialog.Builder(this)
                .setTitle(displayDeviceName(device))
                .setView(form)
                .setNegativeButton("删除单独配置", (dialog, which) -> deleteDeviceConfig(device))
                .setNeutralButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> saveDeviceConfig(device, enabled.isChecked(), message.getMessage()))
                .create();
        deleteDeviceButton.setOnClickListener(v -> confirmDeleteDevice(device, detailDialog));
        detailDialog.show();
        styleDialogWindow(detailDialog);
    }

    private MessageSelector messageSelector(String initialMessage) {
        MessageSelector selector = new MessageSelector();
        selector.setMessage(initialMessage);
        return selector;
    }

    private Config readGlobalSelection() {
        Config config = new Config();
        config.enabled = globalEnabled.isChecked();
        config.message = globalMessage.getMessage();
        return config;
    }

    private String normalizeMessage(String message) {
        if (message == null || message.trim().isEmpty()) return DEFAULT_MESSAGE;
        String trimmed = message.trim();
        for (String option : MESSAGE_OPTIONS) {
            if (option.equals(trimmed)) return trimmed;
        }
        if (trimmed.contains("升级")) return "系统升级中，请稍后再试";
        if (trimmed.contains("暂停")) return "今日暂停生成";
        if (trimmed.contains("不可用")) return "当前功能暂不可用";
        if (trimmed.contains("稍后")) return "请稍后再试";
        if (trimmed.contains("维护")) return "服务维护中";
        return DEFAULT_MESSAGE;
    }

    private String displayDeviceName(DeviceInfo device) {
        return device.label == null || device.label.isEmpty() ? "未命名设备" : device.label;
    }

    private String shortId(String id) {
        return id == null || id.length() <= 8 ? id : id.substring(0, 8);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String jsonUnescape(String value) {
        return value == null ? "" : value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private void setBusy(boolean busy, String message) {
        refreshButton.setEnabled(!busy);
        saveGlobalButton.setEnabled(!busy);
        pauseAllButton.setEnabled(!busy);
        enableAllButton.setEnabled(!busy);
        clearAllButton.setEnabled(!busy);
        statusView.setText(message);
    }

    private LinearLayout topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(22), dp(18), dp(18));
        bar.setBackgroundColor(rgb(17, 24, 39));
        TextView icon = label("控", 20, true, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(box(rgb(20, 184, 166), rgb(20, 184, 166), 14));
        bar.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(label("设备开关管理端", 22, true, Color.WHITE));
        copy.addView(label("查看安装设备，控制全局或单台设备", 13, false, rgb(209, 213, 219)));
        bar.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return bar;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(box(Color.WHITE, rgb(217, 224, 232), 10));
        return layout;
    }

    private LinearLayout dialogSection() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));
        layout.setBackground(box(rgb(248, 250, 252), rgb(226, 232, 240), 10));
        return layout;
    }

    private LinearLayout infoBox(String title, String value) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(10), dp(12), dp(10));
        layout.setBackground(box(Color.WHITE, rgb(226, 232, 240), 8));
        layout.addView(label(title, 11, true, rgb(100, 116, 139)));
        TextView content = label(value == null || value.isEmpty() ? "-" : value, 13, false, rgb(23, 32, 42));
        content.setPadding(0, dp(4), 0, 0);
        content.setTextIsSelectable(true);
        layout.addView(content);
        return layout;
    }

    private TextView messageOptionRow(String text, boolean selected) {
        TextView row = label(selected ? "✓  " + text : "   " + text, 15, selected, selected ? rgb(15, 118, 110) : rgb(23, 32, 42));
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(box(selected ? rgb(236, 253, 245) : Color.WHITE, selected ? rgb(45, 212, 191) : rgb(226, 232, 240), 10));
        return row;
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

    private Button dangerButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(rgb(190, 24, 93));
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setBackground(box(rgb(255, 241, 242), rgb(251, 113, 133), 8));
        return button;
    }

    private void styleDialogWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View decor = window.getDecorView();
        decor.setPadding(dp(10), dp(10), dp(10), dp(10));
        decor.setBackground(box(Color.WHITE, rgb(203, 213, 225), 16));
    }

    private GradientDrawable box(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams pageCardParams(int topDp) {
        LinearLayout.LayoutParams params = matchWrapTop(topDp);
        params.leftMargin = dp(18);
        params.rightMargin = dp(18);
        return params;
    }

    private LinearLayout.LayoutParams matchWrapTop(int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int rgb(int r, int g, int b) {
        return Color.rgb(r, g, b);
    }

    private class MessageSelector extends LinearLayout {
        private final TextView valueView;
        private String message = DEFAULT_MESSAGE;

        MessageSelector() {
            super(MainActivity.this);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(14), dp(12), dp(12), dp(12));
            setBackground(box(Color.WHITE, rgb(203, 213, 225), 10));
            setClickable(true);
            setFocusable(true);

            valueView = label(DEFAULT_MESSAGE, 15, false, rgb(23, 32, 42));
            valueView.setSingleLine(false);
            addView(valueView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView arrow = label("⌄", 22, true, rgb(71, 85, 105));
            arrow.setGravity(Gravity.CENTER);
            addView(arrow, new LinearLayout.LayoutParams(dp(32), LinearLayout.LayoutParams.WRAP_CONTENT));

            setOnClickListener(v -> openMessageDialog());
        }

        String getMessage() {
            return message;
        }

        void setMessage(String nextMessage) {
            message = normalizeMessage(nextMessage);
            valueView.setText(message);
        }

        private void openMessageDialog() {
            LinearLayout options = new LinearLayout(MainActivity.this);
            options.setOrientation(VERTICAL);
            options.setPadding(dp(4), dp(8), dp(4), 0);
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("选择提示语")
                    .setView(options)
                    .setNegativeButton("取消", null)
                    .create();
            for (String option : MESSAGE_OPTIONS) {
                TextView row = messageOptionRow(option, option.equals(message));
                row.setOnClickListener(v -> {
                    setMessage(option);
                    dialog.dismiss();
                });
                options.addView(row, matchWrapTop(8));
            }
            dialog.show();
            styleDialogWindow(dialog);
        }
    }

    private static Config defaultConfig() {
        Config config = new Config();
        config.enabled = true;
        config.message = DEFAULT_MESSAGE;
        return config;
    }

    private interface CheckedAction {
        String run() throws Exception;
    }

    private static class Config {
        boolean enabled;
        String message;
    }

    private static class DeviceInfo {
        String deviceId;
        String label;
        String appVersion;
        String firstSeen;
        String lastSeen;
        boolean hasSpecificConfig;
        boolean enabled;
        String message;
    }
}
