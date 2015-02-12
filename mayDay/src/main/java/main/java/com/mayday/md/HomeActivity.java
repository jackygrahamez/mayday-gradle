package com.mayday.md;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mayday.md.common.AppConstants;
import com.mayday.md.common.ApplicationSettings;
import com.mayday.md.data.PBDatabase;
import com.mayday.md.model.Page;
import com.mayday.md.trigger.GearFitTrigger;
import com.mayday.md.trigger.HardwareTriggerReceiver;
import com.mayday.md.trigger.HardwareTriggerService;
import com.mayday.md.GearFitDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.samsung.android.sdk.cup.ScupDialog;
import com.samsung.android.sdk.cup.ScupLabel;

public class HomeActivity extends Activity {

    String[] NAMES = {"MayDay"};
    private GearFitDialog mHelloCupDialog = null;
    private static final int MAYDAY_CUP = 0;
    public static final String CUSTOM_INTENT = "android.intent.action.SCREEN_ON";

    ProgressDialog pDialog;

    String pageId;
    String selectedLang;
//    String mobileDataUrl;
//    String helpDataUrl;

    int lastUpdatedVersion;
    int latestVersion;
//    long lastRunTimeInMillis;
    int lastLocalDBVersion;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);
        Context context = getApplicationContext();
        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        HardwareTriggerReceiver hardwareTriggerReceiver = new HardwareTriggerReceiver();
        GearFitTrigger gearFitTrigger = new GearFitTrigger();

        Bundle bundle=getIntent().getExtras();
        boolean startedByCUP=false;

        gearFitTrigger.GearFitTrigger(bundle, context, mPref);

        //deleteShortCut();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, NAMES);
        ListView mListView = (ListView) findViewById(R.id.demo_list);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == MAYDAY_CUP) {
                    if (mHelloCupDialog == null) {
                        mHelloCupDialog = new GearFitDialog(getApplicationContext());
                    } else {
                        mHelloCupDialog.finish();
                        mHelloCupDialog = null;
                    }
                }
            }
        });

        latestVersion = -1;
        lastUpdatedVersion = ApplicationSettings.getLastUpdatedVersion(HomeActivity.this);

        int wizardState = ApplicationSettings.getWizardState(this);
        if (AppConstants.SKIP_WIZARD) {
            pageId = "home-ready";
            Log.e(">>>>>>", "Checking Alert State");
            if (ApplicationSettings.isAlertActive(HomeActivity.this)) {
                Log.e(">>>>>>", "HomeAlerting");
                pageId = "home-alerting";
            } else {
                Log.e(">>>>>>", "HomeReady");
                pageId = "home-ready";
            }
        } else
        if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED) {
            pageId = "home-not-configured";
        } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_ALARM) {
            pageId = "home-not-configured-alarm";
        } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_DISGUISE) {
            pageId = "home-not-configured-disguise";
        } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_READY) {
            pageId = "home-ready";
        }

        selectedLang = ApplicationSettings.getSelectedLanguage(this);
//        helpDataUrl = AppConstants.BASE_URL + AppConstants.HELP_DATA_URL;

//        lastRunTimeInMillis = ApplicationSettings.getLastRunTimeInMillis(this);

        /*
        lastLocalDBVersion is used for local db version update. If local db version is changed, then all local data will be deleted,
        tables will be reformed & database is blank. So at that point we will force local-data update from assets, then a retrieval-try
        from the remote database even if the data was retrieved within last 24-hours period.
         */
        lastLocalDBVersion = ApplicationSettings.getLastUpdatedDBVersion(this);
        if(lastLocalDBVersion < AppConstants.DATABASE_VERSION){
            Log.e("<<<<<", "local db version changed. needs a force update");
            ApplicationSettings.setLocalDataInsertion(this, false);
//            lastRunTimeInMillis = -1;
        }

        if (!ApplicationSettings.getLocalDataInsertion(HomeActivity.this)) {
            Log.e("???????", "Initializing local data");
            new InitializeLocalData().execute();
        }
