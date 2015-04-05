package me.thomasvt.magisterclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.thomasvt.magisterclient.db.School;
import me.thomasvt.magisterclient.db.SchoolDatabase;
import me.thomasvt.magisterclient.db.SchoolDatabaseHelper;

public class SchoolSelector extends Activity {
    private ListView mListView;
    private EditText mEditText;
    private SharedPreferences mPreferences;
    private SchoolDatabaseHelper mDatabase;
    private static final String SCHOOL_LIST_URL = "https://raw.githubusercontent.com/Tobiaqs/MagisterSchools/master/i-like-trains.txt";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_selector);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Tobias: Dit zorgt ervoor dat de "home"-knop op de actionbar alleen zichtbaar is als de parent activity
        // daadwerkelijk nog open is. Dit is alleen zo als er al een URL is ingesteld.
        getActionBar().setDisplayHomeAsUpEnabled(mPreferences.getString(Magister.PREF_HOST, null) != null);

        mListView = (ListView) findViewById(R.id.school_list);
        mEditText = (EditText) findViewById(R.id.school_filter);

        mDatabase = new SchoolDatabaseHelper(this);

        List<School> schoolList = mDatabase.getSchools();
        if(schoolList.size() > 0 && System.currentTimeMillis() - mPreferences.getLong(Magister.PREF_LAST_SCHOOL_LIST_UPDATE, 0) < DateUtils.DAY_IN_MILLIS) {
            setupList(schoolList);
        }
        else {
            new SchoolListDownloader().execute();
        }
    }

    protected void onDestroy() {
        mDatabase.close();
        super.onDestroy();
    }

    class SchoolListDownloader extends AsyncTask<Void, Void, Object> {
        final Pattern SCHOOL_PATTERN = Pattern.compile("^([^;]+);(.+)$", Pattern.MULTILINE);
        ProgressDialog mProgressDialog;

        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(SchoolSelector.this, getText(R.string.loading_schools), getText(R.string.please_wait));
        }

        protected Object doInBackground(Void... params) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(SCHOOL_LIST_URL).openConnection();
                String html = Utils.convertStream(connection.getInputStream());
                Matcher matcher = SCHOOL_PATTERN.matcher(html);
                List<School> schoolList = new ArrayList<>();
                while(matcher.find()) {
                    schoolList.add(new School(matcher.group(2), matcher.group(1)));
                }

                if(schoolList.size() == 0)
                    return new RuntimeException(getString(R.string.error_no_schools));
                else {
                    copyFavourites(schoolList);
                    mDatabase.setSchools(schoolList);
                    mPreferences.edit().putLong(Magister.PREF_LAST_SCHOOL_LIST_UPDATE, System.currentTimeMillis()).apply();
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

    private void setupList(final List<School> schoolList) {
        Utils.sortSchoolList(schoolList);
        final SchoolAdapter adapter = new SchoolAdapter(SchoolSelector.this, schoolList);
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
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    class SchoolAdapter extends ArrayAdapter<School> {
        public SchoolAdapter(Context context, List<School> schoolList) {
            super(context, R.layout.row_school, schoolList);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder viewHolder;
            if(view == null) {
                view = getLayoutInflater().inflate(R.layout.row_school, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(android.R.id.text1);
                viewHolder.button = (ImageView) view.findViewById(R.id.favourite);
                view.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final School school = getItem(position);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(SchoolSelector.this);
                    mPreferences.edit().putString(Magister.PREF_HOST, school.host).apply();

                    finish();
                    startActivity(new Intent(SchoolSelector.this, Magister.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
            });

            viewHolder.text.setText(school.name);
            viewHolder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    DialogInterface.OnClickListener toggleFavourite = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            // Tobias: indien de listener aangeroepen wordt via AlertDialog
                            // is dialogInterface niet null.
                            if(dialogInterface != null) {
                                mPreferences.edit().putBoolean(Magister.PREF_FAVOURITE_INFO_SHOWN, true).apply();
                            }
                            school.favourite = !school.favourite;
                            mDatabase.setFavourite(school.id, school.favourite);
                            ((ImageView) view).setImageResource(school.favourite ? R.drawable.ic_action_important_on : R.drawable.ic_action_important);
                        }
                    };

                    if(!school.favourite && !mPreferences.getBoolean(Magister.PREF_FAVOURITE_INFO_SHOWN, false)) {
                        new AlertDialog.Builder(SchoolSelector.this)
                            .setTitle(R.string.add_favourite)
                            .setMessage(R.string.info_first_favourite)
                            .setPositiveButton(R.string.yes, toggleFavourite)
                            .setNegativeButton(R.string.no, null)
                            .show();
                    }
                    else {
                        toggleFavourite.onClick(null, 0);
                    }
                }
            });
            viewHolder.button.setImageResource(school.favourite ? R.drawable.ic_action_important_on : R.drawable.ic_action_important);
            return view;
        }

        class ViewHolder {
            TextView text;
            ImageView button;
        }
    }

    private void copyFavourites(List<School> schoolList) {
        Cursor cursor = mDatabase.raw().query(SchoolDatabase.TABLE_SCHOOLS, new String[] { SchoolDatabase.Fields.NAME }, SchoolDatabase.Fields.FAVOURITE + " > 0", null, null, null, null);
        List<String> nameList = new ArrayList<>();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            nameList.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();

        for(int i = 0; i < schoolList.size(); i++) {
            School school = schoolList.get(i);
            school.favourite = nameList.contains(school.name);
        }
    }
}
