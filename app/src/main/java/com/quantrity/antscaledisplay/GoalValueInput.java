package com.quantrity.antscaledisplay;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Binds the three alternative layouts used to edit one goal endpoint. */
final class GoalValueInput {
    private final LinearLayout unitlessLayout;
    private final LinearLayout singleLayout;
    private final LinearLayout stoneLayout;
    private final EditText unitlessValue;
    private final EditText singleValue;
    private final EditText stonesValue;
    private final EditText poundsValue;
    private final TextView singleUnit;
    private final TextView stonesUnit;
    private final TextView poundsUnit;
    private GoalValueDefinition definition;

    GoalValueInput(LinearLayout unitlessLayout, LinearLayout singleLayout,
                   LinearLayout stoneLayout, EditText unitlessValue, EditText singleValue,
                   EditText stonesValue, EditText poundsValue, TextView singleUnit,
                   TextView stonesUnit, TextView poundsUnit) {
        this.unitlessLayout = unitlessLayout;
        this.singleLayout = singleLayout;
        this.stoneLayout = stoneLayout;
        this.unitlessValue = unitlessValue;
        this.singleValue = singleValue;
        this.stonesValue = stonesValue;
        this.poundsValue = poundsValue;
        this.singleUnit = singleUnit;
        this.stonesUnit = stonesUnit;
        this.poundsUnit = poundsUnit;
    }

    void configure(Metric metric, User user, boolean showFatMass) {
        definition = GoalValueDefinition.forMetric(metric, user.mass_unit, showFatMass);
        unitlessLayout.setVisibility(definition.mode == GoalValueDefinition.Mode.UNITLESS
                ? View.VISIBLE : View.GONE);
        singleLayout.setVisibility(definition.mode == GoalValueDefinition.Mode.SINGLE_UNIT
                ? View.VISIBLE : View.GONE);
        stoneLayout.setVisibility(definition.mode == GoalValueDefinition.Mode.STONE_POUNDS
                ? View.VISIBLE : View.GONE);
        if (definition.primaryUnitResource != 0) {
            TextView label = definition.mode == GoalValueDefinition.Mode.STONE_POUNDS
                    ? stonesUnit : singleUnit;
            label.setText(definition.primaryUnitResource);
        }
        if (definition.secondaryUnitResource != 0) {
            poundsUnit.setText(definition.secondaryUnitResource);
        }
    }

    void setCanonicalValue(double value) {
        clearErrors();
        if (definition.mode == GoalValueDefinition.Mode.UNITLESS) {
            unitlessValue.setText(definition.format(value));
        } else if (definition.mode == GoalValueDefinition.Mode.SINGLE_UNIT) {
            singleValue.setText(definition.format(value));
        } else {
            stonesValue.setText(definition.formatStones(value));
            poundsValue.setText(definition.formatPounds(value));
        }
    }

    LocalizedNumberParser.Result readCanonicalValue() {
        if (definition.mode == GoalValueDefinition.Mode.UNITLESS) {
            return parseSingle(unitlessValue);
        }
        if (definition.mode == GoalValueDefinition.Mode.SINGLE_UNIT) {
            return parseSingle(singleValue);
        }
        LocalizedNumberParser.Result stones = parse(stonesValue);
        LocalizedNumberParser.Result pounds = parse(poundsValue);
        if (!stones.isValid() || !pounds.isValid()) return LocalizedNumberParser.Result.invalid();
        return LocalizedNumberParser.Result.valid(
                definition.toCanonical(stones.value(), pounds.value()));
    }

    void clear() {
        unitlessValue.setText("");
        singleValue.setText("");
        stonesValue.setText("");
        poundsValue.setText("");
        clearErrors();
    }

    private LocalizedNumberParser.Result parseSingle(EditText input) {
        LocalizedNumberParser.Result result = parse(input);
        return result.isValid()
                ? LocalizedNumberParser.Result.valid(definition.toCanonical(result.value()))
                : result;
    }

    private LocalizedNumberParser.Result parse(EditText input) {
        LocalizedNumberParser.Result result =
                LocalizedNumberParser.parse(input.getText().toString());
        if (!result.isValid()) input.setError(input.getContext().getString(
                R.string.edit_goal_invalid_number_error));
        return result;
    }

    private void clearErrors() {
        unitlessValue.setError(null);
        singleValue.setError(null);
        stonesValue.setError(null);
        poundsValue.setError(null);
    }
}