//        else if (!AppUtil.isToday(lastRunTimeInMillis) && AppUtil.hasInternet(HomeActivity.this)) {
//            Log.e(">>>>", "local data initialized but last run not today");
//            new GetLatestVersion().execute();
//        }
        else{
            Log.e(">>>>>", "no update needed");
            startNextActivity();
        }
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
//    	AppUtil.unbindDrawables(getWindow().getDecorView().findViewById(android.R.id.content));
//        System.gc();
    }


    private void deleteShortCut() {

        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName("com.mayday.md", "HomeActivity");
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent removeIntent = new Intent();
        removeIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        removeIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "ShortcutName");
        removeIntent.putExtra("duplicate", false);

        removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        sendBroadcast(removeIntent);
    }


    private void startNextActivity(){
        Log.e(">>>>>>>>>>>>", "starting next activity");

        int wizardState = ApplicationSettings.getWizardState(this);
        if (true) {
            Log.e(">>>>>>", "first run TRUE, running WizardActivity with pageId = " + pageId);
            Intent i = new Intent(HomeActivity.this, WizardActivity.class);
            // Removing default homescreen shortcut when installed via Google Play.
            /*i.setAction(Intent.ACTION_MAIN);
            
            i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            i.putExtra(Intent.EXTRA_SHORTCUT_NAME, "HelloWorldShortcut");
         
            i.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            getApplicationContext().sendBroadcast(i);

                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
//                        i = AppUtil.clearBackStack(i);
                        if (ApplicationSettings.isAlertActive(LoginActivity.this)) {
                            i.putExtra("page_id", "home-alerting");
                        } else {
                            i.putExtra("page_id", "home-ready");
                        }
            */


            i.putExtra("page_id", pageId);
            startActivity(i);
        } else {
            Log.e(">>>>>>", "first run FALSE, running CalculatorActivity");
            Intent i = new Intent(HomeActivity.this, CalculatorActivity.class);
            // Make sure the HardwareTriggerService is started
    		startService(new Intent(this, HardwareTriggerService.class));
            startActivity(i);
        }
    }


//    private void startWizard() {
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                Intent i = new Intent(HomeActivity.this, WizardActivity.class);
//                i = AppUtil.clearBackStack(i);
//                i.putExtra("page_id", pageId);
//                startActivity(i);
//            }
//        }, AppConstants.SPLASH_DELAY_TIME);
//    }

    private class InitializeLocalData extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = ProgressDialog.show(HomeActivity.this, "Application", "Installing...", true, false);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_en.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            lastUpdatedVersion = mobileObj.getInt("version");
	            ApplicationSettings.setLastUpdatedVersion(HomeActivity.this, lastUpdatedVersion);

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_es.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_ph.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_fr.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_pt.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("mobile_de.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("mobile");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_en.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            lastUpdatedVersion = mobileObj.getInt("version");
	            ApplicationSettings.setLastUpdatedVersion(HomeActivity.this, lastUpdatedVersion);

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_es.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_ph.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_fr.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_pt.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        try {
	            JSONObject jsonObj = new JSONObject(loadJSONFromAsset("help_de.json"));
	            JSONObject mobileObj = jsonObj.getJSONObject("help");

	            JSONArray dataArray = mobileObj.getJSONArray("data");
	            insertMobileDataToLocalDB(dataArray);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }

	        return true;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            super.onPostExecute(response);
            if (pDialog.isShowing())
				try {
					pDialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}

            ApplicationSettings.setLocalDataInsertion(HomeActivity.this, true);
            ApplicationSettings.setLastUpdatedDBVersion(HomeActivity.this, AppConstants.DATABASE_VERSION);

            startNextActivity();

//            if (!AppUtil.isToday(lastRunTimeInMillis) && AppUtil.hasInternet(HomeActivity.this)) {
//                Log.e(">>>>", "last run not today");
//                new GetLatestVersion().execute();
//            } else{
//                startNextActivity();
//            }
        }
    }
    
//    private class GetLatestVersion extends AsyncTask<Void, Void, Boolean> {
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            pDialog = ProgressDialog.show(HomeActivity.this, "Application", "Starting...", true, false);
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//
//            String url = AppConstants.BASE_URL + AppConstants.VERSION_CHECK_URL;
//            JsonParser jsonParser = new JsonParser();
//            ServerResponse response = jsonParser.retrieveServerData(AppConstants.HTTP_REQUEST_TYPE_GET, url, null, null, null);
//            if (response.getStatus() == 200) {
//                try {
//                    JSONObject responseObj = response.getjObj();
//                    latestVersion = responseObj.getInt("version");
//                    Log.e("??????", "latest version = " + latestVersion + " last updated version = " + lastUpdatedVersion);
//                    return true;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//            return false;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean response) {
//            super.onPostExecute(response);
//
//            if (latestVersion > lastUpdatedVersion) {
//                new GetMobileDataUpdate().execute();
//            } else {
//                ApplicationSettings.setLastRunTimeInMillis(HomeActivity.this, System.currentTimeMillis());
//                if (pDialog.isShowing())
//					try {
//						pDialog.dismiss();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//                    startNextActivity();
//            }
//        }
//    }



