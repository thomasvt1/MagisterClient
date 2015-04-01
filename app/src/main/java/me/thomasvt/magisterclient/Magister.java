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

import java.util.List;

import me.thomasvt.magisterclient.db.School;
import me.thomasvt.magisterclient.db.SchoolDatabaseHelper;


public class Magister extends Activity {
    private WebView mWebView;
    private SharedPreferences mPreferences;
    private SchoolDatabaseHelper mDatabase;
    
    public static final String TAG = "Magistre";
    private static final String PREF_HIDEMENU = "hidemenu";
    public static final String PREF_HOST = "host";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mDatabase = new SchoolDatabaseHelper(this);

        if (mPreferences.getBoolean(PREF_HIDEMENU, false)) {
            getActionBar().hide();
        }

        CookieSyncManager.createInstance(this).startSync();

        String oldUrl = mPreferences.getString("url", null);
        if(oldUrl != null) {
            mPreferences.edit()
                .putString(PREF_HOST, oldUrl.endsWith(".magister.net") ? oldUrl : oldUrl + ".magister.net")
                .remove("url")
                .apply();
        }

        String url = mPreferences.getString(PREF_HOST, null);
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
        mWebView.loadUrl("about:blank");

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
        return mPreferences.getString(PREF_HOST, null);
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

        boolean showFavourites = mDatabase.hasFavourites();
        menu.findItem(R.id.favourite_schools).setVisible(showFavourites).setEnabled(showFavourites);
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
            case R.id.favourite_schools:
                final List<School> favouriteList = mDatabase.getFavourites();
                CharSequence[] favourites = new CharSequence[favouriteList.size()];
                for(int i = 0; i < favouriteList.size(); i++) {
                    favourites[i] = favouriteList.get(i).name;
                }
                new AlertDialog.Builder(Magister.this)
                    .setItems(favourites, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPreferences.edit().putString(PREF_HOST, favouriteList.get(i).host).apply();
                            loadWebsite();
                        }
                    })
                    .show();
                return true;
        }
        return false;
    }
}