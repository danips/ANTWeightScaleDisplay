package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

import com.dsi.ant.IAnt_6;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class RequestWeight {
    private static final String TAG = "RequestWeight";

    private static final byte channelNumber = (byte) 0x00;
    private static final byte channelType = (byte) 0x00;
    private static final byte networkNumber = (byte) 0x01;
    private static final byte radioFrequency = (byte) 0x39;
    private static final int channelPeriod = 8192;
    private static final int deviceNumber = 0x0000;
    private static final byte deviceType = (byte) 0x77;
    private static final byte txType = (byte) 0x00;

    private ScheduledExecutorService worker = null;
    private ScheduledFuture<?> scheduledFuture = null;
    private static final byte configTimeout = 5;
    private static final byte searchTimeout = 10;//2.5*5 = 12.5s
    private static final byte weightTimeout = 25;
    private static final byte measurementsTimeout = 60;


    private enum stateEnum {
        NONE,
        STARTING_UP,
        CONFIGURING_ASSIGN_CHANNEL,
        CONFIGURING_SET_CHANNEL_TRANSMIT_POWER,
        CONFIGURING_CHANNEL_RF_FREQUENCY,
        CONFIGURING_CHANNEL_PERIOD,
        CONFIGURING_CHANNEL_ID,
        CONFIGURING_SEARCH_TIMEOUT,
        CONFIGURING_OPEN_CHANNEL,
        SEARCHING,
        WAITING_PROFILE_CONFIRMATION,
        RECEIVING,
        END
    }

    stateEnum state = stateEnum.NONE;

    /** Inter-process communication with the ANT Radio Proxy Service. */
    private static IAnt_6 sAntReceiver = null;

    /** The context to use. */
    private final Context sContext;
    private final WeightFragment sFragment;

    static private ProgressDialog sProgressDialog = null;

    /** Is the ANT Radio Proxy Service connected. */
    private static boolean sServiceConnected = false;

    /** Receives and logs all status ANT intents. */
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Debug.ON) {
                String antAction = intent.getAction();
                Log.i(TAG, "enter status onReceive" + antAction);
            }
        }
    };

    Weight the_weight = null;

    private boolean got1 = false, got2 = false, got3 = false, got4 = false, got5 = false, shown = false;
    private boolean hasSegmental = false, got_c5 = false, got_bc = false, got_c8 = false;
    private boolean got_b9 = false, got_b0 = false;

    RequestWeight(WeightFragment fragment) {
        sFragment = fragment;
        sContext = fragment.getActivity();
        if (sContext != null)
            sProgressDialog = ProgressDialog.show(sContext, null, sContext.getResources().getString(R.string.weight_fragment_msg_searching), true);
    }

    /** Receives all data ANT intents. */
    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String antAction = intent.getAction();

            if ((antAction != null) && antAction.equals("com.dsi.ant.intent.action.ANT_RX_MESSAGE_ACTION")) {
                byte[] antMessage = intent.getByteArrayExtra("com.dsi.ant.intent.ANT_MESSAGE");
                int len = antMessage[0];
                if (len != antMessage.length - 2 || antMessage.length <= 2) {
                    if (Debug.ON) Log.e(TAG, "Invalid message: " + messageToString(antMessage));
                    return;
                }

                if (Debug.ON) Log.v(TAG, state.toString() + " " + messageToString(antMessage));
                switch (state) {
                    case NONE:
                    case END:
                        break;
                    case STARTING_UP:
                        if (Debug.ON) Log.d(TAG, String.format("Received startup message (reason %02x); initializing channel", antMessage[2]));

                        try {
                            if (!sAntReceiver.ANTAssignChannel(channelNumber, channelType, networkNumber)) {
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTAssignChannel: false"));
                                return;
                            }
                            if (!sAntReceiver.ANTDisableEventBuffering()) {
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTDisableEventBuffering: false"));
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTAssignChannel: RemoteException"));
                            return;
                        }
                        state = stateEnum.CONFIGURING_ASSIGN_CHANNEL;
                        if (Debug.ON) Log.v(TAG, "State = CONFIGURING_ASSIGN_CHANNEL");
                        break;

                    case CONFIGURING_ASSIGN_CHANNEL:
                        if ((antMessage.length != 5) || (antMessage[4] != 0x00)) {
                            if (Debug.ON) Log.e(TAG, "CONFIGURING_ASSIGN_CHANNEL " + messageToString(antMessage));
                            //TODO notify problem
                            return;
                        }

                        try {
                            if (!sAntReceiver.ANTSetChannelTxPower(channelNumber, (byte) 4)) {//AntDefine.RADIO_TX_POWER_LVL_3) == false) {
                                Log.v(TAG, "false in CONFIGURING_SET_CHANNEL_TRANSMIT_POWER");
                                //TODO notify problem
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.v(TAG, "RemoteException in CONFIGURING_SET_CHANNEL_TRANSMIT_POWER");
                            //TODO notify problem
                            return;
                        }
                        state = stateEnum.CONFIGURING_SET_CHANNEL_TRANSMIT_POWER;
                        break;

                    case CONFIGURING_SET_CHANNEL_TRANSMIT_POWER:
                        /*if ((antMessage.length < 5) && (antMessage[4] != 0x00)) {
                            Log.e(TAG, "CONFIGURING_SET_CHANNEL_TRANSMIT_POWER " + String.format("Error = %02x", antMessage[4]));
                            //TODO notify problem
                            return;
                        }*/

                        //12-17 19:09:21.153: I/ANTSerial(21655): built-in:Built-in:1 - Tx command - [02][45][00][39]
                        //12-17 19:09:21.153: I/ANTSerial(21655): built-in:Built-in:1 - Rx         - [03][40][00][45][00]
                        try {
                            if (!sAntReceiver.ANTSetChannelRFFreq(channelNumber, radioFrequency)) {
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTSetChannelRFFreq: false"));
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTSetChannelRFFreq: RemoteException"));
                            return;
                        }
                        //Log.v(TAG, test + " sent ANTAssignChannel");
                        state = stateEnum.CONFIGURING_CHANNEL_RF_FREQUENCY;
                        break;

                    case CONFIGURING_CHANNEL_RF_FREQUENCY:
                        if ((antMessage.length != 5) || (antMessage[4] != 0x00)) {
                            if (Debug.ON) Log.e(TAG, "CONFIGURING_CHANNEL_RF_FREQUENCY " + messageToString(antMessage));
                            //TODO notify problem
                            return;
                        }

                        //12-17 19:09:21.163: I/ANTSerial(21655): built-in:Built-in:1 - Tx command - [03][43][00][00][20]
                        //12-17 19:09:21.163: I/ANTSerial(21655): built-in:Built-in:1 - Rx         - [03][40][00][43][00]
                        //byte msg10[] = {(byte) 0x03, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x20};
                        //test = sAntReceiver.ANTTxMessage(msg10);
                        try {
                            if (!sAntReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod)) {
                                Log.v(TAG, "false in CONFIGURING_CHANNEL_PERIOD");
                                //TODO notify problem
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.v(TAG, "RemoteException in CONFIGURING_CHANNEL_PERIOD");
                            //TODO notify problem
                            return;
                        }
                        state = stateEnum.CONFIGURING_CHANNEL_PERIOD;
                        break;

                    case CONFIGURING_CHANNEL_PERIOD:
                        if (antMessage[4] != 0x00) {
                            Log.e(TAG, "CONFIGURING_CHANNEL_PERIOD " + String.format("Error = %02x", antMessage[4]));
                            //TODO notify problem
                            return;
                        }

                        //12-17 19:09:21.203: I/ANTSerial(21655): built-in:Built-in:1 - Tx command - [05][51][00][00][00][77][00]
                        //12-17 19:09:21.213: I/ANTSerial(21655): built-in:Built-in:1 - Rx         - [03][40][00][51][00]
                        try {
                            if (!sAntReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType, txType)) {
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTSetChannelId: false"));
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTSetChannelId: RemoteException"));
                            return;
                        }
                        state = stateEnum.CONFIGURING_CHANNEL_ID;
                        break;

                    case CONFIGURING_CHANNEL_ID:
                        if ((antMessage.length != 5) || antMessage[4] != 0x00) {
                            if (Debug.ON) Log.e(TAG, "CONFIGURING_CHANNEL_ID " + messageToString(antMessage));
                            //TODO notify problem
                            return;
                        }

                        //12-17 19:09:21.233: I/ANTSerial(21655): built-in:Built-in:1 - Tx command - [02][44][00][00]
                        //12-17 19:09:21.233: I/ANTSerial(21655): built-in:Built-in:1 - Rx         - [03][40][00][44][00]
                        try {
                            if (!sAntReceiver.ANTSetChannelSearchTimeout(channelNumber, searchTimeout)) {
                                Log.v(TAG, "false in CONFIGURING_SEARCH_TIMEOUT");
                                //TODO notify problem
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Log.v(TAG, "RemoteException in CONFIGURING_SEARCH_TIMEOUT");
                            //TODO notify problem
                            return;
                        }
                        state = stateEnum.CONFIGURING_SEARCH_TIMEOUT;
                        //Log.v(TAG, test + " sent ANTSetChannelSearchTimeout");
                        break;

                    case CONFIGURING_SEARCH_TIMEOUT:
                        if (antMessage[4] != 0x00) {
                            Log.e(TAG, "CONFIGURING_SEARCH_TIMEOUT " + String.format("Error = %02x", antMessage[4]));
                            //TODO notify problem
                            return;
                        }

                        //12-17 19:09:21.243: I/ANTSerial(21655): built-in:Built-in:1 - Tx command - [01][4B][00]
                        //12-17 19:09:21.253: I/ANTSerial(21655): built-in:Built-in:1 - Rx         - [03][40][00][4B][00]
                        byte[] msgOpenChannel = {(byte) 0x01, (byte) 0x4B, (byte) 0x00};
                        try {
                            if (!sAntReceiver.ANTTxMessage(msgOpenChannel)) {
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTOpenChannel: false"));
                                return;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTOpenChannel: RemoteException"));
                            return;
                        }
                        state = stateEnum.CONFIGURING_OPEN_CHANNEL;
                        break;

                    case CONFIGURING_OPEN_CHANNEL:
                        if ((antMessage.length != 5) || (antMessage[4] != 0x00)) {
                            if (Debug.ON) Log.e(TAG, "CONFIGURING_OPEN_CHANNEL " + messageToString(antMessage));
                            //TODO notify problem
                            return;
                        }
                        state = stateEnum.SEARCHING;

                        if (scheduledFuture != null) {
                            scheduledFuture.cancel(false);
                            scheduledFuture = null;
                        }
                        Runnable task = () -> {

                            if (Debug.ON) Log.v(TAG, "***** Runnable searchTimeout executed *****");
                            //if ((state == stateEnum.STARTING_UP) || (state == stateEnum.NONE)) {
                            if (Debug.ON) Log.v(TAG, "Search timeout, NO ANT RESPONSE");
                            releaseService(R.string.weight_process_msg_problem_timeout);
                            //}
                        };
                        scheduledFuture = worker.schedule(task, searchTimeout, TimeUnit.SECONDS);
                        break;

                    case SEARCHING:
                        if (antMessage[1] == (byte)0x4E) {//AntMesg.MESG_BROADCAST_DATA_ID) {
                            if (Debug.ON) Log.d(TAG, "Weight Scale detected "+ messageToString(antMessage) + " state="+state.toString());
                            if (scheduledFuture != null) {
                                scheduledFuture.cancel(false);
                                scheduledFuture = null;
                            }

                            if (Debug.ON) Log.v(TAG, "Received page " + antMessage[3]);
                            if (((antMessage[3] == (byte)0x01) && ((antMessage[9] != (byte)0xfe) || (antMessage[10] != (byte)0xff)))
                                    || ((antMessage[3] == (byte)0xf1) || (antMessage[3] == (byte)0x02) || (antMessage[3] == (byte)0x03) || (antMessage[3] == (byte)0x04))) {
                                //releaseService(R.string.weight_process_msg_problem_scale_not_ready);
                                if (Debug.ON) Log.v(TAG, "NOT OK (maybe smartlab) ====================================");
                                //return;
                            } else if ((antMessage[3] == (byte)0x50) || (antMessage[3] == (byte)0x51)) {
                                if (Debug.ON) Log.v(TAG, "Common pages, nok");
                                return;
                            } else if (Debug.ON) Log.v(TAG, "OK??? " + messageToString(antMessage));
                            /*else if ((antMessage[3] == (byte)0xf1) || (antMessage[3] == (byte)0x02) || (antMessage[3] == (byte)0x03) || (antMessage[3] == (byte)0x04)) {//ANT measures pages
                                Log.v(TAG, "ANT measures pages");
                                releaseService(R.string.weight_process_msg_problem_scale_not_ready);
                                return;
                            }*/

                            //TODO: Check the user ID the scale is sending: FFFF, set to something...

                            sProgressDialog.setMessage(sContext.getResources().getString(R.string.weight_fragment_msg_found));

                            /*if (antMessage[3] == (byte)0x01) {
                                byte capabilities = antMessage[6];
                                Log.v(TAG, "BYTE="+String.format("%02x",antMessage[6]));
                                if ((capabilities & 0x01) != 0)
                                    Log.v(TAG, "Scale: User Profile Selected");
                                if ((capabilities & 0x02) != 0)
                                    Log.v(TAG, "Scale: User Profile Exchange");
                                if ((capabilities & 0x04) != 0) Log.v(TAG, "Scale: ANT- FS");
                                if ((capabilities & 0x08) != 0) {
                                    if ((capabilities & 0x10) != 0) {
                                        Log.v(TAG, "11 Scale: User Specific Data Transmission: Reserved");
                                    } else {
                                        Log.v(TAG, "01 Scale: User Specific Data Transmission: Sends user specific data");
                                    }
                                } else {
                                    if ((capabilities & 0x10) != 0) {
                                        Log.v(TAG, "10 Scale: User Specific Data Transmission: Never sends user specific data");
                                    } else {
                                        Log.v(TAG, "00 Scale: User Specific Data Transmission: Unknown");
                                    }
                                }
                                if ((capabilities & 0x08) != 0) Log.v(TAG, "Display: User Profile  Storage");
                            }
                            //if (true) return;*/

                            //Send profile
                            byte[] msg = genUserProfileDataPage();
                            try {
                                if (!sAntReceiver.ANTTxMessage(msg)) {
                                    releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: false"));
                                    return;
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: RemoteException"));
                                return;
                            }
                            if (Debug.ON) Log.v(TAG, "--------> sent=" + messageToString(msg));
                            state = stateEnum.WAITING_PROFILE_CONFIRMATION;
                            Runnable task2 = () -> {
                                if (Debug.ON) Log.v(TAG, "***** scheduledFuture weightTimeout executed *****");
                                //if ((state == stateEnum.RECEIVING) || (state == stateEnum.WAITING_PROFILE_CONFIRMATION)) {
                                    if (Debug.ON) Log.v(TAG, "Weight timeout");
                                    if (the_weight.weight != -1) releaseService(null);
                                    releaseService(R.string.weight_process_msg_problem_timeout_weight);
                                //}
                            };
                            scheduledFuture = worker.schedule(task2, weightTimeout, TimeUnit.SECONDS);
                        } else {
                            if (antMessage[1] == (byte)0x40) {//AntMesg.MESG_RESPONSE_EVENT_ID) {
                                if (antMessage.length > 3 && antMessage[3] == (byte)0x01) {//AntMesg.MESG_EVENT_ID) {
                                    if (antMessage.length > 4) {
                                        if (antMessage[4] == (byte) 0x02) {//AntDefine.EVENT_RX_FAIL) {
                                            if (Debug.ON)
                                                Log.v(TAG, "EVENT_RX_FAIL, missed message. Ignoring...");
                                            return;
                                        } else if (antMessage[4] == (byte) 0x08) {//== AntDefine.EVENT_RX_FAIL_GO_TO_SEARCH) {
                                            if (Debug.ON) Log.v(TAG, "EVENT_RX_FAIL_GO_TO_SEARCH 1");
                                            //releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                            return;
                                        } else if (antMessage[4] == (byte) 0x01) {//== AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
                                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                            return;
                                        } else if (antMessage[4] == (byte) 0x07) {//== AntDefine.EVENT_CHANNEL_CLOSED) {
                                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                            return;
                                        }
                                    }
                                }
                            }
                            if (Debug.ON) Log.v(TAG, "Unknown problem XX " + messageToString(antMessage));
                        }
                        break;

                    case WAITING_PROFILE_CONFIRMATION:
                        //Log.v(TAG, "**** WAITING_PROFILE_CONFIRMATION " + messageToString(antMessage));

                        if (antMessage[1] == (byte)0x40) {//AntMesg.MESG_RESPONSE_EVENT_ID) {
                            if (antMessage[3] == (byte)0x01) {//AntMesg.MESG_EVENT_ID) {
                                if (antMessage[4] == (byte)0x03) {//AntDefine.EVENT_TX) {
                                    if (Debug.ON) Log.v(TAG, "User profile transmitted successfully");

                                    sProgressDialog.setMessage(sContext.getResources().getString(R.string.weight_fragment_msg_waiting));
                                    byte[] msg2 = genUserProfileDataPage();//(short) 0x2200);//, (boolean) true, (byte) 32, (byte) 179, (boolean) true, (byte) 6);
                                    try {
                                        if (!sAntReceiver.ANTTxMessage(msg2)) {
                                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: false"));
                                            return;
                                        }
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                        releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: RemoteException"));
                                        return;
                                    }
                                    if (Debug.ON) Log.v(TAG, "--------> sent=" + messageToString(msg2));
                                    return;
                                } else if (antMessage[4] == (byte)0x02) {//AntDefine.EVENT_RX_FAIL) {
                                    if (Debug.ON) Log.v(TAG, "EVENT_RX_FAIL, missed message");
                                    return;
                                } else if (antMessage[4] == (byte)0x08) {//AntDefine.EVENT_RX_FAIL_GO_TO_SEARCH) {
                                    if (Debug.ON) Log.v(TAG, "EVENT_RX_FAIL_GO_TO_SEARCH 2");
                                    //releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                } else if (antMessage[4] == (byte)0x01) {//AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
                                    releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                } else if (antMessage[4] == (byte)0x07) {//AntDefine.EVENT_CHANNEL_CLOSED) {
                                    releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                }
                            }
                            if (Debug.ON) Log.v(TAG, "MESG_RESPONSE_EVENT_ID " + messageToString(antMessage) +  " state=" + state.toString());
                        } else if (antMessage[1] == (byte)0x4e) {
                            if (Debug.ON) Log.v(TAG, antMessage[4] + " " + antMessage[5] + " ?= " + (byte) ((userProfileIdentification >> 8) & 0xff) + " " + (byte) (userProfileIdentification & 0xff));
                            if ((antMessage[4] == (byte) ((userProfileIdentification >> 8) & 0xff)) && (antMessage[5] == (byte) (userProfileIdentification & 0xff))) {
                                state = stateEnum.RECEIVING;
                            }
                        }
                        break;

                    case RECEIVING:
                        if (antMessage[1] == (byte)0x4E) {//AntMesg.MESG_BROADCAST_DATA_ID) {
                            //Log.d(TAG, "Received RX message " + messageToString(antMessage) + " state="+state.toString());
                            if (antMessage[3] == (byte)0x01) {//Page 1, weight
                                if ((antMessage[9] != (byte)0xfe) || (antMessage[10] != (byte)0xff)) {
                                    if ((antMessage[9] == (byte)0xff) && (antMessage[10] == (byte)0xff)) {
                                        releaseService(R.string.weight_process_msg_problem_scale_not_ready);
                                        return;
                                    }

                                    the_weight.weight = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 100);
                                    the_weight.date = System.currentTimeMillis();
                                    if (Debug.ON) Log.v(TAG, the_weight.date + " weight=" + the_weight.weight + " from " + messageToString(antMessage));

                                    //Poner un timeout, si no llegan datos de composición es que no está descalzo
                                    if (!got1) {
                                        if (scheduledFuture != null) {
                                            scheduledFuture.cancel(false);
                                            scheduledFuture = null;
                                        }
                                        Runnable task3 = () -> {
                                            if (Debug.ON) Log.v(TAG, "***** scheduledFuture measurementsTimeout executed *****");
                                            //if (state == stateEnum.RECEIVING) {
                                                if (Debug.ON) Log.v(TAG, "Measurements timeout");
                                                if (the_weight.weight != -1)
                                                    releaseService(null);
                                                else releaseService(R.string.weight_process_msg_problem_timeout_measurements);
                                            //}
                                        };
                                        scheduledFuture = worker.schedule(task3, measurementsTimeout, TimeUnit.SECONDS);
                                    }
                                    got1 = true;
                                } /*else {
                                    //Log.v(TAG, "Page1 INVALID " + messageToString(antMessage));
                                }*/

                                /*if (!got1) {
                                    if ((antMessage[9] == (byte)0xfe) && (antMessage[10] == (byte)0xff)) {
                                        //Log.v(TAG, "Page1 INVALID " + messageToString(antMessage));
                                    } else {
                                        got1 = true;
                                        the_weight.weight = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 100);
                                        the_weight.date = Calendar.getInstance().getTime().getTime();//new Date().;
                                        if (Debug.ON) Log.v(TAG, the_weight.date + " weight=" + the_weight.weight + " from " + messageToString(antMessage));

                                        //Poner un timeout, si no llegan datos de composición es que no está descalzo
                                        Runnable task3 = new Runnable() {
                                            public void run() {
                                                if (state == stateEnum.RECEIVING) {
                                                    if (Debug.ON) Log.v(TAG, "Measurements timeout");
                                                    if (the_weight.weight != -1) releaseService(null);
                                                    else releaseService(R.string.weight_process_msg_problem_timeout_weight);
                                                }
                                            }
                                        };
                                        worker.schedule(task3, measurementsTimeout, TimeUnit.SECONDS);
                                    }
                                }*/
                            }  else if ((antMessage[3] == (byte)0x50) || (antMessage[3] == (byte)0x51)) {
                                if (Debug.ON) Log.v(TAG, "Ignoring common pages");
                            } else if (antMessage[3] == (byte)0xf1) {//Tanita BC-1000 & BC-1500 Special pages
                                if (antMessage[4] != (byte)0xff) {
                                    if (antMessage[5] == (byte) 0xa2) {
                                        if (!got2) {
                                            got2 = true;
                                            the_weight.percentFat = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 100 );
                                            the_weight.percentHydration = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 100);
                                            if (Debug.ON) Log.v(TAG, "percentFat=" + the_weight.percentFat + " percentHydration=" + the_weight.percentHydration + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xa3) {
                                        if (!got3) {
                                            got3 = true;
                                            the_weight.muscleMass = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 1000 + (((antMessage[4] & 0xff) >> 4) * 65.535));
                                            the_weight.physiqueRating = (short) (antMessage[9] + 256 * antMessage[10]);
                                            if (Debug.ON) Log.v(TAG, "muscleMass=" + the_weight.muscleMass + " physiqueRating=" + the_weight.physiqueRating + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xa9) {
                                        if (!got4) {
                                            got4 = true;
                                            the_weight.boneMass = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 1000);
                                            the_weight.visceralFatRating = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 1000);
                                            if (Debug.ON) Log.v(TAG, "boneMass=" + the_weight.boneMass + " visceralFatRating=" + the_weight.visceralFatRating + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xd4) {
                                        if (!got5) {
                                            got5 = true;
                                            the_weight.basalMet = (float) ((double) ((antMessage[6] & 0xFF) + 256 * (antMessage[7] & 0xFF)) / 100 + (((antMessage[4] & 0xff) >> 4) * 655.35));
                                            the_weight.metabolicAge = (short) (antMessage[9] + 256 * antMessage[10]);
                                            if (Debug.ON)
                                                Log.v(TAG, "basalMet=" + the_weight.basalMet + " metabolicAge=" + the_weight.metabolicAge + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xc5) {
                                        if (!got_c5) {
                                            hasSegmental = true;
                                            got_c5 = true;
                                            the_weight.leftArmMuscleMass = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 1000);
                                            the_weight.rightLegMuscleMass = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 1000);
                                            if (Debug.ON)
                                                Log.v(TAG, "leftArmMuscleMass=" + the_weight.leftArmMuscleMass + " rightLegMuscleMass=" + the_weight.rightLegMuscleMass + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xbc) {
                                        if (!got_bc) {
                                            hasSegmental = true;
                                            got_bc = true;
                                            the_weight.rightArmPercentFat = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 100 );
                                            the_weight.leftArmPercentFat = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 100 );
                                            if (Debug.ON)
                                                Log.v(TAG, "rightArmPercentFat=" + the_weight.rightArmPercentFat + " leftArmPercentFat=" + the_weight.leftArmPercentFat + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xc8) {
                                        if (!got_c8) {
                                            hasSegmental = true;
                                            got_c8 = true;
                                            the_weight.trunkPercentFat = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 100 );
                                            the_weight.rightArmMuscleMass = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 1000);
                                            if (Debug.ON)
                                                Log.v(TAG, "trunkPercentFat=" + the_weight.trunkPercentFat + " rightArmMuscleMass=" + the_weight.rightArmMuscleMass + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xb9) {
                                        if (!got_b9) {
                                            hasSegmental = true;
                                            got_b9 = true;
                                            the_weight.leftLegMuscleMass = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 1000);
                                            the_weight.trunkMuscleMass = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 1000);
                                            if (Debug.ON)
                                                Log.v(TAG, "leftLegMuscleMass=" + the_weight.leftLegMuscleMass + " trunkMuscleMass=" + the_weight.trunkMuscleMass + " from " + messageToString(antMessage));
                                        }
                                    } else if (antMessage[5] == (byte) 0xb0) {
                                        if (!got_b0) {
                                            hasSegmental = true;
                                            got_b0 = true;
                                            the_weight.rightLegPercentFat = (float)((double)((antMessage[6]& 0xFF) + 256 * (antMessage[7]& 0xFF)) / 100 );
                                            the_weight.leftLegPercentFat = (float)((double)((antMessage[9]& 0xFF) + 256 * (antMessage[10]& 0xFF)) / 100 );
                                            if (Debug.ON)
                                                Log.v(TAG, "rightLegPercentFat=" + the_weight.rightLegPercentFat + " leftLegPercentFat=" + the_weight.leftLegPercentFat + " from " + messageToString(antMessage));
                                        }
                                    } else
                                    if (Debug.ON) Log.w(TAG, "*** Received4 messageId=" + antMessage[1] + " messageData" + messageToString(antMessage));
                                } else {
                                    if ((antMessage[5] == (byte)0xc5) || (antMessage[5] == (byte)0xbc)
                                            || (antMessage[5] == (byte)0xc8) || (antMessage[5] == (byte)0xb9)
                                            || (antMessage[5] == (byte)0xb0))
                                    {
                                        hasSegmental = true;
                                    }
                                    Log.v(TAG,"antMessage[4]=" + String.format("0x%02x", antMessage[4]) + " hasSegmental="+hasSegmental);
                                    if ((antMessage[9] == (byte)0xff) && (antMessage[10] == (byte)0xff)) {//Not barefoot, no composition analysis
                                        if (the_weight.weight == -1) {
                                            if (Debug.ON) Log.v(TAG, "Not barefoot, no composition analysis but no weight, waiting for weight...");
                                        } else releaseService(R.string.weight_process_msg_problem_not_barefoot);
                                        return;
                                    } else if (Debug.ON) Log.w(TAG, "*** Received3.5 INVALID DATA messageData, computing");// + messageToString(antMessage));
                                }
                            } else if (antMessage[3] == (byte)0x02) {//Page 2, Body Composition Percentage
                                if (!got2) {
                                    if ((antMessage[9] != (byte) 0xfe) || (antMessage[10] != (byte) 0xff)) {
                                        got2 = true;
                                        if ((antMessage[7] != (byte) 0xff) && (antMessage[8] != (byte) 0xff)) the_weight.percentHydration = (float) ((double) ((antMessage[7] & 0xFF) + 256 * (antMessage[8] & 0xFF)) / 100);
                                        if ((antMessage[9] != (byte) 0xff) && (antMessage[10] != (byte) 0xff)) the_weight.percentFat = (float) ((double) ((antMessage[9] & 0xFF) + 256 * (antMessage[10] & 0xFF)) / 100);
                                        if (Debug.ON) Log.v(TAG, "percentFat=" + the_weight.percentFat + " percentHydration=" + the_weight.percentHydration + " from " + messageToString(antMessage));
                                    }
                                }
                            } else if (antMessage[3] == (byte)0x03) {//Page 3, Metabolic Information
                                if (!got5) {
                                    if ((antMessage[9] != (byte) 0xfe) || (antMessage[10] != (byte) 0xff)) {
                                        got5 = true;
                                        if ((antMessage[7] != (byte) 0xff) && (antMessage[8] != (byte) 0xff))  the_weight.activeMet = (float) ((double) ((antMessage[7] & 0xFF) + 256 * (antMessage[8] & 0xFF)) / 4);
                                        if ((antMessage[9] != (byte) 0xff) && (antMessage[10] != (byte) 0xff)) the_weight.basalMet = (float) ((double) ((antMessage[9] & 0xFF) + 256 * (antMessage[10] & 0xFF)) / 4);
                                        if (Debug.ON) Log.v(TAG, "activeMet=" + the_weight.activeMet + " basalMet=" + the_weight.basalMet + " from " + messageToString(antMessage));
                                    }
                                }
                            } else if (antMessage[3] == (byte)0x04) {//Page 4, Body Composition Mass
                                if (!got3) {
                                    if ((antMessage[8] != (byte) 0xfe) || (antMessage[9] != (byte) 0xff)) {
                                        got3 = got4 = true;
                                        if ((antMessage[8] != (byte) 0xff) && (antMessage[9] != (byte) 0xff)) the_weight.muscleMass = (float) ((double) ((antMessage[8] & 0xFF) + 256 * (antMessage[9] & 0xFF)) / 100);
                                        if (antMessage[10] != (byte) 0xff) the_weight.boneMass = (float) ((double) (antMessage[10] & 0xFF) / 10);
                                        if (Debug.ON) Log.v(TAG, "muscleMass=" + the_weight.muscleMass + " boneMass=" + the_weight.boneMass + " from " + messageToString(antMessage));
                                    }
                                }
                            } else if (Debug.ON) Log.w(TAG, "*** Received3 messageId=" + antMessage[1] + " messageData" + messageToString(antMessage));

                            if (got1 && got2 && got3 && got4 && got5 && (!hasSegmental || (got_b0 && got_b9 && got_bc && got_c5 && got_c8)) && !shown ) {
                                shown = true;
                                if (Debug.ON) {
                                    Log.v(TAG, "===== Peso finalizado " + the_weight.date + " =====");
                                    Log.v(TAG, "weight=" + the_weight.weight);
                                    Log.v(TAG, "percentFat=" + the_weight.percentFat);
                                    Log.v(TAG, "percentHydration=" + the_weight.percentHydration);
                                    Log.v(TAG, "boneMass=" + the_weight.boneMass);
                                    Log.v(TAG, "muscleMass=" + the_weight.muscleMass);
                                    Log.v(TAG, "physiqueRating=" + the_weight.physiqueRating);
                                    Log.v(TAG, "visceralFatRating=" + the_weight.visceralFatRating);
                                    Log.v(TAG, "metabolicAge=" + the_weight.metabolicAge);
                                    Log.v(TAG, "activeMet=" + the_weight.activeMet);
                                    Log.v(TAG, "basalMet=" + the_weight.basalMet);
                                    Log.v(TAG, "trunkPercentFat=" + the_weight.trunkPercentFat);
                                    Log.v(TAG, "trunkMuscleMass=" + the_weight.trunkMuscleMass);
                                    Log.v(TAG, "leftArmPercentFat=" + the_weight.leftArmPercentFat);
                                    Log.v(TAG, "leftArmMuscleMass=" + the_weight.leftArmMuscleMass);
                                    Log.v(TAG, "rightArmPercentFat=" + the_weight.rightArmPercentFat);
                                    Log.v(TAG, "rightArmMuscleMass=" + the_weight.rightArmMuscleMass);
                                    Log.v(TAG, "leftLegPercentFat=" + the_weight.leftLegPercentFat);
                                    Log.v(TAG, "leftLegMuscleMass=" + the_weight.leftLegMuscleMass);
                                    Log.v(TAG, "rightLegPercentFat=" + the_weight.rightLegPercentFat);
                                    Log.v(TAG, "rightLegMuscleMass=" + the_weight.rightLegMuscleMass);
                                }

                                //if (the_weight.basalMet == -1) the_weight.basalMet = calcBMR(the_weight.weight, the_user.height_cm, the_user.age, the_user.isMale);
                                if (Debug.ON) Log.v(TAG, "*Calc BMR = " + calcBMR(the_weight.weight, the_user.height_cm, the_user.age, the_user.isMale) + " kcal");

                                //if (the_weight.activeMet == -1) the_weight.activeMet = calcAMR(the_weight.basalMet, the_user.activity_level);
                                if (Debug.ON) Log.v(TAG, "*Calc AMR = " + calcAMR(the_weight.basalMet, the_user.activity_level) + " kcal");

                                //if (the_weight.metabolicAge == -1) the_weight.metabolicAge = (int) calcMetAge(the_user.height_cm, the_user.age, the_user.isMale, the_weight.basalMet);
                                if (Debug.ON) Log.v(TAG, "*Calc Met AGE = " + calcMetAge(the_user.height_cm, the_user.isMale, the_weight.basalMet));

                                if (the_weight.percentFat == -1) the_weight.percentFat = calcPercentFat(the_weight.weight, the_user.height_cm, the_user.age, the_user.isMale);
                                if (Debug.ON) Log.v(TAG, "*Calc Percent Fat = " + calcPercentFat(the_weight.weight, the_user.height_cm, the_user.age, the_user.isMale));
                                if (Debug.ON) Log.v(TAG, "===========================");
                                the_weight.activityLevel = the_user.activity_level + (the_user.isLifetimeAthlete ? 0x10 : 0);

                                releaseService(null);
                            }
                        } /*else if (antMessage[1] == (byte)0x40) {//AntMesg.MESG_RESPONSE_EVENT_ID) {
                            if (antMessage[3] == (byte)0x01) {//AntMesg.MESG_EVENT_ID) {
                                if (antMessage[4] == (byte)0x03) {//AntDefine.EVENT_TX) {
                                    if (Debug.ON) Log.v(TAG, "User profile transmitted successfully");

                                    if (repeat != 0) {//Enviar dos veces la petición para asegurarse éxito
                                        repeat--;// = false;

                                        sProgressDialog.setMessage(sContext.getResources().getString(R.string.weight_fragment_msg_waiting));
                                        byte msg2[] = genUserProfileDataPage();//(short) 0x2200);//, (boolean) true, (byte) 32, (byte) 179, (boolean) true, (byte) 6);
                                        try {
                                            if (!sAntReceiver.ANTTxMessage(msg2)) {
                                                releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: false"));
                                                return;
                                            }
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                            releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_while), "ANTTxMessage: RemoteException"));
                                            return;
                                        }
                                        if (Debug.ON) Log.v(TAG, "--------> sent=" + messageToString(msg2));
                                        return;
                                    }
                                } else if (antMessage.length > 4 && antMessage[4] == (byte)0x02) {//AntDefine.EVENT_RX_FAIL) {
                                    if (Debug.ON) Log.v(TAG, "EVENT_RX_FAIL, missed message");
                                    return;
                                } else if (antMessage.length > 4 && antMessage[4] == (byte)0x08) {//AntDefine.EVENT_RX_FAIL_GO_TO_SEARCH) {
                                    if (Debug.ON) Log.v(TAG, "EVENT_RX_FAIL_GO_TO_SEARCH 2");
                                    //releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                } else if (antMessage.length > 4 && antMessage[4] == (byte)0x01) {//AntDefine.EVENT_RX_SEARCH_TIMEOUT) {
                                    releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                } else if (antMessage.length > 4 && antMessage[4] == (byte)0x07) {//AntDefine.EVENT_CHANNEL_CLOSED) {
                                    releaseService(String.format(sContext.getString(R.string.weight_process_msg_problem_event), messageToString(antMessage)));
                                    return;
                                }
                            }
                            if (Debug.ON) Log.v(TAG, "MESG_RESPONSE_EVENT_ID " + messageToString(antMessage) +  " state=" + state.toString());
                        }*/ else if (Debug.ON) Log.w(TAG, "*** Received messageId=" + antMessage[1] + " messageData" + messageToString(antMessage) +  " state=" + state.toString());
                        break;
                    default:
                        if (Debug.ON) Log.i(TAG, "UNEXPECTED message " + messageToString(antMessage) +  " state=" + state.toString());
                        break;
                }
            } else if (Debug.ON) Log.i(TAG, "UNEXPECTED enter data onReceive: " + antAction);
        }
    };

    /**
     * Class for interacting with the ANT interface.
     */
    private ServiceConnection sIAntConnection;

    /**
     * Binds this activity to the ANT service.
     */
    void initService() {
        boolean ret;
        got1 = got2 = got3 = got4 = got5 = shown = false;
        hasSegmental = got_c5 = got_bc = got_c8 = got_b9 = got_b0 = false;
        the_weight = new Weight();
        state = stateEnum.NONE;
        userProfileIdentification = -1;

        worker = Executors.newSingleThreadScheduledExecutor();

        Runnable task = () -> {
            if (Debug.ON) Log.v(TAG, "***** Runnable configTimeout executed *****");
            if (Debug.ON) Log.v(TAG, "Search timeout, NO ANT RESPONSE");
            if (state == stateEnum.STARTING_UP)
            {
                //Check ANT Radio Service permissions
                if (Build.VERSION.SDK_INT >= 16) {
                    try {
                        PackageInfo pi = sContext.getApplicationContext().getPackageManager().getPackageInfo("com.dsi.ant.service.socket", PackageManager.GET_PERMISSIONS);
                        final String[] requestedPermissions = pi.requestedPermissions;
                        boolean enabled = false;
                        for (int i = 0, len = requestedPermissions.length; i < len; i++) {
                            if (requestedPermissions[i].startsWith("com.dsi.ant.permission.ANT") &&
                                    ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0))
                            {
                                enabled = true;
                                break;
                            }
                        }
                        if (!enabled)
                        {
                            releaseService(R.string.msg_problem_ant_permission_disabled);
                            return;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            releaseService(R.string.weight_process_msg_problem_timeout);
        };
        scheduledFuture = worker.schedule(task, configTimeout, TimeUnit.SECONDS);


        if(!sServiceConnected) {
            sIAntConnection = new ServiceConnection() {
                // Flag to know if the ANT App was interrupted
                private boolean antInterrupted = false;

                public void onServiceConnected(ComponentName pClassName, IBinder pService) {
                    // This is called when the connection with the service has been
                    // established, giving us the service object we can use to
                    // interact with the service.  We are communicating with our
                    // service through an IDL interface, so get a client-side
                    // representation of that from the raw service object.
                    if(Debug.ON) Log.d(TAG, "sIAntConnection onServiceConnected()");
                    sAntReceiver = IAnt_6.Stub.asInterface(pService);

                    if (sAntReceiver == null) {
                        if (Debug.ON) Log.e(TAG, "Failed to get ANT Receiver");
                        //TODO: avisar al Fragment
                        return;
                    }

                    sServiceConnected = true;

                    try {
                        if (!sAntReceiver.claimInterface()) {
                            if (Debug.ON) Log.e(TAG, "failed to claim ANT interface");
                            //TODO avisar al Fragment
                            return;
                        }

                        if (!sAntReceiver.isEnabled()) {
                            // Make sure not to call AntInterface.enable() again, if it has been
                            // already called before
                            if (!antInterrupted) {
                                if (Debug.ON) Log.i(TAG, "Powering on Radio");
                                sAntReceiver.enable();
                                antInterrupted = true;
                            }
                        } else {
                            if (Debug.ON) Log.i(TAG, "Radio already enabled");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    try {
                        // We expect this call to throw an exception due to a bug in the ANT
                        // Radio Service.  It won't actually fail, though, as we'll get the
                        // startup message (see {@link AntStartupMessage}) one normally expects
                        // after a reset.  Channel initialization can proceed once we receive
                        // that message.
                        sAntReceiver.ANTResetSystem();

                        if (Debug.ON) Log.i(TAG, "resetting ANT OK");
                    } catch (Exception e) {
                        if (Debug.ON) Log.e(TAG, "failed to reset ANT (expected exception) " + e);
                    }

                    //setupChannel();
                    // We register for ANT intents early because we want to have a record of
                    // the status intents in the log as we start up.
                    registerForAntIntents();

                    //Log.e(TAG, "Sensor.SensorState.CONNECTING");
                    state = stateEnum.STARTING_UP;
                }

                public void onServiceDisconnected(ComponentName pClassName) {
                    // This is called when the connection with the service has been
                    // unexpectedly disconnected -- that is, its process crashed.
                    if(Debug.ON) Log.d(TAG, "sIAntConnection onServiceDisconnected()");
                    sAntReceiver = null;

                    sServiceConnected = false;

                    // Try and rebind to the service
                    //INSTANCE.releaseService();
                    //INSTANCE.initService();
                }


            };

            Intent intent = MainActivity.createExplicitFromImplicitIntent(sContext, new Intent("com.dsi.ant.IAnt_6"));
            if (intent != null) {
                ret = sContext.bindService(intent, sIAntConnection, Context.BIND_AUTO_CREATE);
                if (Debug.ON) Log.i(TAG, "initService(): Bound with ANT service: " + ret);
            }
            else
            {
                ret = false;
                if (Debug.ON) Log.i(TAG, "createExplicitFromImplicitIntent(): Returned null");
            }
            if (!ret) releaseService(R.string.weight_process_msg_problem_bind);
        } else {
            if(Debug.ON) Log.d(TAG, "initService: already initialised service");
            //ret = true;
        }
    }

    private void releaseService(int resid) {
        releaseService(sContext.getString(resid));
    }

    /** Unbinds this activity from the ANT service. */
    private void releaseService(final String reason) {
        state = stateEnum.END;

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        // TODO Make sure can handle multiple calls to onDestroy
        if (sServiceConnected) {
            try {
                sAntReceiver.ANTCloseChannel((byte) 0);
                sAntReceiver.ANTUnassignChannel((byte) 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            unregisterForAntIntents();
            sContext.unbindService(sIAntConnection);
            sServiceConnected = false;
            sIAntConnection = null;
        }

        worker.shutdownNow();
        sProgressDialog.cancel();

        if (reason != null) {
            if (sFragment.getActivity() != null) {
                //handler.post(new Runnable() {
                sFragment.getActivity().runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(sContext);
                    builder.setMessage(reason).
                            setTitle(sContext.getString(R.string.weight_process_msg_error)).
                            setIcon(android.R.drawable.ic_dialog_alert);

                    if (reason.equals(sContext.getString(R.string.msg_problem_ant_permission_disabled)))
                    {
                        builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:com.dsi.ant.service.socket"));
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            sContext.startActivity(intent);
                            dialog.cancel();
                            //finish();
                        }).setCancelable(false);
                    }
                    else
                    {
                        builder.setPositiveButton(android.R.string.yes, null);
                    }
                    builder.create().show();
                });
            }
        }

        if (the_weight.weight != -1) {
            //Weight OK, save to history
            the_weight.uuid = the_user.uuid;
            the_weight.age = the_user.age;
            the_weight.isMale = the_user.isMale;
            the_weight.height = the_user.height_cm;
            ((MainActivity)sContext).saveWeight(the_weight);
        }

        sFragment.userToUpload = the_user;
        sFragment.updateUi();

        if(Debug.ON) Log.d(TAG, "releaseService() unbound.");
    }

    private short userProfileIdentification = -1;
    private byte[] genUserProfileDataPage() {
        if (userProfileIdentification == -1) {
            /*short tmp = 0;
            for (byte chars : the_user.name.getBytes()) tmp += chars;
            tmp += the_user.activity_level;
            tmp += (the_user.isLifetimeAthlete) ? 1 : 0;
            tmp += (the_user.usesCm) ? the_user.height_cm : the_user.height_ft + the_user.height_in;
            tmp += the_user.age;
            tmp += (the_user.isMale) ? 1 : 0;
            userProfileIdentification = 256;
            userProfileIdentification += (tmp % 30000);*/
            userProfileIdentification = (short) new Random().nextInt(Short.MAX_VALUE + 1);

        }
        if (Debug.ON) Log.v(TAG, "Profile. isMale=" + the_user.isMale + ", age=" + the_user.age
                + ", height=" + the_user.height_cm + ", isLifetimeAthlete="
                + the_user.isLifetimeAthlete + ", activity_level=" + the_user.activity_level
                + ", userProfileIdentification=" + userProfileIdentification);

        //09 4e 00 3a 22 00 03 ff a0 b3 86 92 00 00
        //09 4e 00 3a 22 00 03 ff a0 b3 86
        byte[] ret = {(byte) 0x09, (byte) 0x4e, (byte) 0x00, (byte) 0x3a, (byte) 0xff, (byte) 0xff, (byte) 0x03, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        ret[4] = (byte)((userProfileIdentification >> 8) & 0xff);
        ret[5] = (byte)(userProfileIdentification & 0xff);
        ret[8] = (byte)((the_user.isMale ? 0x80 : 0x00) | (byte)the_user.age);
        byte height = (byte)the_user.height_cm;
        ret[9] = height;
        ret[10] = (byte)((the_user.isLifetimeAthlete ? 0x80 : 0x00) | (byte)the_user.activity_level);

        return ret;
    }

    void registerForAntIntents() {
        if (Debug.ON) Log.i(TAG, "Registering for ant intents. sServiceConnected=" + sServiceConnected);
        // Register for ANT intent broadcasts.
        if (sServiceConnected) {
            IntentFilter statusIntentFilter = new IntentFilter();
            statusIntentFilter.addAction("com.dsi.ant.intent.action.ANT_ENABLED");
            statusIntentFilter.addAction("com.dsi.ant.intent.action.ANT_DISABLED");
            statusIntentFilter.addAction("com.dsi.ant.intent.action.ANT_INTERFACE_CLAIMED_ACTION");
            statusIntentFilter.addAction("com.dsi.ant.intent.action.ANT_RESET");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sContext.registerReceiver(statusReceiver, statusIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                sContext.registerReceiver(statusReceiver, statusIntentFilter);
            }

            IntentFilter dataIntentFilter = new IntentFilter();
            dataIntentFilter.addAction("com.dsi.ant.intent.action.ANT_RX_MESSAGE_ACTION");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sContext.registerReceiver(dataReceiver, dataIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                sContext.registerReceiver(dataReceiver, dataIntentFilter);
            }
        }
    }

    void unregisterForAntIntents() {
        if (sServiceConnected) {
            try
            {
                sContext.unregisterReceiver(statusReceiver);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                sContext.unregisterReceiver(dataReceiver);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private String messageToString(byte[] message) {
        StringBuilder out = new StringBuilder();
        for (byte b : message) {
            out.append(String.format("%s%02x", (out.length() == 0 ? "" : " "), b));
        }
        return out.toString();
    }

    User the_user;
    public void setProfile(User user) {
        this.the_user = user;
    }

    private static byte calcLimits(float[] limits, float value) {
        if (value < limits[0])  return 0;
        else if (value >= limits[0] && value < limits[1]) return 1;
        else if (value >= limits[1] && value <= limits[2]) return 2;
        else if (value > limits[2]) return 3;
        else return -1;
    }

    private static byte calcLimitsHydration(float[] limits, float value) {
        if (value < limits[0]) return 0;
        else if (value >= limits[0] && value <= limits[1]) return 1;
        else if (value > limits[1]) return 2;
        else return -1;
    }

    private static byte calcLimitsBone(float limit, float value) {
        if (value < 0.9*limit) return 0;
        else return 1;
    }

    static byte getPercentHydrationDesc(float percentHydration, boolean isMale) {
        float[] limits;
        if (isMale) {
            limits = new float[]{50, 65};
        } else {
            limits = new float[]{45, 60};
        }
        return calcLimitsHydration(limits, percentHydration);
    }

    static byte getBoneMassDesc(float weight, float boneMass, boolean isMale) {
        if (isMale) {
            if (weight < 65.0) return calcLimitsBone(2.66f, boneMass);
            else if (weight < 95.0) return calcLimitsBone(3.29f, boneMass);
            else return calcLimitsBone(3.69f, boneMass);
        } else {
            if (weight < 50.0) return calcLimitsBone(1.95f, boneMass);
            else if (weight < 75.0) return calcLimitsBone(2.40f, boneMass);
            else return calcLimitsBone(2.95f, boneMass);
        }
    }

    static byte getBMIDesc(byte age, float bmi, boolean isMale) {
        if ((age >= 19) && (age <= 24)) {
            float[] limits = {19f, 24f, 30f};
            return calcLimits(limits, bmi);
        } else if ((age >= 25) && (age <= 34)) {
            float[] limits = {20f, 25f, 30.5f};
            return calcLimits(limits, bmi);
        } else if ((age >= 35) && (age <= 44)) {
            float[] limits = {21f, 26f, 31f};
            return calcLimits(limits, bmi);
        } else if ((age >= 45) && (age <= 54)) {
            float[] limits = {22f, 27f, 31.5f};
            return calcLimits(limits, bmi);
        } else if ((age >= 55) && (age <= 65)) {
            float[] limits = {23f, 28f, 32f};
            return calcLimits(limits, bmi);
        } else if (age > 65) {
            float[] limits = {24f, 29f, 32.5f};
            return calcLimits(limits, bmi);
        } else {
            if (isMale) {
                float[] limits;
                switch (age) {
                    case 7:
                    case 8:
                        limits = new float[] {14f, 18.6f, 20.6f};
                        return calcLimits(limits, bmi);
                    case 9:
                    case 10:
                        limits = new float[] {14.4f, 20.2f, 22.8f};
                        return calcLimits(limits, bmi);
                    case 11:
                    case 12:
                        limits = new float[] {15.2f, 21.8f, 25f};
                        return calcLimits(limits, bmi);
                    case 13:
                    case 14:
                        limits = new float[] {16.2f, 23.4f, 26.26f};
                        return calcLimits(limits, bmi);
                    case 15:
                    case 16:
                        limits = new float[] {17.3f, 24f, 27.8f};
                        return calcLimits(limits, bmi);
                    case 17:
                    case 18:
                        limits = new float[] {18.4f, 24f, 28.6f};
                        return calcLimits(limits, bmi);
                }
            } else {
                float[] limits;
                switch (age) {
                    case 7:
                    case 8:
                        limits = new float[] {13.8f, 18.9f, 20.9f};
                        return calcLimits(limits, bmi);
                    case 9:
                    case 10:
                        limits = new float[] {14.3f, 20.4f, 23f};
                        return calcLimits(limits, bmi);
                    case 11:
                    case 12:
                        limits = new float[] {15.1f, 22f, 25f};
                        return calcLimits(limits, bmi);
                    case 13:
                    case 14:
                        limits = new float[] {16.4f, 23.7f, 26.7f};
                        return calcLimits(limits, bmi);
                    case 15:
                    case 16:
                        limits = new float[] {17.5f, 24.8f, 27.6f};
                        return calcLimits(limits, bmi);
                    case 17:
                    case 18:
                        limits = new float[] {18.2f, 24f, 27.8f};
                        return calcLimits(limits, bmi);
                }
            }
            return -1;
        }
    }

    static byte getPercentFatDesc(byte age, float percentFat, boolean isMale) {
        if (isMale) {
            if (age >= 20 && age <= 39) {
                float[] limits = {8, 20, 25};
                return calcLimits(limits, percentFat);
            } else if (age >= 40 && age <= 59) {
                float[] limits = {11, 22, 28};
                return calcLimits(limits, percentFat);
            } else if (age >= 60 && age <= 79) {
                float[] limits = {13, 25, 30};
                return calcLimits(limits, percentFat);
            } else {
                float[] limits;
                switch (age) {
                    case 7:
                        limits = new float[] {13, 20, 25};
                        return calcLimits(limits, percentFat);
                    case 8:
                        limits = new float[] {13, 21, 26};
                        return calcLimits(limits, percentFat);
                    case 9:
                        limits = new float[] {13, 22, 27};
                        return calcLimits(limits, percentFat);
                    case 10:
                    case 11:
                    case 12:
                        limits = new float[] {13, 23, 38};
                        return calcLimits(limits, percentFat);
                    case 13:
                        limits = new float[] {12, 22, 27};
                        return calcLimits(limits, percentFat);
                    case 14:
                        limits = new float[] {12, 21, 26};
                        return calcLimits(limits, percentFat);
                    case 15:
                        limits = new float[] {11, 21, 24};
                        return calcLimits(limits, percentFat);
                    case 16:
                    case 17:
                    case 18:
                        limits = new float[] {10, 20, 24};
                        return calcLimits(limits, percentFat);
                    case 19:
                        limits = new float[] {9, 20, 24};
                        return calcLimits(limits, percentFat);
                    default:
                        return -1;
                }
            }

        } else {
            if (age >= 20 && age <= 39) {
                float[] limits = {21, 33, 39};
                return calcLimits(limits, percentFat);
            } else if (age >= 40 && age <= 59) {
                float[] limits = {23, 34, 40};
                return calcLimits(limits, percentFat);
            } else if (age >= 60 && age <= 79) {
                float[] limits = {24, 36, 42};
                return calcLimits(limits, percentFat);
            } else {
                float[] limits;
                switch (age) {
                    case 7:
                        limits = new float[] {15, 25, 29};
                        return calcLimits(limits, percentFat);
                    case 8:
                        limits = new float[] {15, 26, 30};
                        return calcLimits(limits, percentFat);
                    case 9:
                        limits = new float[] {16, 27, 31};
                        return calcLimits(limits, percentFat);
                    case 10:
                        limits = new float[] {16, 28, 32};
                        return calcLimits(limits, percentFat);
                    case 11:
                    case 12:
                    case 13:
                        limits = new float[] {16, 29, 33};
                        return calcLimits(limits, percentFat);
                    case 14:
                    case 15:
                    case 16:
                        limits = new float[] {16, 30, 34};
                        return calcLimits(limits, percentFat);
                    case 17:
                        limits = new float[] {16, 30, 35};
                        return calcLimits(limits, percentFat);
                    case 18:
                        limits = new float[] {17, 31, 36};
                        return calcLimits(limits, percentFat);
                    case 19:
                        limits = new float[] {19, 32, 37};
                        return calcLimits(limits, percentFat);
                    default:
                        return -1;
                }
            }
        }
    }

    //Using the Mifflin St Jeor Equation
    private double calcBMR(double weight, double height, double age, boolean isMale) {
        return 10.0 * weight + 6.25 * height - 5.0 * age + ((isMale) ? 5 : -161);
    }

    //Using http://www.globalrph.com/resting_metabolic_rate.htm
    private double calcAMR(double bmr, int activity_level) {
        switch (activity_level) {
            case 1: return 1.3 * bmr;//Up to 20 Minutes
            case 2: return 1.3375 * bmr;//21 to 40 Minutes
            case 3: return 1.375 * bmr;//41 to 60 Minutes
            case 4: return 1.55 * bmr;//1 to 5 hours
            case 5: return 1.7 * bmr;//5 to 9 hours
            case 6: return 1.9 * bmr;//Over 9 hours
            case 0://No exercise
            default: return 1.2 * bmr;
        }
    }

    private double calcMetAge(double height, boolean isMale, double bmr) {
        return 0.2 * (24.0 * Math.pow(height / 100, 2) + 6.25 * height + ((isMale) ? 5 : -161) -  bmr);
    }

    private double calcPercentFat(double weight, double height, double age, boolean isMale) {
        return 1.2 * (weight/Math.pow(height / 100, 2)) + 0.23 * age - ((isMale) ? 10.8 : 0) - 5.4;
    }
}
