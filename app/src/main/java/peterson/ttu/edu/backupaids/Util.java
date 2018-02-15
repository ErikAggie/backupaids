package peterson.ttu.edu.backupaids;

import android.util.SparseIntArray;

/**
 * Stuff that should probably be in a resource someday...
 */

public class Util {


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

}
