package com.evos.ui.activities;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.evos.R;
import com.evos.interfaces.IDialogFragmentContainer;
import com.evos.network.impl.NetService;
import com.evos.storage.ReceivedPreferences;
import com.evos.ui.fragments.dialogues.YesNoDialogFragment;
import com.evos.view.AbstractBaseActivity;
import com.evos.view.CustomCheckBox;

import java.io.Serializable;
import java.util.Locale;

public class OpenFuelDiscountActivity extends AbstractBaseActivity implements IDialogFragmentContainer {

    private WebView wvFuelDiscount;
    private CustomCheckBox cbBrightMode;
    private ProgressBar pbLoading;
    private int brightness;
    private int brightnessMode = 0;
    private static final String langString = "&lang=";
    private static final String themeString = "&theme=";
    private static final String ua = "ua";
    private static final String ru = "ru";
    ActivityResultLauncher<Intent> activityResultLauncher;

    private enum DialogId {
        REQUEST_WRITE_PERMISSION
    }

    @Override
    protected void callBeforeSetContentView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        super.callBeforeSetContentView();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fuel_discount;
    }

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
        if (isCanWriteAccess()) {
            getBrightMode();
            getCurrentBright();
        }
        openFuelDiscountLink(NetService.getPreferencesManager().getReceivedPreferences());
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (isCanWriteAccess()) {
            setBrightness(brightness);
            setAutoModeIfNeeded();
        }
    }

    private void setAutoModeIfNeeded() {
        if (brightnessMode == 1) {
            ContentResolver contentResolver = getContentResolver();
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
    }

    @Override
    protected void findViews() {
        super.findViews();
        initWebView();
        cbBrightMode = this.findViewById(R.id.cb_bright_mode);
        addStyleableView(cbBrightMode);

        pbLoading = this.findViewById(R.id.pbLoading);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        wvFuelDiscount = this.findViewById(R.id.wvFuelDiscount);
        wvFuelDiscount.getSettings().setJavaScriptEnabled(true);
        wvFuelDiscount.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        wvFuelDiscount.getSettings().setAppCacheEnabled(false);
        wvFuelDiscount.getSettings().setDomStorageEnabled(true);
        wvFuelDiscount.getSettings().setLoadsImagesAutomatically(true);
        wvFuelDiscount.getSettings().setAllowFileAccess(false);
        wvFuelDiscount.getSettings().setDatabaseEnabled(false);

        wvFuelDiscount.setWebChromeClient(new WebChromeClient());

        wvFuelDiscount.setWebViewClient(new WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pbLoading.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void setInteractionHandlers() {
        cbBrightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isCanWriteAccess()) {
                cbBrightMode.setChecked(!isChecked);
                showBrightnessPermissionDialog();
                return;
            }
            if (isChecked) {
                setBrightness(255);
            } else {
                setBrightness(brightness);
            }
        });
    }

    private boolean isCanWriteAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showSystemSettingsScreen() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityInfo ai = intent.resolveActivityInfo(getPackageManager(), 0);
        if (ai != null && ai.exported){
            activityResultLauncher.launch(intent);
        }
    }

    private void getCurrentBright() {
        try {
            ContentResolver contentResolver = getContentResolver();
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setBrightness(int bright) {
        if (brightness == 0) {
            getBrightMode();
            getCurrentBright();
        }
        ContentResolver cResolver = getContentResolver();
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, bright);
    }

    private void showBrightnessPermissionDialog() {
        Bundle bundle = new Bundle();
        bundle.putInt(YesNoDialogFragment.KEY_MESSAGE_ID, R.string.granting_permissions_is_required);
        bundle.putSerializable(YesNoDialogFragment.KEY_DIALOG_ID, DialogId.REQUEST_WRITE_PERMISSION);
        YesNoDialogFragment yesNoDialog = new YesNoDialogFragment();
        yesNoDialog.setArguments(bundle);
        yesNoDialog.show(getSupportFragmentManager(), "permission");
    }

    private void openFuelDiscountLink(ReceivedPreferences receivedPreferences) {
        if (!TextUtils.isEmpty(receivedPreferences.getFuelDiscountLink())) {
            pbLoading.setVisibility(View.VISIBLE);

            String url = receivedPreferences.getFuelDiscountLink();
            url += langString + getDefaultLocale();
            url += themeString + getCurrentTheme();
            wvFuelDiscount.loadUrl(url);
        }
    }

    private String getCurrentTheme() {
        return com.evos.storage.Settings.isThemeDark() ? "dark" : "light";
    }

    private String getDefaultLocale() {
        String defaultLocale = Locale.getDefault().getLanguage().toLowerCase();
        return defaultLocale.equals(ru) ? ru : ua;
    }

    private void getBrightMode() {
        try {
            ContentResolver contentResolver = getContentResolver();
            brightnessMode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onDialogResult(Serializable dialogId, Serializable actionId) {
        if (dialogId == DialogId.REQUEST_WRITE_PERMISSION
                && actionId == YesNoDialogFragment.YesNoEnum.YES) {
            showSystemSettingsScreen();
        }
    }
}