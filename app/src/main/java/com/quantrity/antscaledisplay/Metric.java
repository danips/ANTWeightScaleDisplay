package com.quantrity.antscaledisplay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Authoritative definition of every measurement understood by the application. */
public enum Metric {
    UNDEFINED(-1, 0, 0, 0, Unit.NONE, false, 0, weight -> -1),
    HEIGHT(0, 0, 0, 0, Unit.NONE, false, 0, weight -> weight.height),
    WEIGHT(1, R.id.graph_weight, R.drawable.ic_weight, R.string.weight_fragment_icon_desc_weight,
            Unit.MASS, false, 0xFF34A5DE, weight -> weight.weight),
    BMI(2, R.id.graph_bmi, R.drawable.ic_bmi, R.string.weight_fragment_bmi_tag,
            Unit.NONE, false, 0xFFDE3450, weight -> weight.weight == -1 || weight.height <= 0
                    ? -1 : weight.weight / Math.pow(weight.height / 100, 2)),
    PERCENTFAT(3, R.id.graph_percentFat, R.drawable.ic_percent_fat,
            R.string.weight_fragment_icon_desc_percentFat, Unit.PERCENT, true, 0xFFFF9C13,
            weight -> weight.percentFat),
    PERCENTHYDRATION(4, R.id.graph_percentHydration, R.drawable.ic_percent_hydration,
            R.string.weight_fragment_icon_desc_percentHydration, Unit.PERCENT, false, 0xFF3DC4D4,
            weight -> weight.percentHydration),
    BONEMASS(5, R.id.graph_boneMass, R.drawable.ic_bone_mass,
            R.string.weight_fragment_icon_desc_boneMass, Unit.MASS, false, 0xFF94857D,
            weight -> weight.boneMass),
    MUSCLEMASS(6, R.id.graph_muscleMass, R.drawable.ic_muscle_mass,
            R.string.weight_fragment_icon_desc_muscleMass, Unit.MASS, false, 0xFFB35FAE,
            weight -> weight.muscleMass),
    PHYSIQUERATING(7, R.id.graph_physiqueRating, R.drawable.ic_physique_rating,
            R.string.weight_fragment_icon_desc_physiqueRating, Unit.NONE, false, 0xFF578CCF,
            weight -> weight.physiqueRating),
    VISCERALFATRATING(8, R.id.graph_visceralFatRating, R.drawable.ic_visceral_fat_rating,
            R.string.weight_fragment_icon_desc_visceralFat, Unit.NONE, false, 0xFFE06E3A,
            weight -> weight.visceralFatRating),
    METABOLICAGE(9, R.id.graph_metabolicAge, R.drawable.ic_metabolic_age,
            R.string.weight_fragment_icon_desc_metabolicAge, Unit.YEARS, false, 0xFF72C250,
            weight -> weight.metabolicAge),
    ACTIVEMET(10, R.id.graph_activeMet, R.drawable.ic_metabolic,
            R.string.weight_fragment_icon_desc_activeMet, Unit.ENERGY, false, 0xFF3BD743,
            weight -> weight.activeMet),
    BASALMET(11, R.id.graph_basalMet, R.drawable.ic_metabolic,
            R.string.weight_fragment_icon_desc_basalMet, Unit.ENERGY, false, 0xFFD73BCF,
            weight -> weight.basalMet),
    TRUNKPERCENTFAT(12, R.id.graph_trunkFatPercent, R.drawable.ic_trunk,
            R.string.graphs_fragment_measurement_trunk_percent_fat, Unit.PERCENT, true, 0xFF34A5DE,
            weight -> weight.trunkPercentFat),
    TRUNKMUSCLEMASS(13, R.id.graph_trunkMuscleMass, R.drawable.ic_trunk,
            R.string.graphs_fragment_measurement_trunk_muscle_mass, Unit.MASS, false, 0xFF34A5DE,
            weight -> weight.trunkMuscleMass),
    LEFTARMPERCENTFAT(14, R.id.graph_leftArmFatPercent, R.drawable.ic_left_arm,
            R.string.graphs_fragment_measurement_left_arm_percent_fat, Unit.PERCENT, true, 0xFF34A5DE,
            weight -> weight.leftArmPercentFat),
    LEFTARMMUSCLEMASS(15, R.id.graph_leftArmMuscleMass, R.drawable.ic_left_arm,
            R.string.graphs_fragment_measurement_left_arm_muscle_mass, Unit.MASS, false, 0xFF34A5DE,
            weight -> weight.leftArmMuscleMass),
    RIGHTARMPERCENTFAT(16, R.id.graph_rightArmFatPercent, R.drawable.ic_right_arm,
            R.string.graphs_fragment_measurement_right_arm_percent_fat, Unit.PERCENT, true, 0xFF34A5DE,
            weight -> weight.rightArmPercentFat),
    RIGHTARMMUSCLEMASS(17, R.id.graph_rightArmMuscleMass, R.drawable.ic_right_arm,
            R.string.graphs_fragment_measurement_right_arm_muscle_mass, Unit.MASS, false, 0xFF34A5DE,
            weight -> weight.rightArmMuscleMass),
    LEFTLEGPERCENTFAT(18, R.id.graph_leftLegFatPercent, R.drawable.ic_left_leg,
            R.string.graphs_fragment_measurement_left_leg_percent_fat, Unit.PERCENT, true, 0xFF34A5DE,
            weight -> weight.leftLegPercentFat),
    LEFTLEGMUSCLEMASS(19, R.id.graph_leftLegMuscleMass, R.drawable.ic_left_leg,
            R.string.graphs_fragment_measurement_left_leg_muscle_mass, Unit.MASS, false, 0xFF34A5DE,
            weight -> weight.leftLegMuscleMass),
    RIGHTLEGPERCENTFAT(20, R.id.graph_rightLegFatPercent, R.drawable.ic_right_leg,
            R.string.graphs_fragment_measurement_right_leg_percent_fat, Unit.PERCENT, true, 0xFF34A5DE,
            weight -> weight.rightLegPercentFat),
    RIGHTLEGMUSCLEMASS(21, R.id.graph_rightLegMuscleMass, R.drawable.ic_right_leg,
            R.string.graphs_fragment_measurement_right_leg_muscle_mass, Unit.MASS, false, 0xFF34A5DE,
            weight -> weight.rightLegMuscleMass);

