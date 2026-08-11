package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ForegroundUploadStateTest {
    @Test
    public void progressIsImmutableBoundedAndReplayable() {
        ForegroundUploadState initial = ForegroundUploadState.running(2);
        ForegroundUploadState first = initial.operationCompleted();
        ForegroundUploadState second = first.operationCompleted();
        ForegroundUploadState bounded = second.operationCompleted();

        assertTrue(initial.running);
        assertEquals(0, initial.completed);
        assertEquals(1, first.completed);
        assertEquals(2, second.completed);
        assertEquals(2, bounded.completed);
    }

    @Test
    public void idleStateDoesNotAdvance() {
        ForegroundUploadState idle = ForegroundUploadState.idle();

        assertFalse(idle.running);
        assertSame(idle, idle.operationCompleted());
    }
}
