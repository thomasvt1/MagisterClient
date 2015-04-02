package me.thomasvt.magisterclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyAppWebViewClient extends WebViewClient {

    private Context context;
    private SharedPreferences mPreferences;

    public MyAppWebViewClient(Context context){
        this.context = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        CookieSyncManager.getInstance().sync();
        /*

        [].slice.call(document.getElementsByTagName('A'), 0).forEach(function(link) {
            if (link.target === '_blank') {
                link.target = '_self';
                link.href = 'newtab:' + link.href;
            }
        });

        */

        view.loadUrl("javascript: [].slice.call(document.getElementsByTagName('A'), 0).forEach(function(e){'_blank'===e.target&&(e.target='_self',e.href='newtab:'+e.href)});");
    }


    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        String html = "<html><body><h2>Geen internet!</h2>Het lijkt erop dat je geen internetverbinding hebt.<br>Probeer het later opnieuw!<br><br><br><br><h3>Tip:</h3>Laad pagina's als je verbinding hebt zodat als je offline gaat de app deze van je cache kan laden.<br>Ook kan het zijn dat jouw school nog geen Magister 6 ondersteund.</body></html>";
        view.loadData(html, "text/html", null);
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.contains("/studiewijzers/") || url.contains("/berichten/bijlagen/")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
            view.goBack();
            return false;
        }
        if (url.startsWith("https://" + mPreferences.getString(Magister.PREF_HOST, null))) {
            return false;
        }
        return false;
    }
}

