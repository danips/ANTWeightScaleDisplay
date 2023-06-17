package com.quantrity.antscaledisplay;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    //private static final String TAG = "UsersAdapter";
    private static final int BLUE = Color.parseColor("#33b5e5");
    private static final int PINK = Color.parseColor("#F52887");

    private final ArrayList<User> mDataset;
    private final Context mContext;
    private final UsersFragment usersFragment;

    // Provide a suitable constructor (depends on the kind of dataset)
    UsersAdapter(ArrayList<User> myDataset, Context mContext, UsersFragment usersFragment) {
        mDataset = myDataset;
        this.mContext = mContext;
        this.usersFragment = usersFragment;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        // each data item is just a string in this case
        private User user;
        final TextView nameTV;
        final TextView descTV;
        final ImageView userIV;
        final ImageView gcIV;
        final ImageView emailIV;
        final RelativeLayout ll_user;

        ViewHolder(View v) {
            super(v);

            nameTV =  v.findViewById(R.id.nameTV);
            descTV =  v.findViewById(R.id.descTV);
            userIV =  v.findViewById(R.id.userIV);
            gcIV =  v.findViewById(R.id.gcIV);
            emailIV =  v.findViewById(R.id.emailIV);
            ll_user =  v.findViewById(R.id.ll_user);

            v.setOnCreateContextMenuListener(this);
            v.setOnClickListener(view -> usersFragment.editUser(user));
            v.setOnLongClickListener(view -> {
                view.showContextMenu();
                return true;
            });
        }



        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            contextMenu.setHeaderTitle(R.string.users_fragment_user_contextmenu_title);
            MenuItem mi = contextMenu.add(0, view.getId(), 0, R.string.users_fragment_user_contextmenu_delete);//groupId, itemId, order, title
            mi.setOnMenuItemClickListener(menuItem -> {

                int pos = mDataset.indexOf(user);
                //Delete user weights
                ((MainActivity)mContext).deleteHistoryAndUser(user);

                notifyItemRemoved(pos);
                ((MainActivity)mContext).supportInvalidateOptionsMenu();
                return true;
            });
            mi = contextMenu.add(0, view.getId(), 0, R.string.users_fragment_user_contextmenu_edit);
            mi.setOnMenuItemClickListener(menuItem -> {
                usersFragment.editUser(user);
                return true;
            });
        }

    }

    public void add(int position, User item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void update(User item) {
        int position = mDataset.indexOf(item);
        if (position != -1) notifyItemChanged(position);
        else add(0, item);
    }

    public User get(User item) {
        return mDataset.get(mDataset.indexOf(item));
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_user, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final User item = mDataset.get(position);

        holder.user = item;
        holder.nameTV.setText(item.name);
        if (item.usesCm) {
            holder.descTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag), item.age) + ", " + item.height_cm + " " + mContext.getResources().getString(R.string.edit_user_fragment_units_tag_cm));
        } else {
            holder.descTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag), item.age) + ", " + item.height_ft + mContext.getResources().getString(R.string.edit_user_fragment_units_tag_ft) + " " + item.height_in + mContext.getResources().getString(R.string.edit_user_fragment_units_tag_in));
        }
        holder.userIV.setColorFilter((item.isMale) ? BLUE : PINK, PorterDuff.Mode.SRC_IN);

        if ((item.gc_user!= null) && (item.gc_pass != null)) {
            holder.gcIV.setVisibility(View.VISIBLE);
            holder.gcIV.clearColorFilter();
        } else {
            holder.gcIV.setVisibility(View.GONE);
        }
        if (item.email_to != null) {
            holder.emailIV.setVisibility(View.VISIBLE);
            holder.emailIV.clearColorFilter();
        } else {
            holder.emailIV.setVisibility(View.GONE);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}
