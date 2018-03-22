package peterson.ttu.edu.backupaids;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;

/**
 * Stuff that should probably be in a resource someday...
 */

public class Util {

    public static final String SERVICE_NAME = "ListenForExternalMic";
    public static final String BUDDY_NAME_STRING = "buddyname";
    public static final String LISTEN_PORT_STRING = "listenport";


    public static final int SAMPLE_RATE = 44100;

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

    // Taken from https://stackoverflow.com/questions/13307565/screen-on-off-detection
    public static boolean isScreenOn(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        boolean screenOn = false;
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                Log.i("Util", "Screen " + display.getName() + " is " + display.getState());
                screenOn = true;
            }
        }
        return screenOn;
    }

}
