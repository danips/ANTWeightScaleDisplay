package com.quantrity.antscaledisplay;

import java.util.Locale;

/** Defines how each manually editable metric maps to a Weight and the user's display units. */
enum EditableWeightMetric {
    WEIGHT(Metric.WEIGHT, (weight, value) -> weight.weight = value),
    PERCENT_FAT(Metric.PERCENTFAT, (weight, value) -> weight.percentFat = value),
    PERCENT_HYDRATION(Metric.PERCENTHYDRATION,
            (weight, value) -> weight.percentHydration = value),
    BONE_MASS(Metric.BONEMASS, (weight, value) -> weight.boneMass = value),
    MUSCLE_MASS(Metric.MUSCLEMASS, (weight, value) -> weight.muscleMass = value),
    PHYSIQUE_RATING(Metric.PHYSIQUERATING,
            (weight, value) -> weight.physiqueRating = (int) value),
    VISCERAL_FAT(Metric.VISCERALFATRATING,
            (weight, value) -> weight.visceralFatRating = value),
    METABOLIC_AGE(Metric.METABOLICAGE,
            (weight, value) -> weight.metabolicAge = (int) value),
    ACTIVE_MET(Metric.ACTIVEMET, (weight, value) -> weight.activeMet = value),
    BASAL_MET(Metric.BASALMET, (weight, value) -> weight.basalMet = value),
    TRUNK_PERCENT_FAT(Metric.TRUNKPERCENTFAT,
            (weight, value) -> weight.trunkPercentFat = value),
    TRUNK_MUSCLE_MASS(Metric.TRUNKMUSCLEMASS,
            (weight, value) -> weight.trunkMuscleMass = value),
    LEFT_ARM_PERCENT_FAT(Metric.LEFTARMPERCENTFAT,
            (weight, value) -> weight.leftArmPercentFat = value),
    LEFT_ARM_MUSCLE_MASS(Metric.LEFTARMMUSCLEMASS,
            (weight, value) -> weight.leftArmMuscleMass = value),
    RIGHT_ARM_PERCENT_FAT(Metric.RIGHTARMPERCENTFAT,
            (weight, value) -> weight.rightArmPercentFat = value),
    RIGHT_ARM_MUSCLE_MASS(Metric.RIGHTARMMUSCLEMASS,
            (weight, value) -> weight.rightArmMuscleMass = value),
    LEFT_LEG_PERCENT_FAT(Metric.LEFTLEGPERCENTFAT,
            (weight, value) -> weight.leftLegPercentFat = value),
    LEFT_LEG_MUSCLE_MASS(Metric.LEFTLEGMUSCLEMASS,
            (weight, value) -> weight.leftLegMuscleMass = value),
    RIGHT_LEG_PERCENT_FAT(Metric.RIGHTLEGPERCENTFAT,
            (weight, value) -> weight.rightLegPercentFat = value),
    RIGHT_LEG_MUSCLE_MASS(Metric.RIGHTLEGMUSCLEMASS,
            (weight, value) -> weight.rightLegMuscleMass = value);

    private interface Setter {
        void set(Weight weight, double value);
    }

    final Metric metric;
    private final Setter setter;

    EditableWeightMetric(Metric metric, Setter setter) {
        this.metric = metric;
        this.setter = setter;
    }

    double value(Weight weight) {
        return metric.value(weight);
    }

    void set(Weight weight, double value) {
        setter.set(weight, value);
    }

    boolean acceptsDecimalInput() {
        return metric != Metric.PHYSIQUERATING
                && metric.getUnit() != Metric.Unit.YEARS
                && metric.getUnit() != Metric.Unit.ENERGY;
    }

    String displayText(Weight weight, User user) {
        double value = value(weight);
        if (value == -1) return "";
        double displayed = toDisplayValue(value, weight.weight, user);
        String pattern = acceptsDecimalInput() ? "%.1f" : "%.0f";
        return String.format(Locale.getDefault(), pattern, displayed);
    }

    String hint() {
        return String.format(Locale.getDefault(), acceptsDecimalInput() ? "%.2f" : "%.0f", 0f);
    }

    double toCanonicalValue(double displayed, double bodyKilograms, User user) {
        if (displayed == -1) return -1;
        if (metric.percentageMayBeMass() && user.show_fat_mass) {
            return MassConverter.displayMassToPercentage(
                    displayed, bodyKilograms, user.mass_unit);
        }
        if (metric.getUnit() == Metric.Unit.MASS) {
            return MassConverter.toKilograms(displayed, user.mass_unit);
        }
        return displayed;
    }

    int unitResource(User user) {
        Metric.Unit unit = metric.displayedUnit(user.show_fat_mass);
        if (unit == Metric.Unit.PERCENT) return R.string.weight_edit_fragment_percent_tag;
        if (unit == Metric.Unit.YEARS) return R.string.weight_edit_fragment_years_tag;
        if (unit == Metric.Unit.ENERGY) return R.string.weight_edit_fragment_kcal_tag;
        if (unit != Metric.Unit.MASS) return 0;
        return user.mass_unit == User.MassUnit.LB || user.mass_unit == User.MassUnit.ST
                ? R.string.weight_edit_fragment_lb_tag
                : R.string.weight_edit_fragment_kg_tag;
    }

    double invalidFallback() {
        return metric.getUnit() == Metric.Unit.ENERGY ? 0 : -1;
    }

    private double toDisplayValue(double canonical, double bodyKilograms, User user) {
        if (metric.percentageMayBeMass() && user.show_fat_mass) {
            return MassConverter.percentageToDisplayMass(
                    canonical, bodyKilograms, user.mass_unit);
        }
        if (metric.getUnit() == Metric.Unit.MASS) {
            return MassConverter.toDisplayMass(canonical, user.mass_unit);
        }
        return canonical;
    }
}
