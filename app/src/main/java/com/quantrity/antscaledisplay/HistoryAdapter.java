package com.quantrity.antscaledisplay;


import android.content.Context;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    //private static final String TAG = "HistoryAdapter";

    private ArrayList<Weight> mDataset;
    private final Context mContext;

    private User user;

    private final SimpleDateFormat dateFormatter;

    // Provide a suitable constructor (depends on the kind of dataset)
    HistoryAdapter(ArrayList<Weight> myDataset, Context mContext, User user) {
        mDataset = myDataset;
        this.mContext = mContext;
        this.user = user;
        this.dateFormatter = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(mContext);
        this.dateFormatter.applyPattern(dateFormatter.toPattern().replaceAll("y", "yy").replaceAll("y{4}", "yy"));
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        // each data item is just a string in this case
        private Weight weight;
        final TextView dateTV;
        final TextView timeTV;
        final TextView weightTV;
        final TextView bmiTV;
        final TextView trunkPercentFatTV;
        final TextView trunkMuscleMassTV;
        final TextView leftArmPercentFatTV;
        final TextView leftArmMuscleMassTV;
        final TextView rightArmPercentFatTV;
        final TextView rightArmMuscleMassTV;
        final TextView leftLegPercentFatTV;
        final TextView leftLegMuscleMassTV;
        final TextView rightLegPercentFatTV;
        final TextView rightLegMuscleMassTV;
        final TextView percentFatTV;
        final TextView percentHydrationTV;
        final TextView boneMassTV;
        final TextView muscleMassTV;
        final TextView physiqueRatingTV;
        final TextView visceralFatRatingTV;
        final TextView metabolicAgeTV;
        final TextView basalMetTV;
        final ImageView weightIV;
        final ImageView bmiIV;
        final ImageView percentFatIV;
        final ImageView percentHydrationIV;
        final ImageView boneMassIV;
        final ImageView muscleMassIV;
        final ImageView physiqueRatingIV;
        final ImageView visceralFatRatingIV;
        final ImageView metabolicAgeIV;
        final ImageView basalMetIV;
        final TableRow tftmTR;
        final TableRow laflamTR;
        final TableRow raframTR;
        final TableRow llfllmTR;
        final TableRow rlfrlmTR;
        final TableRow pfphTR;
        final TableRow bmmmTR;
        final TableRow prvfTR;
        final TableRow maamTR;

        ViewHolder(View v) {
            super(v);

            dateTV = v.findViewById(R.id.row_weight_dateTV);
            timeTV = v.findViewById(R.id.row_weight_timeTV);
            weightTV = v.findViewById(R.id.row_weight_weightTV);
            bmiTV = v.findViewById(R.id.row_weight_bmiTV);
            trunkPercentFatTV = v.findViewById(R.id.row_weight_trunkPercentFatTV);
            trunkMuscleMassTV = v.findViewById(R.id.row_weight_trunkMuscleMassTV);
            leftArmPercentFatTV = v.findViewById(R.id.row_weight_leftArmPercentFatTV);
            leftArmMuscleMassTV = v.findViewById(R.id.row_weight_leftArmMuscleMassTV);
            rightArmPercentFatTV = v.findViewById(R.id.row_weight_rightArmPercentFatTV);
            rightArmMuscleMassTV = v.findViewById(R.id.row_weight_rightArmMuscleMassTV);
            leftLegPercentFatTV = v.findViewById(R.id.row_weight_leftLegPercentFatTV);
            leftLegMuscleMassTV = v.findViewById(R.id.row_weight_leftLegMuscleMassTV);
            rightLegPercentFatTV = v.findViewById(R.id.row_weight_rightLegPercentFatTV);
            rightLegMuscleMassTV = v.findViewById(R.id.row_weight_rightLegMuscleMassTV);
            percentFatTV = v.findViewById(R.id.row_weight_percentFatTV);
            percentHydrationTV = v.findViewById(R.id.row_weight_percentHydrationTV);
            boneMassTV = v.findViewById(R.id.row_weight_boneMassTV);
            muscleMassTV = v.findViewById(R.id.row_weight_muscleMassTV);
            physiqueRatingTV = v.findViewById(R.id.row_weight_physiqueRatingTV);
            visceralFatRatingTV = v.findViewById(R.id.row_weight_visceralFatRatingTV);
            metabolicAgeTV = v.findViewById(R.id.row_weight_metabolicAgeTV);
            basalMetTV = v.findViewById(R.id.row_weight_basalMetTV);
            weightIV = v.findViewById(R.id.row_weight_weightIV);
            bmiIV = v.findViewById(R.id.row_weight_bmiIV);
            percentFatIV = v.findViewById(R.id.row_weight_percentFatIV);
            percentHydrationIV = v.findViewById(R.id.row_weight_percentHydrationIV);
            boneMassIV = v.findViewById(R.id.row_weight_boneMassIV);
            muscleMassIV = v.findViewById(R.id.row_weight_muscleMassIV);
            physiqueRatingIV = v.findViewById(R.id.row_weight_physiqueRatingIV);
            visceralFatRatingIV = v.findViewById(R.id.row_weight_visceralFatRatingIV);
            metabolicAgeIV = v.findViewById(R.id.row_weight_metabolicAgeIV);
            basalMetIV = v.findViewById(R.id.row_weight_basalMetIV);
            tftmTR = v.findViewById(R.id.tftmTR);
            laflamTR = v.findViewById(R.id.laflamTR);
            raframTR = v.findViewById(R.id.raframTR);
            llfllmTR = v.findViewById(R.id.llfllmTR);
            rlfrlmTR = v.findViewById(R.id.rlfrlmTR);
            pfphTR = v.findViewById(R.id.pfphTR);
            bmmmTR = v.findViewById(R.id.bmmmTR);
            prvfTR = v.findViewById(R.id.prvfTR);
            maamTR = v.findViewById(R.id.maamTR);

            v.setOnCreateContextMenuListener(this);
            v.setOnClickListener(View::showContextMenu);
        }



        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            //Log.v(TAG, contextMenu + "__" + view + ".." + contextMenuInfo);
            contextMenu.setHeaderTitle(R.string.users_fragment_user_contextmenu_title);
            MenuItem mi = contextMenu.add(0, view.getId(), 0, R.string.users_fragment_user_contextmenu_delete);//groupId, itemId, order, title
            mi.setOnMenuItemClickListener(menuItem -> {
                int position = mDataset.indexOf(weight);
                notifyItemRemoved(position);
                mDataset.remove(weight);
                ((MainActivity)mContext).deleteWeight(weight);
                return true;
            });
            if (user != null) {
                if ((user.gc_user != null) && (user.gc_pass != null) && (!user.gc_user.equals("")) && (!user.gc_pass.equals(""))) {
                    mi = contextMenu.add(0, view.getId(), 0, String.format(mContext.getString(R.string.users_fragment_user_contextmenu_upload_to), mContext.getString(R.string.edit_user_fragment_garmin_connect_category)));
                    mi.setOnMenuItemClickListener(menuItem -> {
                        AsyncUpload au = new AsyncUpload((MainActivity)mContext, weight, user, true, false);
                        au.execute(mContext.getCacheDir() + "/weight.fit");
                        //((MainActivity) mContext).showMessage(mContext.getString(R.string.gc_warning));
                        return true;
                    });
                }
                if ((user.email_to != null) && !user.email_to.equals("")) {
                    mi = contextMenu.add(0, view.getId(), 0, String.format(mContext.getString(R.string.users_fragment_user_contextmenu_upload_to), mContext.getString(R.string.edit_user_fragment_email_category)));
                    mi.setOnMenuItemClickListener(menuItem -> {
                        AsyncUpload au = new AsyncUpload((MainActivity)mContext, weight, user, false, true);
                        au.execute(mContext.getCacheDir() + "/weight.fit");
                        return true;
                    });
                }

                mi = contextMenu.add(0, view.getId(), 0, mContext.getString(R.string.weight_edit_fragment_edit_weight));
                mi.setOnMenuItemClickListener(menuItem -> {
                    ((MainActivity)mContext).openEditWeightFragment(weight, user, true);
                    return true;
                });
            }
        }

    }

    void replaceAll(ArrayList<Weight> myDataset, User user) {
        this.user = user;
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Log.v("TAG", "onCreateViewHolder");
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_weight, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //Log.v("TAG", "onBindViewHolder");
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Weight item = mDataset.get(position);

        holder.weight = item;
        int age_then = User.calcAge(user.birthdate, item.date);

        holder.dateTV.setText(dateFormatter.format(item.date));
        holder.timeTV.setText(DateUtils.formatDateTime(mContext, item.date, DateUtils.FORMAT_SHOW_TIME));

        double bmi = item.weight / Math.pow((item.height) / 100, 2);

        holder.bmiTV.setText(mContext.getString(R.string.weight_fragment_bmi_tag) + " " + String.format("%.02f", bmi));

        holder.weightTV.setText(user.printMass(mContext, item.weight));
        if (item.boneMass != -1) {
            holder.boneMassTV.setText(user.printMass(mContext, item.boneMass));
            switch (RequestWeight.getBoneMassDesc((float) item.weight, (float) item.boneMass, item.isMale)) {
                case 0:
                    holder.boneMassIV.setBackgroundResource(R.drawable.rounded_red_mini);
                    break;
                case 1:
                    holder.boneMassIV.setBackgroundResource(R.drawable.rounded_green_mini);
                    break;
                default:
                    holder.boneMassIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.boneMassTV.setVisibility(View.VISIBLE);
        } else holder.boneMassTV.setVisibility(View.INVISIBLE);

        if (item.muscleMass != -1) {
            holder.muscleMassTV.setText(user.printMass(mContext, item.muscleMass));
            holder.muscleMassTV.setVisibility(View.VISIBLE);
        } else holder.muscleMassTV.setVisibility(View.INVISIBLE);

        switch (RequestWeight.getBMIDesc((byte) age_then, (float) bmi, item.isMale)) {
            case 0:
            case 2:
                holder.weightIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
                holder.bmiIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
                break;
            case 1: holder.weightIV.setBackgroundResource(R.drawable.rounded_green_mini);
                holder.bmiIV.setBackgroundResource(R.drawable.rounded_green_mini);
                break;
            case 3: holder.weightIV.setBackgroundResource(R.drawable.rounded_red_mini);
                holder.bmiIV.setBackgroundResource(R.drawable.rounded_red_mini);
                break;
            default: holder.weightIV.setBackgroundResource(R.drawable.rounded_blue_mini);
                holder.bmiIV.setBackgroundResource(R.drawable.rounded_blue_mini);
                break;
        }

        if (item.percentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.percentFatTV.setText(user.printMass(mContext, item.weight * item.percentFat / 100));
            } else {
                holder.percentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.percentFat));
            }

            switch (RequestWeight.getPercentFatDesc((byte) age_then, (float) item.percentFat, item.isMale)) {
                case 0:
                case 2:
                    holder.percentFatIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
                    break;
                case 1: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_green_mini);
                    break;
                case 3: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_red_mini);
                    break;
                default: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.percentFatTV.setVisibility(View.VISIBLE);
        } else holder.percentFatTV.setVisibility(View.INVISIBLE);


        if (item.percentHydration != -1) {
            holder.percentHydrationTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), item.percentHydration));

            switch (RequestWeight.getPercentHydrationDesc((float) item.percentHydration, item.isMale)) {
                case 0:
                case 2:
                    holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
                    break;
                case 1:
                    holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_green_mini);
                    break;
                default:
                    holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.percentHydrationTV.setVisibility(View.VISIBLE);
        } else holder.percentHydrationTV.setVisibility(View.INVISIBLE);


        if (item.physiqueRating != -1) {
            holder.physiqueRatingTV.setText(Integer.toString(item.physiqueRating));

            switch (item.physiqueRating) {
                case 1:
                case 2:
                case 3:
                    holder.physiqueRatingIV.setBackgroundResource(R.drawable.rounded_red_mini);
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    holder.physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green_mini);
                    break;
            }
            holder.physiqueRatingTV.setVisibility(View.VISIBLE);
        } else holder.physiqueRatingTV.setVisibility(View.INVISIBLE);
        if (item.visceralFatRating != -1) {
            holder.visceralFatRatingTV.setText(String.format("%.2f", item.visceralFatRating));

            if ((item.visceralFatRating >= 1.0) && (item.visceralFatRating <= 12.5)) {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_green_mini);
            } else if ((item.visceralFatRating >= 12.5) && (item.visceralFatRating <= 59.0)) {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_red_mini);
            } else {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.visceralFatRatingTV.setVisibility(View.VISIBLE);
        } else holder.visceralFatRatingTV.setVisibility(View.INVISIBLE);

        if (item.metabolicAge != -1) {
            holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_age_mini);
            holder.metabolicAgeTV.setText(String.format(mContext.getString(R.string.weight_fragment_years_tag), item.metabolicAge));
            if (item.metabolicAge <= age_then) holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_green_mini);
            else if (item.metabolicAge <= (age_then) * 1.1) holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
            else holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_red_mini);
            holder.metabolicAgeTV.setVisibility(View.VISIBLE);
        } else {
            if (item.activeMet != -1) {
                holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_mini);
                holder.metabolicAgeTV.setText(String.format(mContext.getString(R.string.weight_fragment_kcal_tag), item.activeMet));
                holder.metabolicAgeTV.setVisibility(View.VISIBLE);
            } else holder.metabolicAgeTV.setVisibility(View.INVISIBLE);
        }

        if (item.basalMet != -1) {
            holder.basalMetTV.setText(String.format(mContext.getString(R.string.weight_fragment_kcal_tag), item.basalMet));
            holder.basalMetTV.setVisibility(View.VISIBLE);
            holder.basalMetIV.setVisibility(View.VISIBLE);
        } else {
            holder.basalMetTV.setVisibility(View.INVISIBLE);
            holder.basalMetIV.setVisibility(View.INVISIBLE);
        }

        if (item.trunkPercentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.trunkPercentFatTV.setText(user.printMass(mContext, item.weight * item.trunkPercentFat / 100));
            } else {
                holder.trunkPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.trunkPercentFat));
            }
            //holder.trunkPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.trunkPercentFat));
            holder.trunkPercentFatTV.setVisibility(View.VISIBLE);
        }
        else holder.trunkPercentFatTV.setVisibility(View.INVISIBLE);
        if (item.trunkMuscleMass != -1) {
            holder.trunkMuscleMassTV.setText(user.printMass(mContext, item.trunkMuscleMass));
            holder.trunkMuscleMassTV.setVisibility(View.VISIBLE);
        }
        else holder.trunkMuscleMassTV.setVisibility(View.INVISIBLE);

        if (item.leftArmPercentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.leftArmPercentFatTV.setText(user.printMass(mContext, item.weight * item.leftArmPercentFat / 100));
            } else {
                holder.leftArmPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.leftArmPercentFat));
            }
            //holder.leftArmPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.leftArmPercentFat));
            holder.leftArmPercentFatTV.setVisibility(View.VISIBLE);
        }
        else holder.leftArmPercentFatTV.setVisibility(View.INVISIBLE);
        if (item.leftArmMuscleMass != -1) {
            holder.leftArmMuscleMassTV.setText(user.printMass(mContext, item.leftArmMuscleMass));
            holder.leftArmMuscleMassTV.setVisibility(View.VISIBLE);
        }
        else holder.leftArmMuscleMassTV.setVisibility(View.INVISIBLE);

        if (item.rightArmPercentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.rightArmPercentFatTV.setText(user.printMass(mContext, item.weight * item.rightArmPercentFat / 100));
            } else {
                holder.rightArmPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.rightArmPercentFat));
            }
            //holder.rightArmPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.rightArmPercentFat));
            holder.rightArmPercentFatTV.setVisibility(View.VISIBLE);
        }
        else holder.rightArmPercentFatTV.setVisibility(View.INVISIBLE);
        if (item.rightArmMuscleMass != -1) {
            holder.rightArmMuscleMassTV.setText(user.printMass(mContext, item.rightArmMuscleMass));
            holder.rightArmMuscleMassTV.setVisibility(View.VISIBLE);
        }
        else holder.rightArmMuscleMassTV.setVisibility(View.INVISIBLE);

        if (item.leftLegPercentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.leftLegPercentFatTV.setText(user.printMass(mContext, item.weight * item.leftLegPercentFat / 100));
            } else {
                holder.leftLegPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.leftLegPercentFat));
            }
            //holder.leftLegPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.leftLegPercentFat));
            holder.leftLegPercentFatTV.setVisibility(View.VISIBLE);
        }
        else holder.leftLegPercentFatTV.setVisibility(View.INVISIBLE);
        if (item.leftLegMuscleMass != -1) {
            holder.leftLegMuscleMassTV.setText(user.printMass(mContext, item.leftLegMuscleMass));
            holder.leftLegMuscleMassTV.setVisibility(View.VISIBLE);
        }
        else holder.leftLegMuscleMassTV.setVisibility(View.INVISIBLE);

        if (item.rightLegPercentFat != -1) {
            if (user.show_fat_mass)
            {
                holder.rightLegPercentFatTV.setText(user.printMass(mContext, item.weight * item.rightLegPercentFat / 100));
            } else {
                holder.rightLegPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.rightLegPercentFat));
            }
            //holder.rightLegPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.rightLegPercentFat));
            holder.rightLegPercentFatTV.setVisibility(View.VISIBLE);
        }
        else holder.rightLegPercentFatTV.setVisibility(View.INVISIBLE);
        if (item.rightLegMuscleMass != -1) {
            holder.rightLegMuscleMassTV.setText(user.printMass(mContext, item.rightLegMuscleMass));
            holder.rightLegMuscleMassTV.setVisibility(View.VISIBLE);
        }
        else holder.rightLegMuscleMassTV.setVisibility(View.INVISIBLE);

        if ((item.boneMass == -1) && (item.muscleMass == -1)) holder.bmmmTR.setVisibility(View.GONE);
        else holder.bmmmTR.setVisibility(View.VISIBLE);
        if ((item.trunkPercentFat == -1) && (item.trunkMuscleMass == -1)) holder.tftmTR.setVisibility(View.GONE);
        else holder.tftmTR.setVisibility(View.VISIBLE);
        if ((item.leftArmPercentFat == -1) && (item.leftArmMuscleMass == -1)) holder.laflamTR.setVisibility(View.GONE);
        else holder.laflamTR.setVisibility(View.VISIBLE);
        if ((item.rightArmPercentFat == -1) && (item.rightArmMuscleMass == -1)) holder.raframTR.setVisibility(View.GONE);
        else holder.raframTR.setVisibility(View.VISIBLE);
        if ((item.leftArmPercentFat == -1) && (item.leftLegMuscleMass == -1)) holder.llfllmTR.setVisibility(View.GONE);
        else holder.llfllmTR.setVisibility(View.VISIBLE);
        if ((item.rightArmPercentFat == -1) && (item.rightLegMuscleMass == -1)) holder.rlfrlmTR.setVisibility(View.GONE);
        else holder.rlfrlmTR.setVisibility(View.VISIBLE);
        if ((item.percentFat == -1) && (item.percentHydration == -1)) holder.pfphTR.setVisibility(View.GONE);
        else holder.pfphTR.setVisibility(View.VISIBLE);
        if ((item.physiqueRating == -1) && (item.visceralFatRating == -1)) holder.prvfTR.setVisibility(View.GONE);
        else holder.prvfTR.setVisibility(View.VISIBLE);
        if ((item.metabolicAge == -1) && (item.activeMet == -1) && (item.basalMet == -1)) holder.maamTR.setVisibility(View.GONE);
        else holder.maamTR.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() { return mDataset.size(); }

}
