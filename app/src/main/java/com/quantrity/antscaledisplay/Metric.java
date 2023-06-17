package com.quantrity.antscaledisplay;

public enum Metric {
    UNDEFINED (-1),
    HEIGHT (0),
    WEIGHT (1),
    BMI (2),
    PERCENTFAT(3),
    PERCENTHYDRATION(4),
    BONEMASS(5),
    MUSCLEMASS(6),
    PHYSIQUERATING(7),
    VISCERALFATRATING(8),
    METABOLICAGE(9),
    ACTIVEMET(10),
    BASALMET(11),
    TRUNKPERCENTFAT(12),
    TRUNKMUSCLEMASS(13),
    LEFTARMPERCENTFAT(14),
    LEFTARMMUSCLEMASS(15),
    RIGHTARMPERCENTFAT(16),
    RIGHTARMMUSCLEMASS(17),
    LEFTLEGPERCENTFAT(18),
    LEFTLEGMUSCLEMASS(19),
    RIGHTLEGPERCENTFAT(20),
    RIGHTLEGMUSCLEMASS(21),
    //ACTIVITYLEVEL(22)
    ;
    //private static final String TAG = "Metric";

    private final int metricCode;

    Metric(int metricCode) {
        this.metricCode = metricCode;
    }

    public int getMetricCode() {
        return this.metricCode;
    }

    public static boolean isSameMetric(Metric m, int id)
    {
        int res = getGraph(m);
        return (res == id);
    }

    public static int getGraph(Metric m)
    {
        switch (m) {
            case BMI: return R.id.graph_bmi;
            case PERCENTFAT: return R.id.graph_percentFat;
            case PERCENTHYDRATION: return R.id.graph_percentHydration;
            case BONEMASS: return R.id.graph_boneMass;
            case MUSCLEMASS: return R.id.graph_muscleMass;
            case PHYSIQUERATING: return R.id.graph_physiqueRating;
            case VISCERALFATRATING: return R.id.graph_visceralFatRating;
            case METABOLICAGE: return R.id.graph_metabolicAge;
            case ACTIVEMET: return R.id.graph_activeMet;
            case BASALMET: return R.id.graph_basalMet;
            case TRUNKPERCENTFAT: return R.id.graph_trunkFatPercent;
            case TRUNKMUSCLEMASS: return R.id.graph_trunkMuscleMass;
            case LEFTARMPERCENTFAT: return R.id.graph_leftArmFatPercent;
            case LEFTARMMUSCLEMASS: return R.id.graph_leftArmMuscleMass;
            case RIGHTARMPERCENTFAT: return R.id.graph_rightArmFatPercent;
            case RIGHTARMMUSCLEMASS: return R.id.graph_rightArmMuscleMass;
            case LEFTLEGPERCENTFAT: return R.id.graph_leftLegFatPercent;
            case LEFTLEGMUSCLEMASS: return R.id.graph_leftLegMuscleMass;
            case RIGHTLEGPERCENTFAT: return R.id.graph_rightLegFatPercent;
            case RIGHTLEGMUSCLEMASS: return R.id.graph_rightLegMuscleMass;
            case WEIGHT:
            default:
                return R.id.graph_weight;
        }
    }

    public static int getRes(Metric m)
    {
        switch (m) {
            case BMI: return R.drawable.ic_bmi;
            case PERCENTFAT: return R.drawable.ic_percent_fat;
            case PERCENTHYDRATION: return R.drawable.ic_percent_hydration;
            case BONEMASS: return R.drawable.ic_bone_mass;
            case MUSCLEMASS: return R.drawable.ic_muscle_mass;
            case PHYSIQUERATING: return R.drawable.ic_physique_rating;
            case VISCERALFATRATING: return R.drawable.ic_visceral_fat_rating;
            case METABOLICAGE: return R.drawable.ic_metabolic_age;
            case ACTIVEMET:
            case BASALMET: return R.drawable.ic_metabolic;
            case TRUNKPERCENTFAT:
            case TRUNKMUSCLEMASS: return R.drawable.ic_trunk;
            case LEFTARMPERCENTFAT:
            case LEFTARMMUSCLEMASS: return R.drawable.ic_left_arm;
            case RIGHTARMPERCENTFAT:
            case RIGHTARMMUSCLEMASS: return R.drawable.ic_right_arm;
            case LEFTLEGPERCENTFAT:
            case LEFTLEGMUSCLEMASS: return R.drawable.ic_left_leg;
            case RIGHTLEGPERCENTFAT:
            case RIGHTLEGMUSCLEMASS: return R.drawable.ic_right_leg;
            case WEIGHT:
            default:
            return R.drawable.ic_weight;
        }
    }

}
