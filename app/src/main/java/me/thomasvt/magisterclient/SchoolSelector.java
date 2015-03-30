package me.thomasvt.magisterclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class SchoolSelector extends Activity {
    private ListView mListView;
    private EditText mEditText;
    private File mSchoolListCache;
    private SharedPreferences mPreferences;

    private final static long SCHOOL_LIST_CACHE_MAX_AGE = 7 * DateUtils.DAY_IN_MILLIS;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_selector);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Tobias: Dit zorgt ervoor dat de "home"-knop op de actionbar alleen zichtbaar is als de parent activity
        // daadwerkelijk nog open is. Dit is alleen zo als er al een URL is ingesteld.
        getActionBar().setDisplayHomeAsUpEnabled(mPreferences.getString(Magister.PREF_URL, null) != null);

        mListView = (ListView) findViewById(R.id.school_list);
        mEditText = (EditText) findViewById(R.id.school_filter);
        mSchoolListCache = new File(getFilesDir() + File.separator + "school_list.json");

        if(!mSchoolListCache.exists() || System.currentTimeMillis() - mSchoolListCache.lastModified() > SCHOOL_LIST_CACHE_MAX_AGE) {
            new SchoolListDownloader().execute();
        }
        else {
            try {
                FileInputStream fis = new FileInputStream(mSchoolListCache);
                JSONArray jsonSchoolList = new JSONArray(Utils.convertStream(fis));
                fis.close();
                List<School> schoolList = new ArrayList<School>();
                for(int i = 0; i < jsonSchoolList.length(); i++) {
                    JSONObject jsonSchool = jsonSchoolList.getJSONObject(i);
                    School school = new School();
                    school.name = jsonSchool.getString("name");
                    school.url = jsonSchool.getString("url");
                    schoolList.add(school);
                }
                setupList(schoolList);
            }
            catch (Exception e) {
                new SchoolListDownloader().execute();
            }
        }
    }

    class SchoolListDownloader extends AsyncTask<Void, Void, Object> {
        final Pattern RID_PATTERN = Pattern.compile("rid=([^\"]+)");
        final Pattern SCHOOL_PATTERN = Pattern.compile("<option value=\"([^;]+);[^\"]+\">(.+?)</option>");
        ProgressDialog mProgressDialog;

        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(SchoolSelector.this, getText(R.string.loading_schools), getText(R.string.please_wait));
        }

        protected Object doInBackground(Void... params) {
            HttpsURLConnection connection;
            Scanner scanner;
            String html;
            Matcher matcher;
            try {
                connection = (HttpsURLConnection) new URL("https://selfservice.entree.kennisnet.nl/accountbeheer/").openConnection();
                html = Utils.convertStream(connection.getInputStream());

                matcher = RID_PATTERN.matcher(html);
                String rid;
                if(matcher.find())
                    rid = matcher.group(1);
                else
                    return new RuntimeException(getString(R.string.error_parsing_schools));

                connection = (HttpsURLConnection) new URL("https://aselect.entree.kennisnet.nl/openaselect/sso/web?asid=" + rid + "&requestor=selfservice&remote_idp=https%3A%2F%2Fsiam.swp.nl%2Faselectserver%2Fserver&form_federative_institution=https%3A%2F%2Fsiam.swp.nl%2Faselectserver%2Fserver").openConnection();
                scanner = new Scanner(connection.getInputStream(), "UTF-8");
                scanner.useDelimiter("\\A");
                html = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                List<School> schoolList = new ArrayList<School>();
                matcher = SCHOOL_PATTERN.matcher(html);

                while(matcher.find()) {
                    School school = new School();
                    school.url = matcher.group(1).replace(".swp.nl", ".magister.net");
                    school.name = matcher.group(2);
                    schoolList.add(school);
                }

                if(schoolList.size() == 0)
                    return new RuntimeException(getString(R.string.error_no_schools));
                else {
                    JSONArray jsonSchoolList = new JSONArray();

                    for(School school : schoolList) {
                        JSONObject jsonSchool = new JSONObject();
                        jsonSchool.put("name", school.name);
                        jsonSchool.put("url", school.url);
                        jsonSchoolList.put(jsonSchool);
                    }

                    FileOutputStream fos = new FileOutputStream(mSchoolListCache);
                    fos.write(jsonSchoolList.toString().getBytes("UTF-8"));
                    fos.close();
                    return schoolList;
                }
            } catch (Exception e) {
                return e;
            }
        }

        protected void onPostExecute(final Object result) {
            mProgressDialog.dismiss();

            if(result instanceof List) {
                // Geen problemen.
                setupList((List<School>) result);
            }
            else /* result is dus een Exception */ {
                Exception exception = (Exception) result;
                // Wel een probleempje
                String error;
                if(exception instanceof IOException) {
                    error = getString(R.string.error_network);
                }
                else if(exception instanceof RuntimeException) {
                    error = exception.getMessage();
                }
                else if(exception instanceof JSONException) {
                    error = getString(R.string.error_saving_schools);
                }
                else /* Vreemde Exception, komt waarschijnlijk niet voor maar je weet het nooit */ {
                    error = getString(R.string.error_weird_problem, exception.getClass().getSimpleName() + "\n" + exception.getMessage());
                }

                new AlertDialog.Builder(SchoolSelector.this)
                    .setTitle(R.string.error)
                    .setMessage(error)
                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
            }
        }
    }

    // Data structure voor scholen
    class School {
        String url;
        String name;

        // Compatibiliteit met o.a. ArrayAdapter
        public String toString() {
            return name;
        }
    }

    private void setupList(final List<School> schoolList) {
        final ArrayAdapter<School> adapter = new ArrayAdapter<School>(SchoolSelector.this, android.R.layout.simple_list_item_1, schoolList);
        mEditText.setEnabled(true);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                adapter.getFilter().filter(charSequence);
            }
        });
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Geselecteerde school
                School selectedSchool = (School) adapterView.getItemAtPosition(i);
                SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(SchoolSelector.this);
                mPreferences.edit().putString(Magister.PREF_URL, selectedSchool.url).apply();

                finish();
                startActivity(new Intent(SchoolSelector.this, Magister.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
}
