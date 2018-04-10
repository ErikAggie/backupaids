package peterson.ttu.edu.backupaids.model;

import android.annotation.SuppressLint;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds a preset
 */

public class SoundPreset {
    // Key names in the Json file
    private static final String PRESET_NAME = "PresetName";
    private static final String VOLUME_LEVEL = "VolumeLevel";
    private static final String HZ125 = "Hz125";
    private static final String HZ250 = "Hz250";
    private static final String HZ500 = "Hz500";
    private static final String HZ1000 = "Hz1000";
    private static final String HZ2000 = "Hz2000";
    private static final String HZ3000 = "Hz3000";
    private static final String HZ4000 = "Hz4000";
    private static final String HZ8000 = "Hz8000";

    public enum Frequency {
        HZ125("Hz125", 125),
        HZ250("Hz250", 250),
        HZ500("Hz500", 500),
        HZ1000("Hz1000", 1000),
        HZ2000("Hz2000", 2000),
        HZ3000("Hz3000", 3000),
        HZ4000("Hz4000", 4000),
        HZ8000("Hz8000", 8000);

        final String name;
        final int frequency;

        Frequency(String name, int frequency) {
            this.name = name;
            this.frequency = frequency;
        }
    }

    @SuppressLint("UseSparseArrays")
    private final Map<Integer, Short> mFrequencyValues = new HashMap<>();

    private String mName;
    private int mVolumeAdjust;

    /**
     * Constructor from Json. Package-private since only SoundPresetManager
     * should call it.
     */
    SoundPreset(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while ( jsonReader.hasNext()) {
            switch ( jsonReader.nextName()) {
                case PRESET_NAME:
                    mName = jsonReader.nextString();
                    break;
                case VOLUME_LEVEL:
                    mVolumeAdjust = jsonReader.nextInt();
                    break;
                case HZ125:
                    mFrequencyValues.put(Frequency.HZ125.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ250:
                    mFrequencyValues.put(Frequency.HZ250.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ500:
                    mFrequencyValues.put(Frequency.HZ500.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ1000:
                    mFrequencyValues.put(Frequency.HZ1000.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ2000:
                    mFrequencyValues.put(Frequency.HZ2000.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ3000:
                    mFrequencyValues.put(Frequency.HZ3000.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ4000:
                    mFrequencyValues.put(Frequency.HZ4000.frequency, (short)jsonReader.nextInt());
                    break;
                case HZ8000:
                    mFrequencyValues.put(Frequency.HZ8000.frequency, (short)jsonReader.nextInt());
                    break;
                default:
                    throw new RuntimeException("Unknown Json parameter " + jsonReader.nextName());
            }
        }
        jsonReader.endObject();
    }

    public SoundPreset() {}

    public SoundPreset(SoundPreset other) {
        this.mName = other.mName;
        this.mVolumeAdjust = other.mVolumeAdjust;
        this.mFrequencyValues.putAll(other.mFrequencyValues);
    }

    /**
     * Save this preset. Package-private since only SoundPresetManager
     * should call it
     * @param jsonWriter How to write the object
     */
    void savePreset(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(PRESET_NAME).value(mName);
        jsonWriter.name(VOLUME_LEVEL).value(mVolumeAdjust);
        jsonWriter.name(HZ125).value(mFrequencyValues.get(Frequency.HZ125.frequency));
        jsonWriter.name(HZ250).value(mFrequencyValues.get(Frequency.HZ250.frequency));
        jsonWriter.name(HZ500).value(mFrequencyValues.get(Frequency.HZ500.frequency));
        jsonWriter.name(HZ1000).value(mFrequencyValues.get(Frequency.HZ1000.frequency));
        jsonWriter.name(HZ2000).value(mFrequencyValues.get(Frequency.HZ2000.frequency));
        jsonWriter.name(HZ3000).value(mFrequencyValues.get(Frequency.HZ3000.frequency));
        jsonWriter.name(HZ4000).value(mFrequencyValues.get(Frequency.HZ4000.frequency));
        jsonWriter.name(HZ8000).value(mFrequencyValues.get(Frequency.HZ8000.frequency));
        jsonWriter.endObject();
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public int getVolumeAdjust() {
        return mVolumeAdjust;
    }

    public void setVolumeAdjust(int mVolumeAdjust) {
        this.mVolumeAdjust = mVolumeAdjust;
    }

    public void setFrequencyAdjustment(int frequency, short value) {
        mFrequencyValues.put(frequency, value);
    }

    public short getFrequencyAdjustment(int frequency) {
        if ( !mFrequencyValues.containsKey(frequency)) {
            return (short)0;
        }
        return mFrequencyValues.get(frequency);
    }

    public Map<Integer, Short> getFrequencyAdjustments() {
        @SuppressLint("UseSparseArrays") Map<Integer, Short> newMap = new HashMap<>(mFrequencyValues);
        return newMap;
    }

}
