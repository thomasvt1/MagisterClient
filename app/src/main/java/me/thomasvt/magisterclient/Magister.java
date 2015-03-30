package me.thomasvt.magisterclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;


public class Magister extends Activity {
    private WebView mWebView;
    private SharedPreferences mPreferences;
    
    public static final String TAG = "Magistre";
    private static final String PREF_HIDEMENU = "hidemenu";
    public static final String PREF_URL = "url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (mPreferences.getBoolean(PREF_HIDEMENU, false)) {
            getActionBar().hide();
        }

        CookieSyncManager.createInstance(this).startSync();

        String url = mPreferences.getString(PREF_URL, null);
        if(url == null) {
            selectSchool();
            finish();
            return;
        }

        setContentView(R.layout.activity_magister);
        mWebView = (WebView) findViewById(R.id.activity_magister_webview);

        WebSettings webSettings = mWebView.getSettings();

        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        webSettings.setAppCachePath(getCacheDir().getAbsolutePath());
        //TODO: Better cache function
        //webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        //webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }
        mWebView.setWebViewClient(new MyAppWebViewClient(this));

        if(savedInstanceState == null) {
            loadWebsite();
        }

        giveVoteOption(); //ask for rating if requirement met
    }

    /* Disabled because function caused a freeze on startup, causing an never ending white screen.
    void analytics() {
        Log.i(TAG, "analytics");
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.newTracker(R.xml.global_tracker);
        analytics.enableAutoActivityReports(getApplication());
        analytics.setLocalDispatchPeriod(30);

        GoogleAnalytics.getInstance(this).reportActivityStart(this); //Get an Analytics tracker to report app starts & uncaught exceptions etc.
        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }
    */

    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        CookieSyncManager.getInstance().stopSync();
        super.onDestroy();
    }

    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
    }

    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        //GoogleAnalytics.getInstance(this).reportActivityStop(this); //Stop the analytics tracking
    }

    private boolean isNetworkAvailable() {
        Log.i(TAG, "isNetworkAvailable");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loadWebsite() {
        String url = "https://" + getHost() + "/";
        Log.i(TAG, "loadWebsite");

        WebSettings webSettings = mWebView.getSettings();

        if (!isNetworkAvailable()) { // loading offline
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            mWebView.loadUrl(url + "leerling/#/agenda");
            Toast.makeText(Magister.this, R.string.offline_mode, Toast.LENGTH_LONG).show();
        }
        else {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            mWebView.loadUrl(url);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    public String getHost() {
        String url = mPreferences.getString(PREF_URL, null);
        // Tobias: Compat met vorige versies
        if(!url.endsWith(".magister.net")) {
            url += ".magister.net";
            mPreferences.edit().putString(PREF_URL, url).apply();
        }
        return url;
    }

    private void selectSchool() {
        Log.i(TAG, "selectSchool");
        startActivity(new Intent(this, SchoolSelector.class));
    }

    private void giveVoteOption() {
        Log.i(TAG, "giveVoteOption");
        AppRater appRater = new AppRater(this);
        appRater.setDaysBeforePrompt(3);
        appRater.setLaunchesBeforePrompt(7);

        appRater.setPhrases("Waardeer deze app", "We zouden het erg leuk vinden als je de app waardeerd op Google Play, Bedankt voor je support!", "Waardeer", "Later", "Nee bedankt");
        appRater.setTargetUri("https://play.google.com/store/apps/details?id=me.thomasvt.magisterclient");
        appRater.setPreferenceKeys("app_rater", "flag_dont_show", "launch_count", "first_launch_time");

        appRater.show();
    }

    private Toast mExitToast;

    @Override
    public void onBackPressed() {
        if (mWebView != null && !mWebView.getUrl().equals("https://" + getHost() + "/magister/#/vandaag") && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }

        // Tobias: Degelijkere methode om te checken of een Toast nog zichtbaar is.
        if (mExitToast != null && mExitToast.getView() != null && mExitToast.getView().isShown()) {
            mExitToast.cancel();
            finish();
            return;
        }

        mExitToast = Toast.makeText(this, R.string.repeat_click_to_close, Toast.LENGTH_SHORT);
        mExitToast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.magister, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean actionBarHidden = mPreferences.getBoolean(PREF_HIDEMENU, false);
        menu.findItem(R.id.action_hide_actionbar).setVisible(!actionBarHidden).setEnabled(!actionBarHidden);
        menu.findItem(R.id.action_show_actionbar).setVisible(actionBarHidden).setEnabled(actionBarHidden);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload:
                mWebView.reload();
                return true;
            case R.id.action_clear_cache:
                mWebView.clearCache(true);
                mWebView.reload();
                return true;
            case R.id.action_change:
                selectSchool();
                return true;
            case R.id.action_hide_actionbar:
                new AlertDialog.Builder(this)
                    .setTitle(R.string.action_hide_actionbar)
                    .setMessage(R.string.warning_hide_actionbar)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActionBar().hide();
                            mPreferences.edit().putBoolean(PREF_HIDEMENU, true).apply();
                        }
                    })
                    .show();
                return true;
            case R.id.action_show_actionbar:
                getActionBar().show();
                mPreferences.edit().putBoolean(PREF_HIDEMENU, false).apply();
                return true;
        }
        return false;
    }
}