package com.quantrity.antscaledisplay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GoalsFragment extends Fragment {
    //private final static String TAG = "GoalsFragment";

    private GoalAdapter mAdapter;

    public GoalsFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_goals, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.goal_recycler_view);

        if (getActivity() != null) {
            // use a linear layout manager
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);

            mAdapter = new GoalAdapter(((MainActivity) getActivity()).getGoalsArraySelectedUser(),
                    getActivity(), ((MainActivity) getActivity()).getSelectedUser(),
                    ((MainActivity) getActivity()).getLastHistorySelectedUser());
            mRecyclerView.setAdapter(mAdapter);
        }

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }

    final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Log.v(TAG, "onItemSelected " + view);
            if ((view != null) && (getActivity() != null)) {
                //Log.v(TAG, "onItemSelected2 " + view);
                User user = (User)adapterView.getItemAtPosition(i);
                ((MainActivity)getActivity()).setSelectedUser(user);

                //Mostrar todos los pesos del usuario
                mAdapter.replaceAll(((MainActivity)getActivity()).getGoalsArraySelectedUser(), user);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        //Log.v(TAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_goals_menu, menu);
        if (getActivity() != null)
            ((MainActivity) getActivity()).addUsersSpinner(menu, oisListener);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        if (item.getItemId() == R.id.action_add_goal) {
            if (getActivity() != null)
                ((MainActivity) getActivity()).openEditGoalFragment(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