//    private class GetMobileDataUpdate extends AsyncTask<Void, Void, Boolean> {
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            try {
//				pDialog = ProgressDialog.show(HomeActivity.this, "Application", "Downloading updates...", true, false);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//       }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//
//            int version = 0;
//            for(version = lastUpdatedVersion + 1; version <= latestVersion; version ++){
//                if (selectedLang.equals("en")) {
//                    mobileDataUrl = AppConstants.BASE_URL + "/api/mobile." + version + ".json";
//                } else {
//                    mobileDataUrl = AppConstants.BASE_URL + "/api/" + selectedLang + "/" + "mobile." + version + ".json";
//                }
//
//                JsonParser jsonParser = new JsonParser();
//                ServerResponse response = jsonParser.retrieveServerData(AppConstants.HTTP_REQUEST_TYPE_GET, mobileDataUrl, null, null, null);
//                if (response.getStatus() == 200) {
//                    Log.d(">>>><<<<", "success in retrieving server-response for url = " + mobileDataUrl);
//                    try {
//                        JSONObject responseObj = response.getjObj();
//                        JSONObject mobObj = responseObj.getJSONObject("mobile");
//                        JSONArray dataArray = mobObj.getJSONArray("data");
//                        insertMobileDataToLocalDB(dataArray);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        return false;
//                    }
//                }
//            }
//
//            if(version > latestVersion){
//                return true;
//            } else{
//                return false;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Boolean response) {
//            super.onPostExecute(response);
//
//            if(response){
//                new GetHelpDataUpdate().execute();
//            }
//            else{
//                if (pDialog.isShowing())
//                    pDialog.dismiss();
//
//                startNextActivity();
//            }
//        }
//    }




//    private class GetHelpDataUpdate extends AsyncTask<Void, Void, Boolean> {
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            try {
//				pDialog = ProgressDialog.show(HomeActivity.this, "Application", "Downloading help pages...", true, false);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//
//            JsonParser jsonParser = new JsonParser();
//            ServerResponse response = jsonParser.retrieveServerData(AppConstants.HTTP_REQUEST_TYPE_GET, helpDataUrl, null, null, null);
//            if (response.getStatus() == 200) {
//                Log.d(">>>><<<<", "success in retrieving server-response for url = " + helpDataUrl);
//                ApplicationSettings.setLastRunTimeInMillis(HomeActivity.this, System.currentTimeMillis());          // if we can retrieve a single data, we change it up-to-date
//                try {
//                    JSONObject responseObj = response.getjObj();
//                    JSONObject mobObj = responseObj.getJSONObject("help");
//                    JSONArray dataArray = mobObj.getJSONArray("data");
//                    insertHelpDataToLocalDB(dataArray);
//                    ApplicationSettings.setLastUpdatedVersion(HomeActivity.this, latestVersion);
//                    return true;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//            return false;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean response) {
//            super.onPostExecute(response);
//            if (pDialog.isShowing())
//				try {
//					pDialog.dismiss();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//            startNextActivity();
//        }
//    }



//    private void insertHelpDataToLocalDB(JSONArray dataArray) {
//        List<HelpPage> pageList = HelpPage.parseHelpPages(dataArray);
//
//        PBDatabase dbInstance = new PBDatabase(HomeActivity.this);
//        dbInstance.open();
//
//        for (int i = 0; i < pageList.size(); i++) {
//            dbInstance.insertOrUpdateHelpPage(pageList.get(i));
//        }
//        dbInstance.close();
//    }


    private void insertMobileDataToLocalDB(JSONArray dataArray) {
        List<Page> pageList = Page.parsePages(dataArray);

        PBDatabase dbInstance = new PBDatabase(HomeActivity.this);
        dbInstance.open();

        for (int i = 0; i < pageList.size(); i++) {
            dbInstance.insertOrUpdatePage(pageList.get(i));
        }
        dbInstance.close();
    }

    public String loadJSONFromAsset(String jsonFileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(jsonFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

}