package edu.haverford.mpp.mappingprogressivephiladelphia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class OrgListActivity extends Activity {

    private OrgListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org_list);
    }

    protected void onResume(){
        super.onResume();
        MyDatabase db = new MyDatabase(this);
        //Array list of organizations
        ArrayList<PhillyOrg> orgList = db.getAllOrganizations();

        //create an ArrayAdaptar from the String Array
        mAdapter = new OrgListAdapter(this,
                R.layout.org_list_item, orgList);
        ListView listView = (ListView) findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(mAdapter);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //map
        MenuItem map = menu.findItem(R.id.map);
        map.setEnabled(true);
        map.getIcon().setAlpha(255);

        //swipe
        MenuItem swipe = menu.findItem(R.id.swipe);
        swipe.setEnabled(true);
        swipe.getIcon().setAlpha(255);

        //list
        MenuItem list = menu.findItem(R.id.list);
        list.setEnabled(false);
        list.getIcon().setAlpha(100);

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
                finish();
                return (true);

            case R.id.swipe:
                Intent intent = new Intent(getApplicationContext(), SwipePickerActivity.class);
                startActivity(intent);
                break;
            case R.id.map:
                intent = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(intent);
                break;
            case R.id.help:
                intent = new Intent(getApplicationContext(), Splash.class);
                SharedPreferences.Editor editor = getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit();
                editor.putBoolean("isFirstRun", true);
                editor.apply();
                startActivity(intent);
                break;
        }
        return (super.onOptionsItemSelected(item));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this,
                "You are now subscribed to " + mAdapter.saveSubscribed() + " organizations",
                Toast.LENGTH_SHORT).show(); //testing the savesubscribed feature
    }


}

class OrgListAdapter extends ArrayAdapter<PhillyOrg> {

    private ArrayList<PhillyOrg> orgList;
    private Context mContext;

    public OrgListAdapter(Context context, int textViewResourceId,
                          ArrayList<PhillyOrg> myOrgList) {
        super(context, textViewResourceId, myOrgList);
        this.orgList = new ArrayList<PhillyOrg>();
        this.orgList.addAll(myOrgList);
        this.mContext = context;
    }

    private class ViewHolder {
        TextView code;
        CheckBox name;

        public void CheckMeOrNot(Boolean bool){
            name.setChecked(bool);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        Log.v("ConvertView", String.valueOf(position));
        MyDatabase db = new MyDatabase(mContext);
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.org_list_item, null);

            holder = new ViewHolder();
            holder.code = (TextView) convertView.findViewById(R.id.code);
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
            PhillyOrg currOrg = orgList.get(position);
            holder.CheckMeOrNot(db.isSubscribed(currOrg.getId()));
            Log.w("TAG", "getSubbed of " + currOrg.toString() + ": " + Boolean.toString(db.isSubscribed(currOrg.getId())));
            convertView.setTag(holder);


            holder.name.setOnClickListener( new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v ;
                    PhillyOrg currOrg = (PhillyOrg) cb.getTag();
                    Boolean subbed = cb.isChecked();
                    currOrg.setSubscribed(subbed);
                    Toast.makeText(mContext,
                            "Clicked on Checkbox: " + cb.getText() +
                                    " is " + currOrg.getSubscribed(),
                            Toast.LENGTH_SHORT).show();
                    MyDatabase db = new MyDatabase(mContext);


                }
            });
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        PhillyOrg currOrg = orgList.get(position);
        holder.code.setText(" (" +  currOrg.getId() + ")");
        holder.name.setText(currOrg.getGroupName());
        //holder.name.setChecked(currOrg.getSubscribed());
        holder.name.setTag(currOrg);

        return convertView;
    }

    /**
     * Saves the state of the checklist to the database.
     * @return the final number of subscribed organizations after saving
     */
    public int saveSubscribed() {
        int counter = 0;
        MyDatabase db = new MyDatabase(mContext);
        ArrayList<PhillyOrg> listToSave = this.orgList;
        for (int i = 0; i < listToSave.size(); i++) {
            PhillyOrg currOrg = listToSave.get(i);
            if (currOrg.getSubscribed()) {
                db.insertSubYes(currOrg.getId());
                counter++;
            }
            else {
                db.insertSubNo(currOrg.getId());
            }
        }
        return counter;
    }

    }


