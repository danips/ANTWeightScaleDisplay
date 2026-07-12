package com.quantrity.antscaledisplay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

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
import com.quantrity.antscaledisplay.databinding.FragmentGraphsBinding;
import com.quantrity.antscaledisplay.databinding.CustomMarkerViewBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GraphsFragment extends Fragment implements OnChartGestureListener, MenuProvider {
    private final static String TAG = "GraphsFragment";

    private LinearLayout graphLayout;
    private FragmentGraphsBinding binding;

    private User the_user;
    private ArrayList<Weight> weights;
    private AppStateViewModel state;
    private int graph_measurement_displayed;
    private int graph_period_displayed;
    private MenuItem measurement_selection;
    private SubMenu measurement_items;
    private MenuItem period_selection;
    private SubMenu period_items;

    private LineChart mChart;
    private List<GraphPoint> graphPoints;
    private boolean averageShowing = false;
    private final CountDownTimer cdt = new CountDownTimer(100, 100) {
        @Override
        public void onTick(long l) {}

        @Override
        public void onFinish() {
            if (mChart == null || graphPoints == null || graphPoints.isEmpty()) return;
            ViewPortHandler handler = mChart.getViewPortHandler();
            MPPointD topLeft = mChart.getValuesByTouchPoint(handler.contentLeft(), handler.contentTop(), YAxis.AxisDependency.LEFT);
            MPPointD bottomRight = mChart.getValuesByTouchPoint(handler.contentRight(), handler.contentBottom(), YAxis.AxisDependency.LEFT);
            double end = Math.min(bottomRight.x, graphPoints.get(graphPoints.size() - 1).x);
            addAverage(end - topLeft.x, end);
        }
    };

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
        binding = FragmentGraphsBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        graphLayout = binding.graph;

        if (getActivity() != null) {
            weights = state.selectedWeights();
            the_user = state.selectedUser();

            // Replaced deprecated PreferenceManager.getDefaultSharedPreferences
            SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
            graph_measurement_displayed = settings.getInt("selected_graph_measurement", R.id.graph_weight);
            graph_period_displayed = settings.getInt("selected_graph_period", R.id.graph_time_month);
            loadGraph(graph_measurement_displayed, graph_period_displayed);
        }

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        cdt.cancel();
        mChart = null;
        graphLayout = null;
        measurement_selection = null;
        measurement_items = null;
        period_selection = null;
        period_items = null;
        binding = null;
        super.onDestroyView();
    }

    private void updateActionBar() {
        Set<Metric> available = MeasurementPresentationFactory.availableMetrics(weights);
        if (measurement_items != null) {
            for (int i = 0; i < measurement_items.size(); i++) {
                Metric metric = Metric.fromGraphId(measurement_items.getItem(i).getItemId());
                measurement_items.getItem(i).setVisible(metric != null && available.contains(metric));
            }
        }
        measurement_selection.setVisible(!available.isEmpty());


        if (weights.size() > 1) {
            long period = weights.get(0).date - weights.get(weights.size() - 1).date;
            period_selection.setVisible(period > 604800000L);
            for (int i = 0; i < period_items.size(); i++) {
                GraphPeriod graphPeriod = GraphPeriod.fromMenuId(period_items.getItem(i).getItemId());
                period_items.getItem(i).setVisible(graphPeriod.isAvailable(period));
            }
        } else period_selection.setVisible(false);
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if ((view != null) && (getActivity() != null)) {
                User user = (User)adapterView.getItemAtPosition(i);
                state.selectUser(user);

                weights = state.selectedWeights();
                the_user = user;
                updateActionBar();
                loadGraph(graph_measurement_displayed, graph_period_displayed);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_graphs_menu, menu);

        if (getActivity() != null)
            ((MainActivity)getActivity()).addUsersSpinner(menu, oisListener);

        period_selection = menu.findItem(R.id.action_graph_param_time);
        period_items = period_selection.getSubMenu();

        measurement_selection = menu.findItem(R.id.action_graph_param);
        setIcon(graph_measurement_displayed);
        measurement_items = measurement_selection.getSubMenu();
        if (measurement_items != null) {
            for (int i = 0; i < measurement_items.size(); i++) {
                turnWhite(measurement_items.getItem(i).getIcon());
            }
        }

        updateActionBar();
    }

    private void setIcon(int itemid) {
        Metric metric = Metric.fromGraphId(itemid);
        measurement_selection.setIcon(metric == null ? Metric.WEIGHT.getIconRes() : metric.getIconRes());
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
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (getActivity() == null) return false;

        if (GraphPeriod.isPeriodMenuId(itemId)) {
            if (graph_period_displayed != menuItem.getItemId()) {
                SharedPreferences settings1 = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor1 = settings1.edit();
                editor1.putInt("selected_graph_period", menuItem.getItemId());
                editor1.apply();
                loadGraph(graph_measurement_displayed, menuItem.getItemId());
            }
        } else if (Metric.fromGraphId(itemId) != null) {
            if (graph_measurement_displayed != menuItem.getItemId()) {
                SharedPreferences settings2 = getActivity().getSharedPreferences(getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor2 = settings2.edit();
                editor2.putInt("selected_graph_measurement", menuItem.getItemId());
                editor2.apply();
                setIcon(menuItem.getItemId());
                loadGraph(menuItem.getItemId(), graph_period_displayed);
            }
        }
        return false;
    }

    private void loadGraph(int item_id, int period_id) {
        graph_measurement_displayed = item_id;
        graph_period_displayed = period_id;
        Metric metric = Metric.fromGraphId(item_id);
        if (metric == null) metric = Metric.WEIGHT;
        GraphPeriod period = GraphPeriod.fromMenuId(period_id);

        graphLayout.removeAllViews();
        graphPoints = GraphSeriesBuilder.rawPoints(weights, metric, the_user);
        if (graphPoints.isEmpty()) {
            if (Debug.ON) Log.v(TAG, "(data.size() == 0) CLEAR GRAPH");
            return;
        }
        double oldest = graphPoints.get(0).x;
        double newest = graphPoints.get(graphPoints.size() - 1).x;
        float window = period.windowDays((long) oldest, (long) newest);

        int color = metric.getGraphColor();
        int color2 = metric.getGraphFillColor();
        String measureFormat = "%.01f";
        boolean stones = false;
        boolean pounds = false;
        Metric.Unit unit = metric.displayedUnit(the_user.show_fat_mass);
        if (unit == Metric.Unit.MASS) {
            measureFormat = getString((the_user.mass_unit == User.MassUnit.LB) ? R.string.edit_user_fragment_units_tag_lb : (the_user.mass_unit == User.MassUnit.ST) ? R.string.edit_user_fragment_units_tag_st : R.string.edit_user_fragment_units_tag_kg);
            stones = (the_user.mass_unit == User.MassUnit.ST);
            pounds = (the_user.mass_unit == User.MassUnit.LB);
        } else if (unit == Metric.Unit.PERCENT) {
            measureFormat = getString(R.string.weight_fragment_percent_tag);
        } else if (unit == Metric.Unit.ENERGY) {
            measureFormat = getString(R.string.weight_fragment_kcal_tag);
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


        ArrayList<Entry> dataEntries = entries(graphPoints);
        ArrayList<Entry> rollingEntries = entries(GraphSeriesBuilder.rollingAverage(
                graphPoints, period.rollingWindowMillis((long) oldest, (long) newest)));
        addAverage(window * GraphPeriod.DAY_MILLIS, newest);

        LineDataSet rollingAvgSeries = new LineDataSet(rollingEntries, null);
        rollingAvgSeries.setDrawCircles(false);
        rollingAvgSeries.setDrawValues(false);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.7f;
        rollingAvgSeries.setLineWidth(2f);
        rollingAvgSeries.setColor(Color.HSVToColor(hsv));
        rollingAvgSeries.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet series = new LineDataSet(dataEntries, null);
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
            for (GraphSeriesBuilder.GoalSeries goalPoints : GraphSeriesBuilder.goalSeries(
                    state.goals(), the_user.uuid, metric, the_user.show_fat_mass)) {
                ArrayList<Entry> goalEntries = new ArrayList<>();
                goalEntries.add(entry(goalPoints.start));
                goalEntries.add(entry(goalPoints.end));
                LineDataSet goalDataSet = new LineDataSet(goalEntries, null);
                goalDataSet.setColor(goalPoints.color);
                goalDataSet.setLineWidth(1f);
                goalDataSet.enableDashedLine(8f, 5f, 0f);
                goalDataSet.setDrawCircles(false);
                goalDataSet.setDrawValues(false);
                dataSets.add(goalDataSet);
            }
        }


        // create a data object with the datasets
        LineData data = new LineData(dataSets);
        mChart.setData(data);

        // get the legend (only possible after setting data)
        mChart.getLegend().setEnabled(false);

        // now modify viewport
        float data_window = (float) ((newest - oldest) / GraphPeriod.DAY_MILLIS);
        mChart.zoom((data_window/window),1,0,0);

        mChart.moveViewToX((float) newest);

        mChart.setOnChartGestureListener(this);
    }

    private static ArrayList<Entry> entries(List<GraphPoint> points) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (GraphPoint point : points) entries.add(entry(point));
        return entries;
    }

    private static Entry entry(GraphPoint point) {
        return new Entry((float) point.x, (float) point.y);
    }

    private void addAverage(double span, double end) {
        if (mChart != null) {
            double averageValue = GraphSeriesBuilder.visibleAverage(graphPoints, end - span, end);
            LimitLine average = new LimitLine((float) averageValue, "Average");
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
                MassConverter.StonePounds stonePounds =
                        MassConverter.toStonePounds(value);
                return String.format(measureFormat,
                        stonePounds.stones, stonePounds.pounds);
            }
            else if (pounds)
            {
                return String.format(measureFormat,
                        MassConverter.kilogramsToPounds(value));
            }
            else
            {
                return String.format(measureFormat, value);
            }
        }
    }

    @SuppressLint("ViewConstructor")
    public static class MyMarkerView extends MarkerView {

        private final TextView tvContent;
        private final String measureFormat;
        private final boolean stones;
        private final boolean pounds;

        public MyMarkerView(Context context, int layoutResource, String measureFormat, boolean stones, boolean pounds) {
            super(context, layoutResource);

            tvContent = CustomMarkerViewBinding.bind(this).tvContent;
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
                double poundsValue = MassConverter.kilogramsToPounds(e.getY());
                tvContent.setText(String.format(measureFormat,
                        poundsValue / MassConverter.POUNDS_PER_STONE,
                        poundsValue % MassConverter.POUNDS_PER_STONE));
            }
            else if (pounds)
            {
                tvContent.setText(String.format(measureFormat,
                        MassConverter.kilogramsToPounds(e.getY())));
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
