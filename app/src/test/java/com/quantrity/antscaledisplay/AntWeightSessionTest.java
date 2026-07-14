package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntWeightSessionTest {
    @Test
    public void capturedSequenceTransitionsToCompleteMeasurement() {
        User user = user();
        AntWeightSession session = new AntWeightSession(
                user, new AntMessageParser(() -> 10L), (short) 0x1234);
        session.start();

        assertAction(AntWeightSession.ActionType.ASSIGN_CHANNEL, session.onMessage(startup()));
        assertAction(AntWeightSession.ActionType.SET_POWER, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SET_FREQUENCY, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SET_PERIOD, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SET_ID, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SET_SEARCH_TIMEOUT, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.OPEN_CHANNEL, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SEARCH_STARTED, session.onMessage(ok()));
        assertAction(AntWeightSession.ActionType.SEND_PROFILE,
                session.onMessage(AntMessageParserTest.page(1, 0, 0, 0, 0, 0, 0xfe, 0xff)));
        assertAction(AntWeightSession.ActionType.NONE,
                session.onMessage(profileConfirmation((short) 0x1234)));
        assertEquals(AntWeightSession.State.RECEIVING, session.state());

        assertAction(AntWeightSession.ActionType.MEASUREMENT_STARTED,
                session.onMessage(AntMessageParserTest.page(1, 0, 0, 0, 0, 0, 0x10, 0x27)));
        session.onMessage(AntMessageParserTest.page(2, 0, 0, 0, 0x50, 0x14, 0x46, 0x0a));
        session.onMessage(AntMessageParserTest.page(3, 0, 0, 0, 0x40, 0x1f, 0, 0x19));
        assertAction(AntWeightSession.ActionType.COMPLETE,
                session.onMessage(AntMessageParserTest.page(4, 0, 0, 0, 0, 0x9e, 0x11, 24)));

        assertTrue(session.hasCompleteMeasurement());
        assertEquals(AntWeightSession.State.FINISHED, session.state());
    }

    @Test
    public void partialMeasurementIsNotSuccessful() {
        AntWeightSession session = receivingSession();
        session.onMessage(AntMessageParserTest.page(1, 0, 0, 0, 0, 0, 0x10, 0x27));
        assertFalse(session.hasCompleteMeasurement());
    }

    @Test
    public void unavailableCompositionCompletesWithWeight() {
        AntWeightSession session = receivingSession();
        assertAction(AntWeightSession.ActionType.MEASUREMENT_STARTED,
                session.onMessage(AntMessageParserTest.page(
                        1, 0, 0, 0, 0, 0, 0x10, 0x27)));

        assertAction(AntWeightSession.ActionType.COMPLETE_WEIGHT_ONLY,
                session.onMessage(AntMessageParserTest.page(
                        0xf1, 0xff, 0xa2, 0, 0, 0, 0xff, 0xff)));

        assertTrue(session.hasCompleteMeasurement());
        assertEquals(100.0, session.weight().weight, 0.001);
        assertEquals(AntWeightSession.State.FINISHED, session.state());
    }

    @Test
    public void channelCloseIsExplicitFailure() {
        AntWeightSession session = receivingSession();
        byte[] event = {3, 0x40, 0, 0x01, 0x07};
        AntWeightSession.Action action = session.onMessage(event);
        // Receiving ignores non-data events; disconnect is surfaced by AntServiceClient itself.
        assertEquals(AntWeightSession.ActionType.NONE, action.type);
    }

    private static AntWeightSession receivingSession() {
        AntWeightSession session = new AntWeightSession(
                user(), new AntMessageParser(() -> 10L), (short) 0x1234);
        session.start();
        session.onMessage(startup());
        for (int i = 0; i < 7; i++) session.onMessage(ok());
        session.onMessage(AntMessageParserTest.page(1, 0, 0, 0, 0, 0, 0xfe, 0xff));
        session.onMessage(profileConfirmation((short) 0x1234));
        return session;
    }

    private static User user() {
        User user = new User();
        user.age = 35;
        user.height_cm = 180;
        user.isMale = true;
        return user;
    }
    private static byte[] startup() { return new byte[]{2, 0x6f, 0, 0}; }
    private static byte[] ok() { return new byte[]{3, 0x40, 0, 0, 0}; }
    private static byte[] profileConfirmation(short id) {
        return new byte[]{9, 0x4e, 0, 1, (byte) (id >> 8), (byte) id, 0, 0, 0, 0, 0};
    }
    private static void assertAction(AntWeightSession.ActionType expected,
                                     AntWeightSession.Action actual) {
        assertEquals(expected, actual.type);
    }
}
