package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntWeightControllerCompletionTest {
    @Test
    public void completionCanOnlyBeClaimedOnce() {
        AntWeightController.CompletionDelivery delivery =
                new AntWeightController.CompletionDelivery();

        assertFalse(delivery.claim());

        delivery.markReady();

        assertTrue(delivery.claim());
        assertFalse(delivery.claim());
    }
}
