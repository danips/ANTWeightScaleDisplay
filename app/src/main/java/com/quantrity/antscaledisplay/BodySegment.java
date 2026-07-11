package com.quantrity.antscaledisplay;

/** Pairs the fat and muscle measurements belonging to each body segment. */
public enum BodySegment {
    TRUNK(R.drawable.ic_trunk, R.string.weight_fragment_icon_desc_trunk,
            Metric.TRUNKPERCENTFAT, Metric.TRUNKMUSCLEMASS),
    LEFT_ARM(R.drawable.ic_left_arm, R.string.weight_fragment_icon_desc_leftArm,
            Metric.LEFTARMPERCENTFAT, Metric.LEFTARMMUSCLEMASS),
    RIGHT_ARM(R.drawable.ic_right_arm, R.string.weight_fragment_icon_desc_rightArm,
            Metric.RIGHTARMPERCENTFAT, Metric.RIGHTARMMUSCLEMASS),
    LEFT_LEG(R.drawable.ic_left_leg, R.string.weight_fragment_icon_desc_leftLeg,
            Metric.LEFTLEGPERCENTFAT, Metric.LEFTLEGMUSCLEMASS),
    RIGHT_LEG(R.drawable.ic_right_leg, R.string.weight_fragment_icon_desc_rightLeg,
            Metric.RIGHTLEGPERCENTFAT, Metric.RIGHTLEGMUSCLEMASS);

    public final int iconRes;
    public final int labelRes;
    public final Metric fatMetric;
    public final Metric muscleMetric;

    BodySegment(int iconRes, int labelRes, Metric fatMetric, Metric muscleMetric) {
        this.iconRes = iconRes;
        this.labelRes = labelRes;
        this.fatMetric = fatMetric;
        this.muscleMetric = muscleMetric;
    }
}
