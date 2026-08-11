package com.quantrity.antscaledisplay;

/** Keeps retained ANT measurement state bound to the profile that produced it. */
final class AntWeightSelectionPolicy {
    private AntWeightSelectionPolicy() {}

    static boolean matches(User measurementUser, User selectedUser) {
        return measurementUser != null
                && selectedUser != null
                && measurementUser.uuid != null
                && measurementUser.uuid.equals(selectedUser.uuid);
    }

    static boolean shouldDiscard(boolean running, boolean completionDelivered,
                                 User measurementUser, User selectedUser) {
        return !running
                && completionDelivered
                && !matches(measurementUser, selectedUser);
    }
}
