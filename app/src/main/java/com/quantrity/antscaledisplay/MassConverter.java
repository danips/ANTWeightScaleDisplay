package com.quantrity.antscaledisplay;

/** Converts between canonical kilograms and the mass values shown or entered by a user. */
final class MassConverter {
    static final double POUNDS_PER_KILOGRAM = 2.20462262;
    static final double POUNDS_PER_STONE = 14.0;

    static final class StonePounds {
        final double stones;
        final double pounds;

        StonePounds(double stones, double pounds) {
            this.stones = stones;
            this.pounds = pounds;
        }
    }

    private MassConverter() {}

    static double kilogramsToPounds(double kilograms) {
        return kilograms * POUNDS_PER_KILOGRAM;
    }

    static double poundsToKilograms(double pounds) {
        return pounds / POUNDS_PER_KILOGRAM;
    }

    /** Returns kilograms for KG users and pounds for LB/ST users. */
    static double toDisplayMass(double kilograms, User.MassUnit unit) {
        return unit == User.MassUnit.LB || unit == User.MassUnit.ST
                ? kilogramsToPounds(kilograms) : kilograms;
    }

    /** Accepts kilograms for KG users and pounds for LB/ST users. */
    static double toKilograms(double displayMass, User.MassUnit unit) {
        return unit == User.MassUnit.LB || unit == User.MassUnit.ST
                ? poundsToKilograms(displayMass) : displayMass;
    }

    static StonePounds toStonePounds(double kilograms) {
        return splitPounds(kilogramsToPounds(kilograms));
    }

    static StonePounds splitPounds(double pounds) {
        double stones = pounds < 0
                ? Math.ceil(pounds / POUNDS_PER_STONE)
                : Math.floor(pounds / POUNDS_PER_STONE);
        return new StonePounds(stones, pounds - stones * POUNDS_PER_STONE);
    }

    static double stonePoundsToKilograms(double stones, double pounds) {
        return poundsToKilograms(stones * POUNDS_PER_STONE + pounds);
    }

    static double percentageToDisplayMass(double percentage, double bodyKilograms,
                                          User.MassUnit unit) {
        return toDisplayMass(percentage * bodyKilograms / 100.0, unit);
    }

    static double displayMassToPercentage(double displayMass, double bodyKilograms,
                                          User.MassUnit unit) {
        return toKilograms(displayMass, unit) * 100.0 / bodyKilograms;
    }
}
