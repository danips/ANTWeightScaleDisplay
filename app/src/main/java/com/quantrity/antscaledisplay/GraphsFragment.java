package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GraphsFragment extends Fragment implements OnChartGestureListener {
    private final static String TAG = "GraphsFragment";

    private LinearLayout graphLayout;

    private User the_user;
    private ArrayList<Weight> weights;
    private int graph_measurement_displayed;
    private int graph_period_displayed;
    private MenuItem measurement_selection;
    private SubMenu measurement_items;
    private MenuItem period_selection;
    private SubMenu period_items;

    private LineChart mChart;
    private ArrayList<Entry> oData;
    private boolean averageShowing = false;
    private final CountDownTimer cdt = new CountDownTimer(100, 100) {
        @Override
        public void onTick(long l) {}

        @Override
        public void onFinish() {
            ViewPortHandler handler = mChart.getViewPortHandler();
            MPPointD topLeft = mChart.getValuesByTouchPoint(handler.contentLeft(), handler.contentTop(), YAxis.AxisDependency.LEFT);
            MPPointD bottomRight = mChart.getValuesByTouchPoint(handler.contentRight(), handler.contentBottom(), YAxis.AxisDependency.LEFT);
            double end = Math.min(bottomRight.x, oData.get(oData.size() - 1).getX());
            addAverage((float)end, (float)(end - topLeft.x), oData);
        }
    };

    public GraphsFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (graphLayout != null) {
            Log.v(TAG, "onConfigurationChanged loadGraph ");
            loadGraph(graph_measurement_displayed, graph_period_displayed);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_graphs, container, false);

        graphLayout = rootView.findViewById(R.id.graph);

        if (getActivity() != null) {
            weights = ((MainActivity) getActivity()).getHistoryArraySelectedUser();
            the_user = ((MainActivity) getActivity()).getSelectedUser();
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        graph_measurement_displayed = settings.getInt("selected_graph_measurement", R.id.graph_weight);
        graph_period_displayed = settings.getInt("selected_graph_period", R.id.graph_time_month);
        loadGraph(graph_measurement_displayed, graph_period_displayed);

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }

    private void updateActionBar() {
        boolean hasWeight = false,
                hasTrunkPercentFat = false,
                hasTrunkMuscleMass = false,
                hasLeftArmPercentFat = false,
                hasLeftArmMuscleMass = false,
                hasRightArmPercentFat = false,
                hasRightArmMuscleMass = false,
                hasLeftLegPercentFat = false,
                hasLeftLegMuscleMass = false,
                hasRightLegPercentFat = false,
                hasRightLegMuscleMass = false,
                hasPercentFat = false,
                hasPercentHydration = false,
                hasBoneMass = false,
                hasMuscleMass = false,
                hasPhysiqueRating = false,
                hasVisceralFat = false,
                hasMetabolicAge = false,
                hasActiveMet = false,
                hasBasalMet = false;
        for (Weight w : weights) {
            if (!hasWeight) hasWeight = (w.weight != -1);
            if (!hasTrunkPercentFat) hasTrunkPercentFat = (w.trunkPercentFat != -1);
            if (!hasTrunkMuscleMass) hasTrunkMuscleMass = (w.trunkMuscleMass != -1);
            if (!hasLeftArmPercentFat) hasLeftArmPercentFat = (w.leftArmPercentFat != -1);
            if (!hasLeftArmMuscleMass) hasLeftArmMuscleMass = (w.leftArmMuscleMass != -1);
            if (!hasRightArmPercentFat) hasRightArmPercentFat = (w.rightArmPercentFat != -1);
            if (!hasRightArmMuscleMass) hasRightArmMuscleMass = (w.rightArmMuscleMass != -1);
            if (!hasLeftLegPercentFat) hasLeftLegPercentFat = (w.leftLegPercentFat != -1);
            if (!hasLeftLegMuscleMass) hasLeftLegMuscleMass = (w.leftLegMuscleMass != -1);
            if (!hasRightLegPercentFat) hasRightLegPercentFat = (w.rightLegPercentFat != -1);
            if (!hasRightLegMuscleMass) hasRightLegMuscleMass = (w.rightLegMuscleMass != -1);
            if (!hasPercentFat) hasPercentFat = (w.percentFat != -1);
            if (!hasPercentHydration) hasPercentHydration = (w.percentHydration != -1);
            if (!hasBoneMass) hasBoneMass = (w.boneMass != -1);
            if (!hasMuscleMass) hasMuscleMass = (w.muscleMass != -1);
            if (!hasPhysiqueRating) hasPhysiqueRating = (w.physiqueRating != -1);
            if (!hasVisceralFat) hasVisceralFat = (w.visceralFatRating != -1);
            if (!hasMetabolicAge) hasMetabolicAge = (w.metabolicAge != -1);
            if (!hasActiveMet) hasActiveMet = (w.activeMet != -1);
            if (!hasBasalMet) hasBasalMet = (w.basalMet != -1);
        }
        measurement_items.getItem(0).setVisible(hasWeight);
        measurement_items.getItem(1).setVisible(hasWeight);
        measurement_items.getItem(2).setVisible(hasPercentFat);
        measurement_items.getItem(3).setVisible(hasPercentHydration);
        measurement_items.getItem(4).setVisible(hasBoneMass);
        measurement_items.getItem(5).setVisible(hasMuscleMass);
        measurement_items.getItem(6).setVisible(hasPhysiqueRating);
        measurement_items.getItem(7).setVisible(hasVisceralFat);
        measurement_items.getItem(8).setVisible(hasMetabolicAge);
        measurement_items.getItem(9).setVisible(hasActiveMet);
        measurement_items.getItem(10).setVisible(hasBasalMet);
        measurement_items.getItem(11).setVisible(hasTrunkPercentFat);
        measurement_items.getItem(12).setVisible(hasTrunkMuscleMass);
        measurement_items.getItem(13).setVisible(hasLeftArmPercentFat);
        measurement_items.getItem(14).setVisible(hasLeftArmMuscleMass);
        measurement_items.getItem(15).setVisible(hasRightArmPercentFat);
        measurement_items.getItem(16).setVisible(hasRightArmMuscleMass);
        measurement_items.getItem(17).setVisible(hasLeftLegPercentFat);
        measurement_items.getItem(18).setVisible(hasLeftLegMuscleMass);
        measurement_items.getItem(19).setVisible(hasRightLegPercentFat);
        measurement_items.getItem(20).setVisible(hasRightLegMuscleMass);

        measurement_selection.setVisible(hasWeight || hasPercentFat || hasPercentHydration || hasBoneMass || hasMuscleMass
                || hasPhysiqueRating || hasVisceralFat || hasMetabolicAge || hasActiveMet
                || hasBasalMet || hasTrunkPercentFat || hasTrunkMuscleMass || hasLeftArmPercentFat
                || hasLeftArmMuscleMass || hasRightArmPercentFat || hasRightArmMuscleMass
                || hasLeftLegPercentFat || hasLeftLegMuscleMass || hasRightLegPercentFat
                || hasRightLegMuscleMass);


        if (weights.size() > 1) {
            long period = weights.get(0).date - weights.get(weights.size() - 1).date;
            period_selection.setVisible(period > 604800000L);
            period_items.getItem(1).setVisible(period > 2592000000L);
            period_items.getItem(2).setVisible(period > 15552000000L);
            period_items.getItem(3).setVisible(period > 31536000000L);
        } else period_selection.setVisible(false);
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if ((view != null) && (getActivity() != null)) {
                User user = (User)adapterView.getItemAtPosition(i);
                ((MainActivity)getActivity()).setSelectedUser(user);

                weights = ((MainActivity)getActivity()).getHistoryArraySelectedUser();
                the_user = user;
                updateActionBar();
                loadGraph(graph_measurement_displayed, graph_period_displayed);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_graphs_menu, menu);

        if (getActivity() != null)
            ((MainActivity)getActivity()).addUsersSpinner(menu, oisListener);

        period_selection = menu.findItem(R.id.action_graph_param_time);
        period_items = period_selection.getSubMenu();

        measurement_selection = menu.findItem(R.id.action_graph_param);
        setIcon(graph_measurement_displayed);
        measurement_items = measurement_selection.getSubMenu();
        for (int i = 0; i < measurement_items.size(); i++)
        {
            turnWhite(measurement_items.getItem(i).getIcon());
        }

        updateActionBar();

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setIcon(int itemid) {
        int resid = R.drawable.ic_weight;
        if (itemid == R.id.graph_weight) {
            resid = R.drawable.ic_weight;
        } else if (itemid == R.id.graph_bmi) {
            resid = R.drawable.ic_bmi;
        } else if (itemid == R.id.graph_metabolicAge || itemid == R.id.graph_activeMet || itemid == R.id.graph_basalMet) {
            resid = R.drawable.ic_metabolic;
        } else if (itemid == R.id.graph_percentFat) {
            resid = R.drawable.ic_percent_fat;
        } else if (itemid == R.id.graph_percentHydration) {
            resid = R.drawable.ic_percent_hydration;
        } else if (itemid == R.id.graph_boneMass) {
            resid = R.drawable.ic_bone_mass;
        } else if (itemid == R.id.graph_muscleMass) {
            resid = R.drawable.ic_muscle_mass;
        } else if (itemid == R.id.graph_physiqueRating) {
            resid = R.drawable.ic_physique_rating;
        } else if (itemid == R.id.graph_visceralFatRating) {
            resid = R.drawable.ic_visceral_fat_rating;
        } else if (itemid == R.id.graph_trunkFatPercent || itemid == R.id.graph_trunkMuscleMass) {
            resid = R.drawable.ic_trunk;
        } else if (itemid == R.id.graph_leftArmFatPercent || itemid == R.id.graph_leftArmMuscleMass) {
            resid = R.drawable.ic_left_arm;
        } else if (itemid == R.id.graph_rightArmFatPercent || itemid == R.id.graph_rightArmMuscleMass) {
            resid = R.drawable.ic_right_arm;
        } else if (itemid == R.id.graph_leftLegFatPercent || itemid == R.id.graph_leftLegMuscleMass) {
            resid = R.drawable.ic_left_leg;
        } else if (itemid == R.id.graph_rightLegFatPercent || itemid == R.id.graph_rightLegMuscleMass) {
            resid = R.drawable.ic_right_leg;
        }
        measurement_selection.setIcon(resid);
        turnWhite(measurement_selection.getIcon());
    }

    private void turnWhite(Drawable d)
    {
        if (d != null)
        {
            d.mutate();
            d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == R.id.graph_time_week || itemId == R.id.graph_time_two_weeks || itemId == R.id.graph_time_six_weeks || itemId == R.id.graph_time_two_months || itemId == R.id.graph_time_four_months || itemId == R.id.graph_time_half_year || itemId == R.id.graph_time_year || itemId == R.id.graph_time_two_years || itemId == R.id.graph_time_always) {
            if (graph_period_displayed != item.getItemId()) {
                SharedPreferences settings1 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor1 = settings1.edit();
                editor1.putInt("selected_graph_period", item.getItemId());
                editor1.apply();
                loadGraph(graph_measurement_displayed, item.getItemId());
            }
        } else if (itemId == R.id.graph_time_month) {
            if (graph_period_displayed != item.getItemId()) {
                SharedPreferences settings1 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor1 = settings1.edit();
                editor1.putInt("selected_graph_period", item.getItemId());
                editor1.apply();
                loadGraph(graph_measurement_displayed, item.getItemId());
            }
        } else if (itemId == R.id.graph_percentHydration || itemId == R.id.graph_boneMass || itemId == R.id.graph_muscleMass || itemId == R.id.graph_physiqueRating || itemId == R.id.graph_visceralFatRating || itemId == R.id.graph_metabolicAge || itemId == R.id.graph_activeMet || itemId == R.id.graph_basalMet || itemId == R.id.graph_trunkFatPercent || itemId == R.id.graph_trunkMuscleMass || itemId == R.id.graph_leftArmFatPercent || itemId == R.id.graph_leftArmMuscleMass || itemId == R.id.graph_rightArmFatPercent || itemId == R.id.graph_rightArmMuscleMass || itemId == R.id.graph_leftLegFatPercent || itemId == R.id.graph_leftLegMuscleMass || itemId == R.id.graph_rightLegFatPercent || itemId == R.id.graph_rightLegMuscleMass) {
            if (graph_measurement_displayed != item.getItemId()) {
                SharedPreferences settings2 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor2 = settings2.edit();
                editor2.putInt("selected_graph_measurement", item.getItemId());
                editor2.apply();
                setIcon(item.getItemId());
                loadGraph(item.getItemId(), graph_period_displayed);
            }
        } else if (itemId == R.id.graph_weight || itemId == R.id.graph_bmi || itemId == R.id.graph_percentFat) {
            if (graph_measurement_displayed != item.getItemId()) {
                SharedPreferences settings2 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor2 = settings2.edit();
                editor2.putInt("selected_graph_measurement", item.getItemId());
                editor2.apply();
                setIcon(item.getItemId());
                loadGraph(item.getItemId(), graph_period_displayed);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private Entry getEntry(int item_id, Weight w) {
        float date = w.date;
        Entry dp = null;
        if (item_id == R.id.graph_bmi) {
            if ((w.weight != -1) && (w.height != -1))
                dp = new Entry(date, (float) (w.weight * Math.pow(100 / ((float) w.height), 2)));
        } else if (item_id == R.id.graph_percentFat) {
            if (w.percentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.percentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_percentHydration) {
            if (w.percentHydration != -1) dp = new Entry(date, (float) w.percentHydration);
        } else if (item_id == R.id.graph_boneMass) {
            if (w.boneMass != -1) dp = new Entry(date, (float) w.boneMass);
        } else if (item_id == R.id.graph_muscleMass) {
            if (w.muscleMass != -1) dp = new Entry(date, (float) w.muscleMass);
        } else if (item_id == R.id.graph_physiqueRating) {
            if (w.physiqueRating != -1) dp = new Entry(date, (float) w.physiqueRating);
        } else if (item_id == R.id.graph_visceralFatRating) {
            if (w.visceralFatRating != -1) dp = new Entry(date, (float) w.visceralFatRating);
        } else if (item_id == R.id.graph_metabolicAge) {
            if (w.metabolicAge != -1) dp = new Entry(date, (float) w.metabolicAge);
        } else if (item_id == R.id.graph_activeMet) {
            if (w.activeMet != -1) dp = new Entry(date, (float) w.activeMet);
        } else if (item_id == R.id.graph_basalMet) {
            if (w.basalMet != -1) dp = new Entry(date, (float) w.basalMet);
        } else if (item_id == R.id.graph_trunkFatPercent) {
            if (w.trunkPercentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.trunkPercentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_trunkMuscleMass) {
            if (w.trunkMuscleMass != -1) dp = new Entry(date, (float) w.trunkMuscleMass);
        } else if (item_id == R.id.graph_leftArmFatPercent) {
            if (w.leftArmPercentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.leftArmPercentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_leftArmMuscleMass) {
            if (w.leftArmMuscleMass != -1) dp = new Entry(date, (float) w.leftArmMuscleMass);
        } else if (item_id == R.id.graph_rightArmFatPercent) {
            if (w.rightArmPercentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.rightArmPercentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_rightArmMuscleMass) {
            if (w.rightArmMuscleMass != -1) dp = new Entry(date, (float) w.rightArmMuscleMass);
        } else if (item_id == R.id.graph_leftLegFatPercent) {
            if (w.leftLegPercentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.leftLegPercentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_leftLegMuscleMass) {
            if (w.leftLegMuscleMass != -1) dp = new Entry(date, (float) w.leftLegMuscleMass);
        } else if (item_id == R.id.graph_rightLegFatPercent) {
            if (w.rightLegPercentFat != -1) {
                dp = new Entry(date, (float) the_user.calc_mass2(w.rightLegPercentFat, w.weight, true));
            }
        } else if (item_id == R.id.graph_rightLegMuscleMass) {
            if (w.rightLegMuscleMass != -1) dp = new Entry(date, (float) w.rightLegMuscleMass);
        } else {
            if (w.weight != -1) dp = new Entry(date, (float) w.weight);
        }
        return dp;
    }

    private void loadGraph(int item_id, int period_id) {
        graph_measurement_displayed = item_id;
        graph_period_displayed = period_id;

        graphLayout.removeAllViews();

        //Establish the date limit
        Calendar date_limit = Calendar.getInstance();
        float window = 0;
        if (period_id == R.id.graph_time_week) {
            date_limit.add(Calendar.DAY_OF_MONTH, -7);
            window = 7;
        } else if (period_id == R.id.graph_time_two_weeks) {
            date_limit.add(Calendar.DAY_OF_MONTH, -14);
            window = 14;
        } else if (period_id == R.id.graph_time_month) {
            date_limit.add(Calendar.MONTH, -1);
            window = 365f / 12f;
        } else if (period_id == R.id.graph_time_six_weeks) {
            date_limit.add(Calendar.DAY_OF_MONTH, -42);
            window = 7 * 6;
        } else if (period_id == R.id.graph_time_two_months) {
            date_limit.add(Calendar.MONTH, -2);
            window = 365f / 6f;
        } else if (period_id == R.id.graph_time_four_months) {
            date_limit.add(Calendar.MONTH, -4);
            window = 365f / 4f;
        } else if (period_id == R.id.graph_time_half_year) {
            date_limit.add(Calendar.MONTH, -6);
            window = 365f / 2f;
        } else if (period_id == R.id.graph_time_year) {
            date_limit.add(Calendar.YEAR, -1);
            window = 365;
        } else if (period_id == R.id.graph_time_two_years) {
            date_limit.add(Calendar.YEAR, -2);
            window = 365 * 2;
        } else {
            if (weights.size() > 0) {
                date_limit.setTimeInMillis(weights.get(weights.size() - 1).date);
                window = (float) (weights.get(0).date - weights.get(weights.size() - 1).date) / (1000 * 24 * 3600);
            }
        }
        long time_limit = date_limit.getTimeInMillis();

        ArrayList<Entry> yVals = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            Weight w = weights.get(i);
            Entry dp = getEntry(item_id, w);
            if (dp != null) yVals.add(dp);
        }

        if (yVals.isEmpty()) {
            if (Debug.ON) Log.v(TAG, "(data.size() == 0) CLEAR GRAPH");
            return;
        }

        int color;
        int color2;
        String measureFormat = "%.01f";
        boolean stones = false;
        boolean pounds = false;
        if (item_id == R.id.graph_bmi) {
            color = Color.parseColor("#de3450");
            color2 = Color.argb(150, 0xde, 0x34, 0x50);
        } else if (item_id == R.id.graph_percentFat) {
            color = Color.parseColor("#ff9c13");
            color2 = Color.argb(150, 0xff, 0x9c, 0x13);
            if (the_user.show_fat_mass) {
                measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
                stones = (the_user.mass_unit == User.MassUnit.ST);
                pounds = (the_user.mass_unit == User.MassUnit.LB);
            } else {
                measureFormat = getString(R.string.weight_fragment_percent_tag);
            }
        } else if (item_id == R.id.graph_percentHydration) {
            color = Color.parseColor("#3dc4d4");
            color2 = Color.argb(150, 0x3d, 0xc4, 0xd4);
            measureFormat = getString(R.string.weight_fragment_percent_tag);
        } else if (item_id == R.id.graph_boneMass) {
            color = Color.parseColor("#94857d");
            color2 = Color.argb(150, 0x94, 0x85, 0x7d);
            measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
            stones = (the_user.mass_unit == User.MassUnit.ST);
            pounds = (the_user.mass_unit == User.MassUnit.LB);
        } else if (item_id == R.id.graph_muscleMass) {
            color = Color.parseColor("#b35fae");
            color2 = Color.argb(150, 0xb3, 0x5f, 0xae);
            measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
            stones = (the_user.mass_unit == User.MassUnit.ST);
            pounds = (the_user.mass_unit == User.MassUnit.LB);
        } else if (item_id == R.id.graph_physiqueRating) {
            color = Color.parseColor("#578ccf");
            color2 = Color.argb(150, 0x57, 0x8c, 0xcf);
        } else if (item_id == R.id.graph_visceralFatRating) {
            color = Color.parseColor("#e06e3a");
            color2 = Color.argb(150, 0xe0, 0x6e, 0x3a);
        } else if (item_id == R.id.graph_metabolicAge) {
            color = Color.parseColor("#72c250");
            color2 = Color.argb(150, 0x72, 0xc2, 0x50);
        } else if (item_id == R.id.graph_activeMet) {
            color = Color.parseColor("#3BD743");
            color2 = Color.argb(150, 0x3b, 0xd7, 0x43);
            measureFormat = getString(R.string.weight_fragment_kcal_tag);
        } else if (item_id == R.id.graph_basalMet) {
            color = Color.parseColor("#d73bcf");
            color2 = Color.argb(150, 0xd7, 0x3b, 0xcf);
            measureFormat = getString(R.string.weight_fragment_kcal_tag);
        } else if (item_id == R.id.graph_trunkFatPercent || item_id == R.id.graph_leftArmFatPercent || item_id == R.id.graph_rightArmFatPercent || item_id == R.id.graph_leftLegFatPercent || item_id == R.id.graph_rightLegFatPercent) {
            color = Color.parseColor("#34a5de");
            color2 = Color.argb(150, 0x34, 0xa5, 0xde);
            if (the_user.show_fat_mass) {
                measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
                stones = (the_user.mass_unit == User.MassUnit.ST);
                pounds = (the_user.mass_unit == User.MassUnit.LB);
            } else {
                measureFormat = getString(R.string.weight_fragment_percent_tag);
            }
        } else {
            color = Color.parseColor("#34a5de");
            color2 = Color.argb(150, 0x34, 0xa5, 0xde);
            measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
            stones = (the_user.mass_unit == User.MassUnit.ST);
            pounds = (the_user.mass_unit == User.MassUnit.LB);
        }

        mChart = new LineChart(getActivity());
        mChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mChart.setDrawGridBackground(true);
        mChart.setBackgroundColor(Color.rgb(48, 48, 48));
        mChart.setGridBackgroundColor(Color.rgb(48, 48, 48));
        // no description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        mChart.getAxisLeft().setTextColor(Color.WHITE);
        mChart.getXAxis().setTextColor(Color.WHITE);
        mChart.getAxisLeft().setDrawGridLines(true);
        mChart.getAxisLeft().enableGridDashedLine(10f, 10f, 0f);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setDrawGridLines(true);
        mChart.getXAxis().setDrawAxisLine(true);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getXAxis().enableGridDashedLine(10f, 10f, 0f);

        mChart.getXAxis().setValueFormatter(new DateAxisValueFormatter());

        graphLayout.addView(mChart);
        mChart.getAxisLeft().setValueFormatter(new StringAxisValueFormatter(measureFormat, stones, pounds));

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        MyMarkerView mv = new MyMarkerView(getContext(), R.layout.custom_marker_view, measureFormat, stones, pounds);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart


        if (oData != null)
        {
            oData.clear();
        }
        oData = new ArrayList<>();
        ArrayList<Entry> oRollingAvg = new ArrayList<>();
        double rollingAvg = 0;
        double windowMillis = (window * 86400000) / 365 * 30 ;//604800000L;
        double deltaTime;
        double lastSaved = 0;
        float previousTime = 0;
        for (int i = 0; i < yVals.size(); i++) {
            Entry current = yVals.get(yVals.size() - i - 1);
            oData.add(current);
            if (previousTime == 0)
            {
                rollingAvg = current.getY();
                oRollingAvg.add(new Entry(current.getX(), (float)rollingAvg));
                lastSaved = current.getX();
            }
            else
            {
                deltaTime = current.getX() - previousTime;
                double coeff = Math.exp(-1.0 * (deltaTime / windowMillis));
                rollingAvg = (1.0 - coeff) * current.getY() + coeff * rollingAvg;
            }
            if ((current.getX() - lastSaved) >= windowMillis) {
                oRollingAvg.add(new Entry(current.getX(), (float) rollingAvg));
                lastSaved = current.getX();
            }
            previousTime = current.getX();
        }
        if (oRollingAvg.get(oRollingAvg.size() - 1).getX() != previousTime)
        {
            oRollingAvg.add(new Entry(previousTime, (float) rollingAvg));
        }

        addAverage(oData.get(oData.size() - 1).getX(), window * 86400000, oData);

        LineDataSet rollingAvgSeries = new LineDataSet(oRollingAvg, null);
        rollingAvgSeries.setDrawCircles(false);
        rollingAvgSeries.setDrawValues(false);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.7f;
        rollingAvgSeries.setLineWidth(2f);
        rollingAvgSeries.setColor(Color.HSVToColor(hsv));
        rollingAvgSeries.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet series = new LineDataSet(oData, null);
        series.setCircleColor(color);
        series.setCircleHoleColor(color);
        series.setCircleRadius(2f);
        series.setDrawValues(false);
        series.setColor(color);
        series.setDrawFilled(true);
        series.setFillColor(color2);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(rollingAvgSeries);
        dataSets.add(series);

        if (getActivity() != null) {
            ArrayList<Goal> goals = ((MainActivity) getActivity()).getGoalsArray();
            if ((goals != null) && (goals.size() > 0)) {
                for (Goal g : goals) {
                    if (g.uuid.equals(the_user.uuid)) {
                        if (Metric.isSameMetric(g.type, graph_measurement_displayed)) {
                            if (((g.type == Metric.PERCENTFAT) || (g.type == Metric.TRUNKPERCENTFAT)
                                    || (g.type == Metric.LEFTARMPERCENTFAT) || (g.type == Metric.RIGHTARMPERCENTFAT)
                                    || (g.type == Metric.LEFTLEGPERCENTFAT) || (g.type == Metric.RIGHTLEGPERCENTFAT))
                                    && (g.show_fat_mass != the_user.show_fat_mass))
                            {
                                continue;
                            }
                            Entry g1 = new Entry(g.start_date, (float) g.start_value);
                            Entry g2;
                            /*double time = (g.start_date + 0.1 * window * 1000 * 60 * 60 * 24);
                            Log.v(TAG, "XXXX " + time + " " + window + " " + g.start_date);
                            Log.v(TAG, "XXXX " + (0.1 * window * 1000 * 60 * 60 * 24));
                            Log.v(TAG, time + " " + g.end_date);
                            if (time < g.end_date) {
                                double val = (time - g.start_date) / (g.end_date - g.start_date);
                                val *= (g.end_value - g.start_value);
                                val += g.start_value;
                                //double val = (((time - g.start_value) * (g.end_value - g.start_value) / (g.end_date - g.start_value)) + g.start_date);
                                //double val = (((time - g.start_value) * (g.end_value - g.start_value) / (g.end_date - g.start_value)) + g.start_date);
                                g2 = new Entry((float) time, (float) val);
                            }
                            else {*/
                            g2 = new Entry(g.end_date, (float) g.end_value);
                            //}
                            ArrayList<Entry> oGoal = new ArrayList<>();
                            oGoal.add(g1);
                            oGoal.add(g2);
                            LineDataSet goal = new LineDataSet(oGoal, null);
                            goal.setColor(g.color);
                            goal.setLineWidth(1f);
                            goal.enableDashedLine(8f, 5f, 0f);
                            goal.setDrawCircles(false);
                            goal.setDrawValues(false);
                            dataSets.add(goal);
                        }
                    }
                }
            }
        }


        // create a data object with the datasets
        LineData data = new LineData(dataSets);
        mChart.setData(data);

        // get the legend (only possible after setting data)
        mChart.getLegend().setEnabled(false);

        // now modify viewport
        float data_window = (oData.get(oData.size() - 1).getX() - oData.get(0).getX())/(1000*24*3600);
        if (Debug.ON) {
            Log.v(TAG, "from " + oData.get(0).getX() + " to " + oData.get(oData.size() - 1).getX());
            Log.v(TAG, "time_limit " + time_limit);
            Log.v(TAG, "new calc " + (oData.get(oData.size() - 1).getX() - oData.get(0).getX()));
            Log.v(TAG, "window " + window);
            Log.v(TAG, "zoom " + (data_window / window));
        }
        mChart.zoom((data_window/window),1,0,0);

        mChart.moveViewToX(oData.get(oData.size() - 1).getX());

        mChart.setOnChartGestureListener(this);
    }

    private void addAverage(float end, float span, ArrayList<Entry> oData)
    {
        if (mChart != null)
        {
            //Log.v(TAG, "end=" + end + " span=" + span);
            double avgOnScreen = 0;
            float onScreenX = end - span;
            //Log.v(TAG, "span="+span+ " onScreenX=" + onScreenX + " last=" + end);
            boolean firstOnScreen = true;
            for (int i = 0; i < oData.size() - 1; i++) {
                if (oData.get(i + 1).getX() >= onScreenX)
                {
                    //Log.v(TAG, "X="+ oData.get(i).getX() + " Y=" + oData.get(i).getY() + " // X2="+ oData.get(i + 1).getX() + " Y2=" + oData.get(i + 1).getY());
                    float xIni, yIni;
                    if (firstOnScreen && (oData.get(i).getX() < onScreenX))
                    {
                        //Log.v(TAG, "hHHHHHHHHHHHH editting first INSIDE");
                        xIni = onScreenX;
                        yIni = ((oData.get(i + 1).getY() - oData.get(i).getY()) / (oData.get(i + 1).getX() - oData.get(i).getX())) * (xIni - oData.get(i).getX()) + oData.get(i).getY();
                        firstOnScreen = false;
                    }
                    else
                    {
                        xIni = oData.get(i).getX();
                        yIni = oData.get(i).getY();
                    }

                    if (oData.get(i + 1).getX() <= end) {
                        double inc = (oData.get(i + 1).getY() + yIni) * (oData.get(i + 1).getX() - xIni) / (2 * span);
                        avgOnScreen += inc;
                        //Log.v(TAG, "A.- X="+ xIni + " Y=" + yIni + " // X2="+ oData.get(i + 1).getX() + " Y2=" + oData.get(i + 1).getY() + " INC=" + inc);
                    }
                    else
                    {
                        float yEnd = ((oData.get(i + 1).getY() - oData.get(i).getY()) / (oData.get(i + 1).getX() - oData.get(i).getX())) * (end - oData.get(i).getX()) + oData.get(i).getY();
                        double inc = (yEnd + yIni) * (end - xIni) / (2 * span);
                        avgOnScreen += inc;
                        break;
                    }
                }
            }

            //Log.v(TAG, "avgOnScreen " + avgOnScreen);
            LimitLine average = new LimitLine((float) avgOnScreen, "Average");
            average.setLineColor(Color.WHITE);
            average.setTextColor(Color.WHITE);
            average.setLineWidth(3f);
            average.enableDashedLine(8f, 5f, 0f);
            average.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            average.setTextSize(10f);

            YAxis leftAxis = mChart.getAxisLeft();
            if (averageShowing) leftAxis.removeAllLimitLines();
            else averageShowing = true;
            leftAxis.addLimitLine(average);
            mChart.invalidate();
            averageShowing = true;

        }
    }

    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
    public void onChartLongPressed(MotionEvent me) {}
    public void onChartDoubleTapped(MotionEvent me) {}
    public void onChartSingleTapped(MotionEvent me) {}
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        if (mChart != null)
        {
            if (averageShowing) {
                mChart.getAxisLeft().removeAllLimitLines();
                averageShowing = false;
            }
            cdt.cancel();
            cdt.start();
        }
    }

    public class DateAxisValueFormatter extends ValueFormatter {
        private final SimpleDateFormat dateFormatter = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(getContext());
        { dateFormatter.applyPattern(dateFormatter.toPattern().replaceAll("y", "yy").replaceAll("y{4}", "yy")); }


        @Override
        public String getFormattedValue(float value) {
            return dateFormatter.format(value);
        }
    }

    public static class StringAxisValueFormatter extends ValueFormatter {
        private final String measureFormat;
        private final boolean stones;
        private final boolean pounds;

        StringAxisValueFormatter(String measureFormat, boolean stones, boolean pounds) {
            this.measureFormat = measureFormat;
            this.stones = stones;
            this.pounds = pounds;
        }

        @Override
        public String getFormattedValue(float value) {
            if (stones)
            {
                value *= 2.20462262f;
                return String.format(measureFormat, Math.floor(value / 14), value % 14);
            }
            else if (pounds)
            {
                return String.format(measureFormat, value * 2.20462262);
            }
            else
            {
                return String.format(measureFormat, value);
            }
        }
    }

    public static class MyMarkerView extends MarkerView {

        private final TextView tvContent;
        private final String measureFormat;
        private final boolean stones;
        private final boolean pounds;

        public MyMarkerView(Context context, int layoutResource, String measureFormat, boolean stones, boolean pounds) {
            super(context, layoutResource);

            tvContent = findViewById(R.id.tvContent);
            this.measureFormat = measureFormat;
            this.stones = stones;
            this.pounds = pounds;
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            if (stones)
            {
                float y = e.getY() * 2.20462262f;
                tvContent.setText(String.format(measureFormat, y / 14, y % 14));
            }
            else if (pounds)
            {
                tvContent.setText(String.format(measureFormat, e.getY() * 2.20462262));
            }
            else
                tvContent.setText(String.format(measureFormat, e.getY()));
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF((float)-(getWidth() / 2), -getHeight());
        }
    }
}