    public enum Unit { NONE, MASS, PERCENT, YEARS, ENERGY }

    private interface Extractor { double extract(Weight weight); }

    private static final List<Metric> GOAL_METRICS = Collections.unmodifiableList(
            Arrays.asList(Arrays.copyOfRange(values(), 2, values().length)));
    private static final List<Metric> EXPORT_METRICS = Collections.unmodifiableList(Arrays.asList(
            WEIGHT, PERCENTFAT, PERCENTHYDRATION, MUSCLEMASS, PHYSIQUERATING,
            VISCERALFATRATING, BONEMASS, METABOLICAGE, BASALMET, ACTIVEMET,
            TRUNKPERCENTFAT, TRUNKMUSCLEMASS, LEFTARMPERCENTFAT, LEFTARMMUSCLEMASS,
            RIGHTARMPERCENTFAT, RIGHTARMMUSCLEMASS, LEFTLEGPERCENTFAT, LEFTLEGMUSCLEMASS,
            RIGHTLEGPERCENTFAT, RIGHTLEGMUSCLEMASS));
    private static final List<Metric> EMAIL_METRICS = Collections.unmodifiableList(Arrays.asList(
            WEIGHT, PERCENTFAT, PERCENTHYDRATION,
            TRUNKPERCENTFAT, TRUNKMUSCLEMASS, LEFTARMPERCENTFAT, LEFTARMMUSCLEMASS,
            RIGHTARMPERCENTFAT, RIGHTARMMUSCLEMASS, LEFTLEGPERCENTFAT, LEFTLEGMUSCLEMASS,
            RIGHTLEGPERCENTFAT, RIGHTLEGMUSCLEMASS, BONEMASS, MUSCLEMASS,
            PHYSIQUERATING, VISCERALFATRATING, METABOLICAGE, ACTIVEMET, BASALMET));

    private final int metricCode;
    private final int graphId;
    private final int iconRes;
    private final int labelRes;
    private final Unit unit;
    private final boolean percentageMayBeMass;
    private final int graphColor;
    private final Extractor extractor;

    Metric(int metricCode, int graphId, int iconRes, int labelRes, Unit unit,
           boolean percentageMayBeMass, int graphColor, Extractor extractor) {
        this.metricCode = metricCode;
        this.graphId = graphId;
        this.iconRes = iconRes;
        this.labelRes = labelRes;
        this.unit = unit;
        this.percentageMayBeMass = percentageMayBeMass;
        this.graphColor = graphColor;
        this.extractor = extractor;
    }

    public int getMetricCode() { return metricCode; }
    public int getGraphId() { return graphId; }
    public int getIconRes() { return iconRes; }
    public int getLabelRes() { return labelRes; }
    public Unit getUnit() { return unit; }
    public int getGraphColor() { return graphColor; }
    public int getGraphFillColor() { return (graphColor & 0x00FFFFFF) | (150 << 24); }
    public boolean percentageMayBeMass() { return percentageMayBeMass; }

    public double value(Weight weight) {
        return weight == null ? -1 : extractor.extract(weight);
    }

    /** Value stored by goals: mass metrics are always kilograms. */
    public double goalValue(Weight weight, boolean showFatMass) {
        double value = value(weight);
        if (value == -1) return -1;
        return percentageMayBeMass && showFatMass ? value * weight.weight / 100 : value;
    }

    /** Value fed to the graph, preserving its existing pounds/stones conversion contract. */
    public double graphValue(Weight weight, User user) {
        double value = value(weight);
        if (value == -1) return -1;
        if (!percentageMayBeMass || !user.show_fat_mass) return value;
        return MassConverter.percentageToDisplayMass(value, weight.weight, user.mass_unit);
    }

    public Unit displayedUnit(boolean showFatMass) {
        return percentageMayBeMass && showFatMass ? Unit.MASS : unit;
    }

    public static Metric fromGraphId(int graphId) {
        for (Metric metric : GOAL_METRICS) if (metric.graphId == graphId) return metric;
        return null;
    }

    public static Metric fromGoalPosition(int position) {
        return position >= 0 && position < GOAL_METRICS.size() ? GOAL_METRICS.get(position) : WEIGHT;
    }

    public static List<Metric> goalMetrics() { return GOAL_METRICS; }
    public static List<Metric> exportMetrics() { return EXPORT_METRICS; }
    public static List<Metric> emailMetrics() { return EMAIL_METRICS; }

    public static boolean isSameMetric(Metric metric, int graphId) {
        return metric != null && metric.graphId == graphId;
    }

    // Retained as source-compatible delegates for callers outside the migrated screens.
    public static int getGraph(Metric metric) { return metric == null ? WEIGHT.graphId : metric.graphId; }
    public static int getRes(Metric metric) { return metric == null ? WEIGHT.iconRes : metric.iconRes; }
}
