package edu.haverford.mpp.mappingprogressivephiladelphia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;


public class FacebookLogin extends Activity {


    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private Realm realm;
    int i = 0;



    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) { // There are no active networks
            return false;
        } else
            return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        realm = Realm.getInstance(this);
        if(!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(this.getApplicationContext());
        }

        setContentView(R.layout.activity_facebook_login);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        List<String> permissionNeeds = Arrays.asList("user_photos", "email", "user_birthday", "public_profile");
        Boolean isLoggedIntoFB = getSharedPreferences("PREFERENCES", MODE_PRIVATE).getBoolean("isLoggedIntoFB", false);
        loginButton.setReadPermissions(permissionNeeds);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {

                TextView view = (TextView) findViewById(R.id.login_text);
                view.setText("You are logged into Facebook.");

                getSharedPreferences("PREFERENCES", MODE_PRIVATE).edit().putBoolean("isLoggedIntoFB", true).apply();
                Bundle parameter = new Bundle();

                Date currentDate = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(currentDate);
                //Months start at 0? Why is currentMonth = 4 by default
                int currentMonth = cal.get(Calendar.MONTH)+1;
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                RealmQuery<OrgEvent> query = realm.where(OrgEvent.class);
                RealmResults<OrgEvent> result = query.findAll();
                if (result.size() > 0){
                    parameter.putString("fields", "id,name,link");
                    for (int i = 0; i< result.size()-2; i++){
                        //I can't figure out why this should be -2, not -1, but it works
                        if (Long.parseLong(result.get(i).getFacebookID()) != 0) {
                            queryFacebookEvent(result.get(i).getFacebookID(), loginResult, currentMonth, currentYear, currentDay);
                        }}}
            }
            @Override
            public void onCancel() {
                System.out.println("onCancel");
                TextView view = (TextView) findViewById(R.id.login_text);
                view.setText("You canceled the login, please try again.");
            }
            @Override
            public void onError(FacebookException exception) {
                Log.v("LoginActivity", exception.getCause().toString());
                TextView view = (TextView) findViewById(R.id.login_text);
                view.setText("Error logging in, please try again.");
            }});
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        // map
        MenuItem map = menu.findItem(R.id.map);
        map.setEnabled(true);
        map.getIcon().setAlpha(255);

        // swipe
        MenuItem swipe = menu.findItem(R.id.swipe);
        swipe.setEnabled(true);
        swipe.getIcon().setAlpha(255);

        // slider
        MenuItem slider = menu.findItem(R.id.slider);
        slider.setEnabled(true);
        slider.getIcon().setAlpha(255);
        /*
        // orgs
        MenuItem orgs = menu.findItem(R.id.go_orgs);
        orgs.setVisible(false);

        // events
        MenuItem events = menu.findItem(R.id.go_events);
        events.setVisible(false);
        */
        return true;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_facebook_login, menu); // Inflate the menu; this adds items to the action bar if it is present
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // handle action bar selections
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return (true);
            case R.id.map:
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(intent);
                break;
            case R.id.swipe:
                intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                startActivity(intent);
                break;
            case R.id.slider:
                intent = new Intent(getApplicationContext(), ScreenSlideActivity.class);
                startActivity(intent);
                break;
            case R.id.help:
                getMapHelp();
        }

        return(super.onOptionsItemSelected(item));
    }

    public void getMapHelp() {
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle("Welcome to Philly Activists and Volunteers Exchange (PAVE)!")
                .setMessage(R.string.dialogMessage)
                .setPositiveButton("Subscribe Now", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Subscribe Later", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    public void queryFacebookEvent(String facebookid, final LoginResult login, int month, int year, int day){

        final int CurrentMonth = month;
        final int CurrentYear = year;
        final int CurrentDay = day;

        final String fbid = facebookid;
        String graphpath = facebookid+"/events";
        Bundle parameter = new Bundle();
        parameter.putString("fields", "id,name,link");
        GraphRequest r = GraphRequest.newGraphPathRequest(login.getAccessToken(), graphpath, new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse graphResponse) {
                //GraphResponse contains the data we asked for from Facebook, as well as a response code

                JSONObject query_result = graphResponse.getJSONObject();
                try {
                    //Taking the JSONArray that contains the response code + raw data,
                    // and getting an array of just the data
                    if (query_result.getJSONArray("data") != null){
                        JSONArray query_data = query_result.getJSONArray("data");
                        //Querying for the first (which is the most recent) event
                        final JSONObject event = query_data.getJSONObject(0);
                        //event is a JSON object containing (in order) name, start_time, end_time, timezone, location, id.
                        //This ID is a separate event_ID that must be queried to get event description

                        realm = Realm.getInstance(getApplicationContext());
                        RealmQuery<OrgEvent> query = realm.where(OrgEvent.class);
                        query.equalTo("facebookID", fbid);
                        RealmResults<OrgEvent> result = query.findAll();
                        if (result.size() > 0 & event != null) {
                            OrgEvent currentOrg = result.first();
                            String date = event.get("start_time").toString();

                            int event_year = Integer.valueOf(date.substring(0, 4));
                            int event_month = Integer.valueOf(date.substring(5, 7));
                            int event_day = Integer.valueOf(date.substring(8, 10));
                            //Wow this code is ugly
                            //But should only add UPCOMING FB events.
                            if (event_year>=CurrentYear & event_month>CurrentMonth || event_year>=CurrentYear & event_month>=CurrentMonth & event_day>=CurrentDay) {
                                realm.beginTransaction();

                                currentOrg.setEventName(event.get("name").toString());
                                currentOrg.setEventID(event.get("id").toString());
                                currentOrg.setStartTime(event.get("start_time").toString());
                                realm.commitTransaction();

                            //Querying again using the eventid to get an event description
                            //Cannot spin off into a separate method, caused problems with nested classes
                            GraphRequest getDescription = GraphRequest.newGraphPathRequest(login.getAccessToken(), event.get("id").toString(), new GraphRequest.Callback() {
                                JSONObject query_result;

                                @Override
                                public void onCompleted(GraphResponse graphResponse) {
                                    query_result = graphResponse.getJSONObject();
                                    try {
                                        String description = query_result.get("description").toString();
                                        RealmQuery<OrgEvent> query = realm.where(OrgEvent.class);
                                        query.equalTo("eventID", event.get("id").toString());
                                        RealmResults<OrgEvent> result = query.findAll();
                                        OrgEvent currentOrg = result.first();
                                        realm.beginTransaction();
                                        currentOrg.setEventDescription(description);
                                        realm.commitTransaction();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        TextView view = (TextView) findViewById(R.id.login_text);
                                        view.setText("Error in event description call, please try logging in again. ");
                                    }
                                }
                            });
                            getDescription.executeAsync();
                        }}
                        else{
                            return;
                        }
                    }




                } catch (JSONException e) {
                    //TextView view = (TextView) findViewById(R.id.login_text);
                    //view.setText("Please check your internet connection and try logging in again.");
                    e.printStackTrace();
                }}});
        r.executeAsync();
    }
}
