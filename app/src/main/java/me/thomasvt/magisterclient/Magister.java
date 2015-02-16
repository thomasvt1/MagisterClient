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
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.analytics.GoogleAnalytics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.app.AlertDialog.*;


public class Magister extends Activity {
    private WebView mWebView;
    static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("debug", "onCreate");
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (preferences.getBoolean("hidemenu", false))
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        checkFistStart(); // prompt with school choose if not started

        giveVoteOption(); //ask for rating if requirement met
    }

    /* Disabled because function caused a freeze on startup, causing an never ending white screen.
    void analytics() {
        Log.i("debug", "analytics");
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.newTracker(R.xml.global_tracker);
        analytics.enableAutoActivityReports(getApplication());
        analytics.setLocalDispatchPeriod(30);

        GoogleAnalytics.getInstance(this).reportActivityStart(this); //Get an Analytics tracker to report app starts & uncaught exceptions etc.
        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }
    */
    void enableSite() {
        Log.i("debug", "enableSite");
        mWebView = new WebView(this);

        setContentView(R.layout.activity_magister);

        mWebView = (WebView) findViewById(R.id.activity_magister_webview);

        new MyAppWebViewClient(this);

        loadWebsite(false);
    }

    protected void onStart() {
        Log.i("debug", "onStart");
        super.onStart();
    }

    protected void onStop() {
        Log.i("debug", "onStop");
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this); //Stop the analytics tracking
    }

    private boolean isNetworkAvailable() {
        Log.i("debug", "isNetworkAvailable");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public void welcomeFinished(View v) {
        Log.i("debug", "welcomeFinished");
        final EditText text = (EditText) findViewById(R.id.schoolName);

        String schoolName = text.getEditableText().toString();
        if (schoolName.length() <= 3)
            return;
        Log.i("schoolName", schoolName);
        preferences.edit().putBoolean("firstStart", false).apply();
        preferences.edit().putString("schoolURL", schoolName).apply();
        addSchoolToList(schoolName);

        enableSite();
        //loadWebsite(true);
    }

    void loadWebsite(boolean firstTime) {
        Log.i("debug", "loadWebsite");
        String url = "https://" + getUrl() + ".magister.net/";

        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 12)
            CookieManager.setAcceptFileSchemeCookies(true);
        CookieSyncManager.getInstance().startSync();

        mWebView.setWebViewClient(new MyAppWebViewClient(this));
        mWebView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        //webSettings.setSupportMultipleWindows(true);
        webSettings.setAppCacheMaxSize(1024 * 1024 * 8);
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        //TODO: Better cache function
        //webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        //webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }

        if ( !isNetworkAvailable() ) { // loading offline
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            mWebView.loadUrl("https://" + getUrl() + ".magister.net/leerling/#/agenda");
            Toast.makeText(getApplicationContext(), "Offline modus, niet alle functies zijn beschikbaar", Toast.LENGTH_LONG).show();
            return;
        }

        if (firstTime) {
            mWebView.clearCache(true);
        }

        mWebView.loadUrl(url);
        //mWebView.loadUrl("http://google.nl/");
    }

    public static String getUrl() {
        return preferences.getString("url", "segbroek");
    }

    public void checkFistStart() {
        Log.i("debug", "checkFirstStart");
        boolean firstStart = preferences.getBoolean("firstStart", true);
        if (firstStart)
            setContentView(R.layout.activity_welcome);
        else {
            enableSite();
        }
    }

    void addSchool() {
        Log.i("debug", "addSchool");
        Builder alert = new Builder(this);

        alert.setTitle("Wat is je magister website?");
        alert.setMessage("____.magister.net");

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Toevoegen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.length() <= 1) {
                    Toast.makeText(getApplicationContext(), "Je moet een naam invullen", Toast.LENGTH_LONG).show();
                    addSchool();
                    return;
                }
                addSchoolToList(value);
                preferences.edit().putString("url", value).apply();
                preferences.edit().putBoolean("setUrl", true).apply();

                loadWebsite(false);
            }
        });

        alert.setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!preferences.getBoolean("setUrl", false)) {
                    Toast.makeText(getApplicationContext(), "Je moet een naam invullen", Toast.LENGTH_LONG).show();
                    addSchool();
                }
            }
        });

        alert.show();
    }

    void changeSchool() {
        Log.i("debug", "changeSchool");
        Set names = preferences.getStringSet("scholen", new HashSet<String>());
        //Set names = new HashSet<String>();

        String schools = android.text.TextUtils.join(",", names.toArray());
        String[] types = schools.split(",");

        final List<String> mapFromSet = new ArrayList<String>();
        Collections.addAll(mapFromSet, types);

        Builder b = new Builder(this);
        b.setTitle("Kies de school");
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), mapFromSet.get(which), Toast.LENGTH_LONG).show();
                preferences.edit().putString("url", mapFromSet.get(which)).apply();
                loadWebsite(false);
                dialog.dismiss();
            }

        });

        b.show();
    }

    void giveVoteOption() {
        Log.i("debug", "giveVoteOption");
        AppRater appRater = new AppRater(this);
        appRater.setDaysBeforePrompt(3);
        appRater.setLaunchesBeforePrompt(7);

        appRater.setPhrases("Waardeer deze app", "We zouden het erg leuk vinden als je de app waardeerd op Google Play, Bedankt voor je support!", "Waardeer", "Later", "Nee bedankt");
        appRater.setTargetUri("https://play.google.com/store/apps/details?id=me.thomasvt.magisterclient");
        appRater.setPreferenceKeys("app_rater", "flag_dont_show", "launch_count", "first_launch_time");

        appRater.show();
    }

    void addSchoolToList(String school) {
        Log.i("debug", "addSchoolToList");
        Set names = preferences.getStringSet("scholen", new HashSet<String>());

        String schools = android.text.TextUtils.join(",", names.toArray());
        String[] types = schools.split(",");

        for (String entry : types) {
            if (entry.equalsIgnoreCase(school))
                return;
        }

        names.add(school);
        preferences.edit().putStringSet("scholen", names).apply();
    }

    void deleteSchools() {
        Log.i("debug", "deleteSchools");
        Builder builder = new Builder(this);
// Add the buttons
        builder.setTitle("Weet je zeker dat je de scholen wilt wissen?");
        builder.setMessage("Dit kan niet ongedaan worden gemaakt");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                preferences.edit().putStringSet("scholen", new HashSet<String>()).apply();
                Toast.makeText(getApplicationContext(), "Scholen zijn gewist!", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Nee", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (mWebView == null)
            super.onBackPressed();
        else if (mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.magister, menu);
        return true;
    }

    void clearCache() {
        Log.i("debug", "clearCache");
        mWebView.clearCache(true);
        mWebView.reload();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.R_layout_action_add:
                addSchool();
                return true;
            case R.id.R_layout_action_reload:
                mWebView.reload();
                return true;
            case R.id.R_layout_action_clear_cache:
                clearCache();
                return true;
            case R.id.R_layout_action_change:
                changeSchool();
                return true;
            case R.id.R_layout_action_delete:
                deleteSchools();
                return true;
            case R.id.R_layout_action_hide:
                hideMenu();
                return true;

        }
        return false;
    }

    void hideMenu() {
        Log.i("debug", "hideMenu");
        Builder builder = new Builder(this);
// Add the buttons
        builder.setTitle("Weet je zeker dat je de menubalk wilt verbergen?");
        builder.setMessage("Dit kan niet ongedaan worden gemaakt. tenzij je de app reset");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                preferences.edit().putBoolean("hidemenu", true).apply();
                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
        builder.setNegativeButton("Nee", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}