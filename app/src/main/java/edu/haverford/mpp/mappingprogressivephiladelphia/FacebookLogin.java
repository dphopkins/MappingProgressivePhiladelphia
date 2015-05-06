package edu.haverford.mpp.mappingprogressivephiladelphia;

import android.app.Activity;
import android.content.Intent;
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
import java.util.List;

import io.realm.Realm;


public class FacebookLogin extends Activity {


    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private Realm realm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getInstance(this);


        //FacebookSdk.sdkInitialize(this.getApplicationContext());

        if(!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(this.getApplicationContext());
        }

        setContentView(R.layout.activity_facebook_login);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        List<String> permissionNeeds = Arrays.asList("user_photos", "email", "user_birthday", "public_profile");
        loginButton.setReadPermissions(permissionNeeds);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {

                String gp = "/61159665895/events";
                String test = "579972348728258";
                Bundle parameter = new Bundle();
                parameter.putString("fields", "id,name,link");
                queryFacebookEvent(gp, loginResult);


            }
            @Override
            public void onCancel() {
                System.out.println("onCancel");
                TextView view = (TextView) findViewById(R.id.login_text);
                view.setText("You canceled the login");
            }
            @Override
            public void onError(FacebookException exception) {
                Log.v("LoginActivity", exception.getCause().toString());
                TextView view = (TextView) findViewById(R.id.login_text);
                view.setText("Error logging in");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_facebook_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void queryFacebookEvent(String facebookid, final LoginResult login){


        String graphpath = facebookid+"/events";
        Bundle parameter = new Bundle();
        parameter.putString("fields", "id,name,link");
        GraphRequest r = GraphRequest.newGraphPathRequest(login.getAccessToken(), graphpath, new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse graphResponse) {
                        //GraphResponse contains the data we asked for from Facebook, as well as a response code

                        JSONObject query_result = graphResponse.getJSONObject();
                        TextView view = (TextView) findViewById(R.id.login_text);
                        try {
                            //Taking the JSONArray that contains the response code + raw data,
                            // and getting an array of just the data
                            JSONArray query_data = query_result.getJSONArray("data");
                            //Querying for the first (which is the most recent) event
                            JSONObject event = query_data.getJSONObject(0);
                            //event is a JSON object containing (in order) name, start_time, end_time, timezone, location, id.
                            //This ID is a separate event_ID that must be queried to get event description


                            //Querying again using the eventid to get an event description
                            //Cannot spin off into a separate method, caused problems with nested classes
                            GraphRequest getDescription = GraphRequest.newGraphPathRequest(login.getAccessToken(), event.get("id").toString(), new GraphRequest.Callback() {
                                JSONObject query_result;

                                @Override
                                public void onCompleted(GraphResponse graphResponse) {
                                    query_result = graphResponse.getJSONObject();
                                    try {
                                        String description = query_result.get("description").toString();
                                        /*
                                        realm.beginTransaction();

                                        realm_event.setEventDescription(description);
                                        realm.commitTransaction();
                                        */
                                        TextView view = (TextView) findViewById(R.id.login_text);
                                        view.setText(description);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        TextView view = (TextView) findViewById(R.id.login_text);
                                        view.setText("Error in event description call");
                                    }
                                }
                            });
                            getDescription.executeAsync();


                            //realm_event.setEventName((String) event.get("name"));
                            //realm_event.setStartTime("start");


                        } catch (JSONException e) {
                            view.setText(graphResponse.toString());
                            view.setText("Error in list of event call");
                            e.printStackTrace();
                        }}});
        r.executeAsync();
    }
}
