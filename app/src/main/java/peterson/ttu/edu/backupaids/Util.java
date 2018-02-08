package peterson.ttu.edu.backupaids;

import java.util.HashMap;

/**
 * Created by erika on 2/8/2018.
 */

public class Util {

    public static final int[] TEST_FREQUENCIES = {125, 250, 500, 1000, 2000, 4000, 8000};

    public static final HashMap<Integer, Integer> FREQUENCY_SOUND_MAP = new HashMap<>();

    static {
        FREQUENCY_SOUND_MAP.put(125, R.raw.hz125);
        FREQUENCY_SOUND_MAP.put(250, R.raw.hz250);
        FREQUENCY_SOUND_MAP.put(500, R.raw.hz500);
        FREQUENCY_SOUND_MAP.put(1000, R.raw.hz1000);
        FREQUENCY_SOUND_MAP.put(2000, R.raw.hz2000);
        FREQUENCY_SOUND_MAP.put(4000, R.raw.hz4000);
        FREQUENCY_SOUND_MAP.put(8000, R.raw.hz8000);
    }

    public static final String[] FREQUENCY_FRAGMENT_NAMES =
            {
                    "FREQUENCY_FRAGMENT_125",
                    "FREQUENCY_FRAGMENT_250",
                    "FREQUENCY_FRAGMENT_500",
                    "FREQUENCY_FRAGMENT_1000",
                    "FREQUENCY_FRAGMENT_2000",
                    "FREQUENCY_FRAGMENT_4000",
                    "FREQUENCY_FRAGMENT_8000",
            };

}
