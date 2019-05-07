package peterson.ttu.edu.backupaids.util;

import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.SparseIntArray;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

import peterson.ttu.edu.backupaids.R;

/**
 * Stuff that should probably be in a resource someday...
 */

public class Util {

    public static final String SERVICE_NAME = "ListenForExternalMic";
    public static final String BUDDY_NAME_STRING = "BuddyName";
    public static final String LISTEN_PORT_STRING = "ListenPort";
    public static final String PIN_NUMBER_STRING = "PinNumber";

    // Generated from https://www.uuidgenerator.net/
    @SuppressWarnings("SpellCheckingInspection")
    public static final String UUID_STRING = "7aaaeccb-070b-454e-9640-ab8f0f38eff4";

    public static final String LISTEN_BUDDY_NAME = "HearingPhoneListen";
    public static final String SPEAK_BUDDY_NAME = "HearingPhoneSpeak";

    public static int LOCAL_SAMPLE_RATE;
    public static int REMOTE_SAMPLE_RATE;

    public static int LOCAL_MIN_BUFFER_SIZE;
    public static int REMOTE_MIN_BUFFER_SIZE;

    public static void setUpSampleRates(AudioManager audioManager) {
        int preferredRate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));

        // 48000 ought to be fine, but the equalizer balks at it
        LOCAL_SAMPLE_RATE = Math.min(44100, preferredRate);
        // Maximum we can stream is 22050, so make sure we cap things at that rate
        REMOTE_SAMPLE_RATE = Math.min(22050, preferredRate);

        LOCAL_MIN_BUFFER_SIZE =
                AudioRecord.getMinBufferSize(LOCAL_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
        REMOTE_MIN_BUFFER_SIZE =
                AudioRecord.getMinBufferSize(REMOTE_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
    }

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

    public static void rotateImageButton(ImageButton button) {
        // Add rotate animation, with code adapted from https://stackoverflow.com/questions/2032304/android-imageview-animation
        RotateAnimation anim =
                new RotateAnimation(0.0f,
                        360.0f,
                        Animation.RELATIVE_TO_SELF,
                        .5f,
                        Animation.RELATIVE_TO_SELF,
                        .5f);

        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(1500);
        button.startAnimation(anim);
    }

    /**
     * Check to see if no headphones are connected
     *
     * @param audioManager AudioManager to use to check
     * @return True if no headphones are connected; false otherwise
     */
    public static boolean noHeadphonesConnected(AudioManager audioManager) {
        AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for ( AudioDeviceInfo audioDevice : audioDevices) {
            switch(audioDevice.getType()) {
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_LINE_ANALOG:
                case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    return false;
            }
        }
        return true;
    }

}
