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


    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        String html = "<html><body><h2>Geen internet!</h2>Het lijkt erop dat je geen internetverbinding hebt.<br>Probeer het later opnieuw!<br><br><br><br><h3>Tip:</h3>Laad pagina's als je verbinding hebt zodat als je offline gaat de app deze van je cache kan laden.</body></html>";
        view.loadData(html, "text/html", null);
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

