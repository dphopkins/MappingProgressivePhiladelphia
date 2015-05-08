package edu.haverford.mpp.mappingprogressivephiladelphia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dan on 5/8/15.
 */
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
        //Log.v("ConvertView", String.valueOf(position));
        MyDatabase db = new MyDatabase(mContext);
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.org_list_item, null);

            holder = new ViewHolder();
            // holder.code = (TextView) convertView.findViewById(R.id.code);
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
            PhillyOrg currOrg = orgList.get(position);
            holder.CheckMeOrNot(db.isSubscribed(currOrg.getId()));
            convertView.setTag(holder);

            /*holder.code.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v){
                    TextView tv = (TextView) v ;
                    Intent intent = new Intent(mContext, OrganizationInfoActivity.class);
                    PhillyOrg currOrg = (PhillyOrg) tv.getTag();
                    intent.putExtra("OrgID", currOrg.getId());
                    mContext.startActivity(intent);
                }
            });*/
            holder.name.setOnClickListener( new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v ;
                    PhillyOrg currOrg = (PhillyOrg) cb.getTag();
                    Boolean subbed = cb.isChecked();
                    currOrg.setSubscribed(subbed);
                }
            });
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        db.close();

        PhillyOrg currOrg = orgList.get(position);
        // holder.code.setText(" (" +  currOrg.getId() + ")");
        holder.name.setText(currOrg.getGroupName());
        // holder.name.setChecked(currOrg.getSubscribed());
        holder.name.setTag(currOrg);
        // holder.code.setTag(currOrg);
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
        db.close();
        return counter;
    }
}