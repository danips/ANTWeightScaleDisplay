package com.quantrity.antscaledisplay;

import java.util.Random;

/** Pure ANT weight protocol state machine. */
final class AntWeightSession {
    enum State {
        IDLE, STARTING, ASSIGNING_CHANNEL, SETTING_POWER, SETTING_FREQUENCY,
        SETTING_PERIOD, SETTING_ID, SETTING_SEARCH_TIMEOUT, OPENING_CHANNEL,
        SEARCHING, WAITING_PROFILE_CONFIRMATION, RECEIVING, FINISHED
    }

    enum Progress { SEARCHING, FOUND, WAITING }
    enum Failure {
        BIND, PERMISSION, CONFIGURATION, SEARCH_TIMEOUT, WEIGHT_TIMEOUT,
        MEASUREMENT_TIMEOUT, SCALE_NOT_READY, NOT_BAREFOOT, DISCONNECTED,
        PROTOCOL, CANCELLED
    }

    enum ActionType {
        NONE, ASSIGN_CHANNEL, SET_POWER, SET_FREQUENCY, SET_PERIOD, SET_ID,
        SET_SEARCH_TIMEOUT, OPEN_CHANNEL, SEND_PROFILE, SEARCH_STARTED,
        MEASUREMENT_STARTED, COMPLETE, FAIL
    }

    static final class Action {
        final ActionType type;
        final Failure failure;
        final String detail;

        private Action(ActionType type, Failure failure, String detail) {
            this.type = type;
            this.failure = failure;
            this.detail = detail;
        }

        static Action of(ActionType type) { return new Action(type, null, null); }
        static Action fail(Failure failure, String detail) {
            return new Action(ActionType.FAIL, failure, detail);
        }
    }

    private final User user;
    private final Weight weight = new Weight();
    private final AntMessageParser parser;
    private final short profileId;
    private State state = State.IDLE;

    AntWeightSession(User user) {
        this(user, new AntMessageParser(), (short) new Random().nextInt(Short.MAX_VALUE + 1));
    }

    AntWeightSession(User user, AntMessageParser parser, short profileId) {
        this.user = user;
        this.parser = parser;
        this.profileId = profileId;
    }

    void start() { state = State.STARTING; }

    Action onMessage(byte[] message) {
        if (!AntMessageParser.isValid(message)) {
            return Action.fail(Failure.PROTOCOL, AntServiceClient.messageToString(message));
        }
        switch (state) {
            case STARTING:
                state = State.ASSIGNING_CHANNEL;
                return Action.of(ActionType.ASSIGN_CHANNEL);
            case ASSIGNING_CHANNEL:
                return acknowledge(message, State.SETTING_POWER, ActionType.SET_POWER);
            case SETTING_POWER:
                state = State.SETTING_FREQUENCY;
                return Action.of(ActionType.SET_FREQUENCY);
            case SETTING_FREQUENCY:
                return acknowledge(message, State.SETTING_PERIOD, ActionType.SET_PERIOD);
            case SETTING_PERIOD:
                return acknowledge(message, State.SETTING_ID, ActionType.SET_ID);
            case SETTING_ID:
                return acknowledge(message, State.SETTING_SEARCH_TIMEOUT,
                        ActionType.SET_SEARCH_TIMEOUT);
            case SETTING_SEARCH_TIMEOUT:
                return acknowledge(message, State.OPENING_CHANNEL, ActionType.OPEN_CHANNEL);
            case OPENING_CHANNEL:
                Action open = acknowledge(message, State.SEARCHING, ActionType.SEARCH_STARTED);
                return open;
            case SEARCHING:
                return searching(message);
            case WAITING_PROFILE_CONFIRMATION:
                return waitingForProfile(message);
            case RECEIVING:
                return receiving(message);
            default:
                return Action.of(ActionType.NONE);
        }
    }

    private Action acknowledge(byte[] message, State next, ActionType action) {
        if (message.length != 5 || message[4] != 0) {
            state = State.FINISHED;
            return Action.fail(Failure.CONFIGURATION, AntServiceClient.messageToString(message));
        }
        state = next;
        return Action.of(action);
    }

    private Action searching(byte[] message) {
        if (message.length > 3 && message[1] == (byte) 0x4e) {
            int page = message[3] & 0xff;
            if (page == 0x50 || page == 0x51) return Action.of(ActionType.NONE);
            state = State.WAITING_PROFILE_CONFIRMATION;
            return Action.of(ActionType.SEND_PROFILE);
        }
        Failure eventFailure = eventFailure(message);
        if (eventFailure != null) {
            state = State.FINISHED;
            return Action.fail(eventFailure, AntServiceClient.messageToString(message));
        }
        return Action.of(ActionType.NONE);
    }

    private Action waitingForProfile(byte[] message) {
        if (isEvent(message, 0x03)) return Action.of(ActionType.SEND_PROFILE);
        Failure eventFailure = eventFailure(message);
        if (eventFailure != null) {
            state = State.FINISHED;
            return Action.fail(eventFailure, AntServiceClient.messageToString(message));
        }
        if (message.length > 5 && message[1] == (byte) 0x4e
                && message[4] == (byte) ((profileId >> 8) & 0xff)
                && message[5] == (byte) (profileId & 0xff)) {
            state = State.RECEIVING;
        }
        return Action.of(ActionType.NONE);
    }

    private Action receiving(byte[] message) {
        AntMessageParser.Outcome outcome = parser.apply(message, weight);
        if (outcome == AntMessageParser.Outcome.SCALE_NOT_READY) {
            state = State.FINISHED;
            return Action.fail(Failure.SCALE_NOT_READY, null);
        }
        if (outcome == AntMessageParser.Outcome.NOT_BAREFOOT) {
            state = State.FINISHED;
            return Action.fail(Failure.NOT_BAREFOOT, null);
        }
        if (outcome == AntMessageParser.Outcome.FIRST_WEIGHT) {
            return Action.of(ActionType.MEASUREMENT_STARTED);
        }
        if (outcome == AntMessageParser.Outcome.COMPLETE) {
            state = State.FINISHED;
            return Action.of(ActionType.COMPLETE);
        }
        return Action.of(ActionType.NONE);
    }

    private static Failure eventFailure(byte[] message) {
        if (isEvent(message, 0x01) || isEvent(message, 0x07)) return Failure.DISCONNECTED;
        return null;
    }

    private static boolean isEvent(byte[] message, int event) {
        return message.length > 4 && message[1] == (byte) 0x40
                && message[3] == (byte) 0x01 && (message[4] & 0xff) == event;
    }

    byte[] profilePage() {
        byte[] page = {(byte) 0x09, (byte) 0x4e, (byte) 0x00, (byte) 0x3a,
                (byte) 0xff, (byte) 0xff, (byte) 0x03, (byte) 0xff,
                (byte) 0x00, (byte) 0x00, (byte) 0x00};
        page[4] = (byte) ((profileId >> 8) & 0xff);
        page[5] = (byte) (profileId & 0xff);
        page[8] = (byte) ((user.isMale ? 0x80 : 0x00) | (byte) user.age);
        page[9] = (byte) user.height_cm;
        page[10] = (byte) ((user.isLifetimeAthlete ? 0x80 : 0x00)
                | (byte) user.activity_level);
        return page;
    }

    Weight weight() { return weight; }
    State state() { return state; }
    boolean hasCompleteMeasurement() { return parser.isComplete() && weight.weight != -1; }
    void finish() { state = State.FINISHED; }
}
