package peterson.ttu.edu.backupaids.util;

import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.SparseIntArray;

import peterson.ttu.edu.backupaids.R;

/**
 * Stuff that should probably be in a resource someday...
 */

public class Util {

    public static final String SERVICE_NAME = "ListenForExternalMic";
    public static final String BUDDY_NAME_STRING = "BuddyName";
    public static final String LISTEN_PORT_STRING = "ListenPort";
    public static final String PIN_NUMBER_STRING = "PinNumber";

    public static final String LISTEN_BUDDY_NAME = "HearingPhoneListen";
    public static final String SPEAK_BUDDY_NAME = "HearingPhoneSpeak";


    public static final String CONNECTION_NAME_EXTRA = "ConnectionName";

    public static final int SAMPLE_RATE = 44100;

    public static final int INPUT_MIN_BUFFER_SIZE =
            AudioRecord.getMinBufferSize(Util.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);


    public static final String SELECTED_PRESET_ITEM = "SelectedPreset";

    public static final SparseIntArray FREQUENCIES_TO_SOUND_IDS = new SparseIntArray();

    static {
        FREQUENCIES_TO_SOUND_IDS.put(125, R.raw.hz125);
        FREQUENCIES_TO_SOUND_IDS.put(250, R.raw.hz250);
        FREQUENCIES_TO_SOUND_IDS.put(500, R.raw.hz500);
        FREQUENCIES_TO_SOUND_IDS.put(1000, R.raw.hz1000);
        FREQUENCIES_TO_SOUND_IDS.put(2000, R.raw.hz2000);
        FREQUENCIES_TO_SOUND_IDS.put(3000, R.raw.hz3000);
        FREQUENCIES_TO_SOUND_IDS.put(4000, R.raw.hz4000);
        FREQUENCIES_TO_SOUND_IDS.put(8000, R.raw.hz8000);
    }

    public static final IntentFilter WIFI_P2P_INTENT_FILTER = new IntentFilter();
    static {
        WIFI_P2P_INTENT_FILTER.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        WIFI_P2P_INTENT_FILTER.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        WIFI_P2P_INTENT_FILTER.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        WIFI_P2P_INTENT_FILTER.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
}
