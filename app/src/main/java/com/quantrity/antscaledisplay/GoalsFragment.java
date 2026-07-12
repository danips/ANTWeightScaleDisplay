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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantrity.antscaledisplay.databinding.FragmentGoalsBinding;

public class GoalsFragment extends Fragment implements MenuProvider {
    //private final static String TAG = "GoalsFragment";

    private GoalAdapter mAdapter;
    private AppStateViewModel state;
    private FragmentGoalsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGoalsBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        RecyclerView mRecyclerView = binding.goalRecyclerView;

        if (getActivity() != null) {
            // use a linear layout manager
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);

            mAdapter = new GoalAdapter(state.selectedGoals(), getActivity(), state.selectedUser(),
                    state.lastSelectedWeight(), this);
            mRecyclerView.setAdapter(mAdapter);
        }

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        binding.goalRecyclerView.setAdapter(null);
        mAdapter = null;
        binding = null;
        super.onDestroyView();
    }

    final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Log.v(TAG, "onItemSelected " + view);
            if ((view != null) && (getActivity() != null)) {
                //Log.v(TAG, "onItemSelected2 " + view);
                User user = (User)adapterView.getItemAtPosition(i);
                state.selectUser(user);

                //Mostrar todos los pesos del usuario
                mAdapter.replaceAll(state.selectedGoals(), user);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        //Log.v(TAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_goals_menu, menu);
        if (getActivity() != null)
            AppHost.from(this).addUsersSpinner(menu, oisListener);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        if (menuItem.getItemId() == R.id.action_add_goal) {
            if (getActivity() != null)
                AppHost.from(this).openEditGoalFragment(null);
            return true;
        }
        return false;
    }

    void deleteGoal(Goal goal) {
        state.deleteGoal(goal, result -> {
            if (getActivity() != null) AppHost.from(this).handleMutationFailure(result);
        });
    }

    void editGoal(Goal goal) {
        if (getActivity() != null) AppHost.from(this).openEditGoalFragment(goal);
    }
}
