package com.tschuy.soilfinder;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by evant_000 on 3/20/14.
 */

// SSL tolerance for Soil Query
class SSLTolerantWebViewClient extends WebViewClient {
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed(); // Ignore SSL certificate errors for name queries
    }
}
