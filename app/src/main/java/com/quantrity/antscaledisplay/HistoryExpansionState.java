package com.quantrity.antscaledisplay;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Expansion ownership keyed by the persisted measurement identity. */
final class HistoryExpansionState {
    private final Set<RowKey> expanded = new HashSet<>();

    boolean isExpanded(Weight weight) {
        return weight != null && expanded.contains(RowKey.from(weight));
    }

    boolean toggle(Weight weight) {
        if (weight == null || !weight.hasAdditionalMeasurements()) return false;
        RowKey key = RowKey.from(weight);
        if (expanded.remove(key)) return false;
        expanded.add(key);
        return true;
    }

    void retainAll(List<Weight> weights) {
        Set<RowKey> retained = new HashSet<>();
        if (weights != null) {
            for (Weight weight : weights) {
                if (weight != null && weight.hasAdditionalMeasurements()) {
                    retained.add(RowKey.from(weight));
                }
            }
        }
        expanded.retainAll(retained);
    }

    int expandedCount() {
        return expanded.size();
    }

    private static final class RowKey {
        private final String userUuid;
        private final long date;

        private RowKey(String userUuid, long date) {
            this.userUuid = userUuid;
            this.date = date;
        }

        static RowKey from(Weight weight) {
            return new RowKey(weight.uuid, weight.date);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RowKey)) return false;
            RowKey key = (RowKey) other;
            return date == key.date && Objects.equals(userUuid, key.userUuid);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(userUuid) + Long.hashCode(date);
        }
    }
}
