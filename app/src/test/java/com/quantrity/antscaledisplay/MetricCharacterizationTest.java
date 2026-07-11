package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class MetricCharacterizationTest {
    @Test
    public void metricCodesAreStableAndUnique() {
        Set<Integer> codes = new HashSet<>();
        for (Metric metric : Metric.values()) {
            assertTrue("Duplicate code for " + metric, codes.add(metric.getMetricCode()));
        }
        assertEquals(-1, Metric.UNDEFINED.getMetricCode());
        assertEquals(1, Metric.WEIGHT.getMetricCode());
        assertEquals(21, Metric.RIGHTLEGMUSCLEMASS.getMetricCode());
    }

    @Test
    public void everySupportedMetricMapsToGraphAndIconResources() {
        for (Metric metric : Metric.values()) {
            if (metric == Metric.UNDEFINED || metric == Metric.HEIGHT) continue;
            assertNotEquals(0, Metric.getGraph(metric));
            assertNotEquals(0, Metric.getRes(metric));
            assertTrue(Metric.isSameMetric(metric, Metric.getGraph(metric)));
        }
    }

    @Test
    public void representativeMappingsRemainStable() {
        assertEquals(R.id.graph_weight, Metric.getGraph(Metric.WEIGHT));
        assertEquals(R.id.graph_percentFat, Metric.getGraph(Metric.PERCENTFAT));
        assertEquals(R.id.graph_leftLegMuscleMass, Metric.getGraph(Metric.LEFTLEGMUSCLEMASS));
        assertEquals(R.drawable.ic_weight, Metric.getRes(Metric.WEIGHT));
        assertEquals(R.drawable.ic_left_leg, Metric.getRes(Metric.LEFTLEGMUSCLEMASS));
    }
}
