package com.liming.livestage;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity implements SimulationEngine.Listener {
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final String PREFS = "livestage_studio";
    private static final String CAMERA_REQUESTED = "camera_requested";

    private CameraPreviewView cameraPreview;
    private LiveOverlayView overlay;
    private SimulationEngine engine;
    private SimulationConfig config;
    private HorizontalScrollView controls;
    private boolean controlsVisible = true;
    private boolean simulationPaused;
    private Button pauseButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode();

        config = loadConfig();
        buildUi();
        engine = new SimulationEngine(config, this);
        overlay.setHost(config.hostName, config.title);
        overlay.setViewers(config.initialViewers);
        overlay.addMessage(new ChatMessage("系统", "本地模拟模式已开启，所有互动均为虚拟数据", ChatMessage.Type.SYSTEM));

        if (!hasAcceptedTerms()) showFirstRunAgreement();
        else startExperience();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF111111);
        cameraPreview = new CameraPreviewView(this);
        root.addView(cameraPreview, matchParent());
        overlay = new LiveOverlayView(this);
        root.addView(overlay, matchParent());

        controls = createControlBar();
        FrameLayout.LayoutParams controlParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(70));
        controlParams.gravity = Gravity.BOTTOM;
        root.addView(controls, controlParams);

        Button toggle = createRoundButton("×");
        toggle.setTextSize(18f);
        toggle.setContentDescription("收起或展开控制栏");
        toggle.setOnClickListener(v -> toggleControls(toggle));
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dp(42), dp(42));
        toggleParams.gravity = Gravity.END | Gravity.BOTTOM;
        toggleParams.setMargins(0, 0, dp(8), dp(78));
        root.addView(toggle, toggleParams);
        setContentView(root);
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private HorizontalScrollView createControlBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xE8000000);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(7), dp(8), dp(7), dp(8));

        Button comment = createActionButton("弹幕");
        comment.setOnClickListener(v -> engine.addManualComment());
        bar.addView(comment, actionParams());
        Button gift = createActionButton("礼物");
        gift.setOnClickListener(v -> engine.addManualGift());
        bar.addView(gift, actionParams());
        Button follow = createActionButton("关注");
        follow.setOnClickListener(v -> engine.addManualFollow());
        bar.addView(follow, actionParams());
        Button like = createActionButton("点赞");
        like.setOnClickListener(v -> engine.addManualLikes());
        bar.addView(like, actionParams());

        Button camera = createActionButton("镜头");
        camera.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraIfNeeded(true);
                return;
            }
            int result = cameraPreview.toggleLens();
            if (result == CameraPreviewView.SWITCH_UNAVAILABLE) {
                Toast.makeText(this, "设备没有可切换的另一枚摄像头", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        result == CameraPreviewView.SWITCHED_FRONT ? "已切换到前置镜头" : "已切换到后置镜头",
                        Toast.LENGTH_SHORT).show();
            }
        });
        bar.addView(camera, actionParams());

        pauseButton = createActionButton("暂停");
        pauseButton.setOnClickListener(v -> toggleSimulation());
        bar.addView(pauseButton, actionParams());
        Button settings = createActionButton("设置");
        settings.setOnClickListener(v -> showSettingsDialog());
        bar.addView(settings, actionParams());
        Button info = createActionButton("说明");
        info.setOnClickListener(v -> showLegalDialog());
        bar.addView(info, actionParams());

        scroll.addView(bar, new FrameLayout.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.MATCH_PARENT));
        return scroll;
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(72), dp(48));
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private Button createActionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13f);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF292929);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFF555555);
        button.setBackground(bg);
        return button;
    }

    private Button createRoundButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xCC202020);
        bg.setStroke(dp(1), 0xFF666666);
        button.setBackground(bg);
        return button;
    }

    private void toggleControls(Button toggle) {
        controlsVisible = !controlsVisible;
        controls.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
        toggle.setText(controlsVisible ? "×" : "≡");
    }

    private void toggleSimulation() {
        simulationPaused = !simulationPaused;
        if (simulationPaused) {
            engine.stop();
            pauseButton.setText("继续");
            overlay.addMessage(new ChatMessage("系统", "模拟互动已暂停", ChatMessage.Type.SYSTEM));
        } else {
            engine.start();
            pauseButton.setText("暂停");
            overlay.addMessage(new ChatMessage("系统", "模拟互动已继续", ChatMessage.Type.SYSTEM));
        }
    }

    private void showSettingsDialog() {
        int pad = dp(18);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, dp(8), pad, dp(4));
        EditText hostInput = createEditText("主播昵称", config.hostName, InputType.TYPE_CLASS_TEXT);
        content.addView(label("主播昵称"));
        content.addView(hostInput);
        EditText titleInput = createEditText("直播标题", config.title, InputType.TYPE_CLASS_TEXT);
        content.addView(label("演示标题"));
        content.addView(titleInput);
        EditText viewersInput = createEditText("初始在线人数", String.valueOf(config.initialViewers), InputType.TYPE_CLASS_NUMBER);
        content.addView(label("初始模拟人数"));
        content.addView(viewersInput);

        Spinner sceneSpinner = new Spinner(this);
        String[] scenes = new String[SimulationConfig.Scene.values().length];
        for (int i = 0; i < scenes.length; i++) scenes[i] = SimulationConfig.Scene.values()[i].label;
        sceneSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, scenes));
        sceneSpinner.setSelection(config.scene.ordinal());
        content.addView(label("场景词库"));
        content.addView(sceneSpinner);

        Spinner speedSpinner = new Spinner(this);
        String[] speeds = new String[SimulationConfig.Speed.values().length];
        for (int i = 0; i < speeds.length; i++) speeds[i] = SimulationConfig.Speed.values()[i].label;
        speedSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, speeds));
        speedSpinner.setSelection(config.speed.ordinal());
        content.addView(label("弹幕速度"));
        content.addView(speedSpinner);

        CheckBox bilingual = new CheckBox(this);
        bilingual.setText("混合中英文观众");
        bilingual.setChecked(config.bilingual);
        content.addView(bilingual);
        TextView note = new TextView(this);
        note.setText("所有观众、评论、礼物和人数均为本地模拟。应用没有网络权限，模拟标识不可关闭。");
        note.setTextColor(0xFF666666);
        note.setTextSize(12f);
        note.setPadding(0, dp(12), 0, dp(4));
        content.addView(note);
        ScrollView wrapper = new ScrollView(this);
        wrapper.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("LiveStage 设置")
                .setView(wrapper)
                .setNegativeButton("取消", null)
                .setNeutralButton("清除本地数据", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                int initial;
                try {
                    initial = Integer.parseInt(viewersInput.getText().toString().trim());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "模拟人数必须是数字", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (initial < 1 || initial > 999999) {
                    Toast.makeText(this, "模拟人数范围为 1—999999", Toast.LENGTH_SHORT).show();
                    return;
                }
                SimulationConfig next = new SimulationConfig();
                next.hostName = safeText(hostInput.getText().toString(), "HOST", 24);
                next.title = safeText(titleInput.getText().toString(), "直播训练演示", 40);
                next.initialViewers = initial;
                next.scene = SimulationConfig.Scene.values()[sceneSpinner.getSelectedItemPosition()];
                next.speed = SimulationConfig.Speed.values()[speedSpinner.getSelectedItemPosition()];
                next.bilingual = bilingual.isChecked();
                config = next;
                saveConfig(config);
                overlay.setHost(config.hostName, config.title);
                engine.applyConfig(config);
                dialog.dismiss();
            });
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> confirmClearData(dialog));
        });
        dialog.setOnDismissListener(d -> enterImmersiveMode());
        dialog.show();
    }

    private void confirmClearData(AlertDialog parent) {
        new AlertDialog.Builder(this)
                .setTitle("清除本地数据")
                .setMessage("将删除昵称、标题、场景、速度和当前模拟计数，但不会删除应用。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (d, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
                    config = new SimulationConfig();
                    saveTermsAccepted();
                    saveConfig(config);
                    overlay.clearSession();
                    overlay.setHost(config.hostName, config.title);
                    engine.resetSession(config);
                    overlay.addMessage(new ChatMessage("系统", "本地配置和当前模拟计数已重置", ChatMessage.Type.SYSTEM));
                    parent.dismiss();
                    Toast.makeText(this, "本地配置已清除", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private EditText createEditText(String hint, String value, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value);
        input.setInputType(type);
        input.setSingleLine(true);
        input.setTextSize(15f);
        input.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        params.setMargins(0, 0, 0, dp(8));
        input.setLayoutParams(params);
        return input;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFF333333);
        view.setTextSize(12f);
        view.setPadding(0, dp(6), 0, 0);
        return view;
    }

    private String safeText(String value, String fallback, int max) {
        String clean = value == null ? "" : value.trim().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        if (clean.isEmpty()) clean = fallback;
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    private void showFirstRunAgreement() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("LiveStage Studio 使用确认")
                .setMessage("本软件仅用于直播训练、影视演示和知情娱乐。所有观众、弹幕、人数、点赞、关注和礼物均为本地模拟，不代表真实互动或收入。\n\n继续使用即表示你同意不将其用于诈骗、虚假交易、冒充真实平台或骚扰他人。")
                .setCancelable(false)
                .setNegativeButton("退出", (d, which) -> finish())
                .setNeutralButton("查看条款", null)
                .setPositiveButton("同意并进入", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> showLegalDialog());
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                saveTermsAccepted();
                dialog.dismiss();
                startExperience();
                enterImmersiveMode();
            });
        });
        dialog.show();
    }

    private void showLegalDialog() {
        ScrollView scroll = new ScrollView(this);
        TextView text = new TextView(this);
        text.setText(LegalText.PRIVACY + "\n\n" + LegalText.TERMS + "\n\n版本：" + BuildConfig.VERSION_NAME);
        text.setTextColor(0xFF222222);
        text.setTextSize(14f);
        text.setLineSpacing(0, 1.18f);
        text.setPadding(dp(20), dp(12), dp(20), dp(18));
        scroll.addView(text);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("隐私与使用条款")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .create();
        dialog.setOnDismissListener(d -> enterImmersiveMode());
        dialog.show();
    }

    private boolean hasAcceptedTerms() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("terms_accepted_v1", false);
    }

    private void saveTermsAccepted() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("terms_accepted_v1", true).apply();
    }

    private void startExperience() {
        if (engine != null && !simulationPaused) engine.start();
        requestCameraIfNeeded(false);
    }

    private void requestCameraIfNeeded(boolean userInitiated) {
        if (!cameraPreview.hasAnyCamera()) {
            overlay.addMessage(new ChatMessage("系统", "设备未检测到摄像头，已进入纯模拟模式", ChatMessage.Type.SYSTEM));
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPreview.start();
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean requestedBefore = prefs.getBoolean(CAMERA_REQUESTED, false);
        if (requestedBefore && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            if (userInitiated) showCameraSettingsDialog();
            else overlay.addMessage(new ChatMessage("系统", "摄像头权限已关闭，点击“镜头”可前往系统设置", ChatMessage.Type.SYSTEM));
            return;
        }
        prefs.edit().putBoolean(CAMERA_REQUESTED, true).apply();
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    private void showCameraSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("摄像头权限未开启")
                .setMessage("可以继续使用纯模拟界面。需要显示实时镜头时，请到系统设置中开启摄像头权限。")
                .setNegativeButton("仅使用模拟界面", null)
                .setPositiveButton("打开系统设置", (d, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setOnDismissListener(d -> enterImmersiveMode())
                .show();
    }

    private SimulationConfig loadConfig() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        SimulationConfig loaded = new SimulationConfig();
        loaded.hostName = safeText(p.getString("host_name", loaded.hostName), "HOST", 24);
        loaded.title = safeText(p.getString("title", loaded.title), "直播训练演示", 40);
        loaded.initialViewers = Math.max(1, Math.min(999999, p.getInt("viewers", loaded.initialViewers)));
        try { loaded.scene = SimulationConfig.Scene.valueOf(p.getString("scene", loaded.scene.name())); }
        catch (IllegalArgumentException ignored) { loaded.scene = SimulationConfig.Scene.BAR; }
        try { loaded.speed = SimulationConfig.Speed.valueOf(p.getString("speed", loaded.speed.name())); }
        catch (IllegalArgumentException ignored) { loaded.speed = SimulationConfig.Speed.NORMAL; }
        loaded.bilingual = p.getBoolean("bilingual", loaded.bilingual);
        return loaded;
    }

    private void saveConfig(SimulationConfig value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("host_name", value.hostName)
                .putString("title", value.title)
                .putInt("viewers", value.initialViewers)
                .putString("scene", value.scene.name())
                .putString("speed", value.speed.name())
                .putBoolean("bilingual", value.bilingual)
                .apply();
    }

    private void enterImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (hasAcceptedTerms() && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPreview.start();
        }
        if (engine != null && hasAcceptedTerms() && !simulationPaused) engine.start();
    }

    @Override protected void onPause() {
        if (engine != null) engine.stop();
        if (cameraPreview != null) cameraPreview.stop();
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_REQUEST_CODE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraPreview.start();
        } else {
            overlay.addMessage(new ChatMessage("系统", "摄像头权限未授予，当前为纯模拟界面", ChatMessage.Type.SYSTEM));
            Toast.makeText(this, "未授予摄像头权限，应用仍可使用纯模拟界面", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override protected void onDestroy() {
        if (engine != null) engine.stop();
        if (cameraPreview != null) cameraPreview.stop();
        super.onDestroy();
    }

    @Override public void onViewerCountChanged(int count) { overlay.setViewers(count); }
    @Override public void onLikeCountChanged(long count) { overlay.setLikes(count); }
    @Override public void onMessage(ChatMessage message) { overlay.addMessage(message); }
    @Override public void onGift(GiftEvent event) { overlay.showGift(event); }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
