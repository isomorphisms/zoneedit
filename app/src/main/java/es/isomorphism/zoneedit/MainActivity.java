package es.isomorphism.zoneedit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public final class MainActivity extends Activity {
    private static final String START_URL = "https://cp.zoneedit.com/login.php";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.setWebContentsDebuggingEnabled(false);
        webView = new WebView(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        }
        setContentView(webView);

        CookieManager.getInstance().setAcceptCookie(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(110);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!isHttps(uri)) {
                    return true;
                }
                if (isZoneEdit(uri)) {
                    return false;
                }
                if (!request.isForMainFrame()) {
                    return false;
                }
                Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browser);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Uri uri = Uri.parse(url);
                if (isZoneEdit(uri)) {
                    injectMobileCss(view);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            handleBack();
                        }
                    });
        }

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL);
        }
    }

    private static boolean isHttps(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme());
    }

    private static boolean isZoneEdit(Uri uri) {
        String host = uri.getHost();
        return isHttps(uri)
                && host != null
                && (host.equalsIgnoreCase("zoneedit.com")
                        || host.toLowerCase(java.util.Locale.ROOT).endsWith(".zoneedit.com"));
    }

    private static void injectMobileCss(WebView view) {
        String script = "(function(){"
                + "var h=document.head||document.documentElement;"
                + "if(!document.querySelector('meta[name=viewport]')){"
                + "var m=document.createElement('meta');m.name='viewport';m.content='width=device-width,initial-scale=1';h.appendChild(m);}"
                + "if(!document.getElementById('ze-mobile')){"
                + "var s=document.createElement('style');s.id='ze-mobile';"
                + "s.textContent='html,body{max-width:100%!important;overflow-x:hidden!important}'"
                + "+'input,select,textarea,button{font-size:16px!important;min-height:44px!important}'"
                + "+'table{max-width:100%!important;display:block!important;overflow-x:auto!important}'"
                + "+'img{max-width:100%!important;height:auto!important}';h.appendChild(s);}"
                + "})();";
        view.evaluateJavascript(script, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    private void handleBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }
}
