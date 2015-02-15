package me.thomasvt.magisterclient;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyAppWebViewClient extends WebViewClient {

    private Context context;

    public MyAppWebViewClient(Context context){
        this.context = context;
    }


    @Override
    public void onPageFinished(WebView view, String url) {
        CookieSyncManager.getInstance().sync();
        view.loadUrl("javascript: var allLinks = document.getElementsByTagName('a'); if (allLinks) {var i;for (i=0; i<allLinks.length; i++) {var link = allLinks[i];var target = link.getAttribute('target'); if (target && target == '_blank') {link.setAttribute('target','_self');link.href = 'newtab:'+link.href;}}}");
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.contains("/studiewijzers/")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
            view.goBack();
            return false;
        }
        if (url.startsWith("https://" + Magister.getUrl())) {
            view.loadUrl(url);
            return true;
        }
        return false;
    }
}

