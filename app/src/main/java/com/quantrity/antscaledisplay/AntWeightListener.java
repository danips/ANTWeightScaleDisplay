package com.quantrity.antscaledisplay;

/** UI boundary for an ANT weight session. */
interface AntWeightListener {
    void onAntProgress(AntWeightSession.Progress progress);
    void onAntSuccess(Weight weight, User user);
    void onAntFailure(AntWeightSession.Failure failure, String detail);
}
