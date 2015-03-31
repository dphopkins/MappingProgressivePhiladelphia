package edu.haverford.mpp.mappingprogressivephiladelphia;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Map;

public class MapActivity extends FragmentActivity {

    int x = 1;
    boolean y = (x==1) ? true : false;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFirstRun();
        setContentView(R.layout.activity_map);
        Log.w("TAG", "Play services configured: " + Boolean.toString(isPlayServicesConfigured()));
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            Log.e("TAG", "Failed to initialize map");
        }

        setUpMapIfNeeded();

        getActionBar().setDisplayHomeAsUpEnabled(false);

        /**
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria,true);
        Location location = locationManager.getLastKnownLocation(provider);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude); */

        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));

        // This zooms, the above one does not. I think the zooming looks kind of distracting, especially if it happens every time.
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

        // Move the camera instantly to Philadelphia with a zoom of 12.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.952595,-75.163736), 12)); //Town Center Philadelphia
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        final SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(MapActivity.this);

        MyDatabase db = new MyDatabase(getApplicationContext());
        ArrayList<Integer> subbedOrgIDs = db.getAllSubscribedOrgIDs();
        PhillyOrg currentOrg = new PhillyOrg();

        for (int i = 0; i < subbedOrgIDs.size(); i++) {
            currentOrg = db.getOrganizationById(subbedOrgIDs.get(i));
            mMap.addMarker(new MarkerOptions().position(new LatLng(currentOrg.getLatitude(), currentOrg.getLongitude())).title(currentOrg.getGroupName()));
        }
        mMap.setMyLocationEnabled(true);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {

        //map
        MenuItem map = menu.findItem(R.id.map);
        map.setEnabled(false);
        map.getIcon().setAlpha(100);

        //swipe
        MenuItem swipe = menu.findItem(R.id.swipe);
        swipe.setEnabled(true);
        swipe.getIcon().setAlpha(255);

        //list
        MenuItem list = menu.findItem(R.id.list);
        list.setEnabled(true);
        list.getIcon().setAlpha(255);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options, menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                // finish();
                return true;
            case R.id.swipe:
                Intent intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                startActivity(intent);
                break;
            case R.id.list:
                intent = new Intent(getApplicationContext(), OrgListActivity.class);
                startActivity(intent);
                break;
            case R.id.help:
                getMapHelp();
                break;
        }
        return(super.onOptionsItemSelected(item));
    }


    private boolean isPlayServicesConfigured() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(MapActivity.this);
        if(status == ConnectionResult.SUCCESS)
            return true;
        else {
            Log.d("STATUS", "Error connecting with Google Play services. Code: " + String.valueOf(status));
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, MapActivity.this, status);
            dialog.show();
            return false;
        }
    }

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);

        if (isFirstRun) {
            new AlertDialog.Builder(this, R.style.DialogTheme)
                    .setTitle("Welcome to Mapping Progressive Philadelphia!")
                    .setMessage("This app is made up of three parts: Swipes, Map, and List." + "\n" + "\n"
                            + "Swipes offers a Tinder-style interface for choosing organizations that " +
                            "you'd like to subscribe to and learn more about. Just swipe the cards left " +
                            "(no thanks!) or right (sign me up!)." + "\n" + "\n" + "Subscribing to an organization " +
                            "simply allows the app to notify you when there's an event going on and places " +
                            "that organization on your personal map of Progressive Philadelphia." + "\n" + "\n" +
                            "Map displays all of the organizations you're subscribed to and allows you to click" +
                            " on a point to get more information about that organization." + "\n" + "\n" + "List is " +
                            "another way to choose organizations to subscribe to, using a more conventional design " +
                            "if Swipes just isn't your style." + "\n" + "\n" + "Click 'Subscribe Now' to get started with " +
                            "Swipes or 'Subscribe Later' to head over to the Map. You can always review this information " +
                            "again by clicking on the Help button in the overflow menu.")
                            // add  + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "hello" to see that scroll works
                    .setPositiveButton("Subscribe Now", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Subscribe Later", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(R.drawable.ic_launcher)
                    .show();
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun", false).apply();
        }
    }

    public void getMapHelp() {
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle("Welcome to Mapping Progressive Philadelphia!")
                .setMessage("This app is made up of three parts: Swipes, Map, and List." + "\n" + "\n"
                        + "Swipes offers a Tinder-style interface for choosing organizations that " +
                        "you'd like to subscribe to and learn more about. Just swipe the cards left " +
                        "(no thanks!) or right (sign me up!)." + "\n" + "\n" + "Subscribing to an organization " +
                        "simply allows the app to notify you when there's an event going on and places " +
                        "that organization on your personal map of Progressive Philadelphia." + "\n" + "\n" +
                        "Map displays all of the organizations you're subscribed to and allows you to click" +
                        " on a point to get more information about that organization." + "\n" + "\n" + "List is " +
                        "another way to choose organizations to subscribe to, using a more conventional design " +
                        "if Swipes just isn't your style." + "\n" + "\n" + "Click 'Subscribe Now' to get started with " +
                        "Swipes or 'Subscribe Later' to head over to the Map. You can always review this information " +
                        "again by clicking on the Help button in the overflow menu.")
                .setPositiveButton("Subscribe Now", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Subscribe Later", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(R.drawable.ic_launcher)
                .show();
    }
}