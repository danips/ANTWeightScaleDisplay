package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OperationEventTest {
    @Test
    public void resultCanBeReplayedButConsumedOnlyOnce() {
        OperationEvent<String> event = new OperationEvent<>("completed");

        assertEquals("completed", event.peek());
        assertEquals("completed", event.peek());
        assertEquals("completed", event.consume());
        assertNull(event.consume());
    }
}
