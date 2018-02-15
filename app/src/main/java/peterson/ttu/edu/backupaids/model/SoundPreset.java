package peterson.ttu.edu.backupaids.model;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

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


    private String mName;
    private int mVolumeAdjust;
    private short Hz125;
    private short Hz250;
    private short Hz500;
    private short Hz1000;
    private short Hz2000;
    private short Hz3000;
    private short Hz4000;
    private short Hz8000;

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
                    Hz125 = (short)jsonReader.nextInt();
                    break;
                case HZ250:
                    Hz250 = (short)jsonReader.nextInt();
                    break;
                case HZ500:
                    Hz500 = (short)jsonReader.nextInt();
                    break;
                case HZ1000:
                    Hz1000 = (short)jsonReader.nextInt();
                    break;
                case HZ2000:
                    Hz2000 = (short)jsonReader.nextInt();
                    break;
                case HZ3000:
                    Hz3000 = (short)jsonReader.nextInt();
                    break;
                case HZ4000:
                    Hz4000 = (short)jsonReader.nextInt();
                    break;
                case HZ8000:
                    Hz8000 = (short)jsonReader.nextInt();
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
        this.Hz125 = other.Hz125;
        this.Hz250 = other.Hz250;
        this.Hz500 = other.Hz500;
        this.Hz1000 = other.Hz1000;
        this.Hz2000 = other.Hz2000;
        this.Hz3000 = other.Hz3000;
        this.Hz4000 = other.Hz4000;
        this.Hz8000 = other.Hz8000;
    }

    public SoundPreset(String name,
                       int volumeAdjust,
                       short hz125,
                       short hz250,
                       short hz500,
                       short hz1000,
                       short hz2000,
                       short hz3000,
                       short hz4000,
                       short hz8000) {
        this.mName = name;
        this.mVolumeAdjust = volumeAdjust;
        this.Hz125 = hz125;
        this.Hz250 = hz250;
        this.Hz500 = hz500;
        this.Hz1000 = hz1000;
        this.Hz2000 = hz2000;
        this.Hz3000 = hz3000;
        this.Hz4000 = hz4000;
        this.Hz8000 = hz8000;
    }

    /**
     * Save this preset. Package-private since only SoundPresetManager
     * should call it
     * @param jsonWriter
     */
    void savePreset(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(PRESET_NAME).value(mName);
        jsonWriter.name(VOLUME_LEVEL).value(mVolumeAdjust);
        jsonWriter.name(HZ125).value(Hz125);
        jsonWriter.name(HZ250).value(Hz250);
        jsonWriter.name(HZ500).value(Hz500);
        jsonWriter.name(HZ1000).value(Hz1000);
        jsonWriter.name(HZ2000).value(Hz2000);
        jsonWriter.name(HZ3000).value(Hz3000);
        jsonWriter.name(HZ4000).value(Hz4000);
        jsonWriter.name(HZ8000).value(Hz8000);
        jsonWriter.endObject();
    }

    public void setFrequencyAdjustment(int frequency, short value) {
        switch ( frequency) {
            case 125:
                Hz125 = value;
                break;
            case 250:
                Hz250 = value;
                break;
            case 500:
                Hz500 = value;
                break;
            case 1000:
                Hz1000 = value;
                break;
            case 2000:
                Hz2000 = value;
                break;
            case 3000:
                Hz3000 = value;
                break;
            case 4000:
                Hz4000 = value;
                break;
            case 8000:
                Hz8000 = value;
                break;
            default:
                throw new RuntimeException("Unknown frequency " + frequency);
        }
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

    public short getHz125() {
        return Hz125;
    }

    public void setHz125(short mHz125) {
        this.Hz125 = mHz125;
    }

    public short getHz250() {
        return Hz250;
    }

    public void setHz250(short mHz250) {
        this.Hz250 = mHz250;
    }

    public short getHz500() {
        return Hz500;
    }

    public void setHz500(short mHz500) {
        this.Hz500 = mHz500;
    }

    public short getHz1000() {
        return Hz1000;
    }

    public void setHz1000(short mHz1000) {
        this.Hz1000 = mHz1000;
    }

    public short getHz2000() {
        return Hz2000;
    }

    public void setHz2000(short mHz2000) {
        this.Hz2000 = mHz2000;
    }

    public short getHz3000() {
        return Hz3000;
    }

    public void setHz3000(short mHz3000) {
        this.Hz3000 = mHz3000;
    }

    public short getHz4000() {
        return Hz4000;
    }

    public void setHz4000(short mHz4000) {
        this.Hz4000 = mHz4000;
    }

    public short getHz8000() {
        return Hz8000;
    }

    public void setHz8000(short mHz8000) {
        this.Hz8000 = mHz8000;
    }

}
