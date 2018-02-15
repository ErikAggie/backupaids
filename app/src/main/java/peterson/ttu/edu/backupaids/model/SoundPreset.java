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
    private short mHz125;
    private short mHz250;
    private short mHz500;
    private short mHz1000;
    private short mHz2000;
    private short mHz3000;
    private short mHz4000;
    private short mHz8000;

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
                    mHz125 = (short)jsonReader.nextInt();
                    break;
                case HZ250:
                    mHz250 = (short)jsonReader.nextInt();
                    break;
                case HZ500:
                    mHz500 = (short)jsonReader.nextInt();
                    break;
                case HZ1000:
                    mHz1000 = (short)jsonReader.nextInt();
                    break;
                case HZ2000:
                    mHz2000 = (short)jsonReader.nextInt();
                    break;
                case HZ3000:
                    mHz3000 = (short)jsonReader.nextInt();
                    break;
                case HZ4000:
                    mHz4000 = (short)jsonReader.nextInt();
                    break;
                case HZ8000:
                    mHz8000 = (short)jsonReader.nextInt();
                    break;
                default:
                    throw new RuntimeException("Unknown Json parameter " + jsonReader.nextName());
            }
        }
        jsonReader.endObject();
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
        this.mHz125 = hz125;
        this.mHz250 = hz250;
        this.mHz500 = hz500;
        this.mHz1000 = hz1000;
        this.mHz2000 = hz2000;
        this.mHz3000 = hz3000;
        this.mHz4000 = hz4000;
        this.mHz8000 = hz8000;
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
        jsonWriter.name(HZ125).value(mHz125);
        jsonWriter.name(HZ250).value(mHz250);
        jsonWriter.name(HZ500).value(mHz500);
        jsonWriter.name(HZ1000).value(mHz1000);
        jsonWriter.name(HZ2000).value(mHz2000);
        jsonWriter.name(HZ3000).value(mHz3000);
        jsonWriter.name(HZ4000).value(mHz4000);
        jsonWriter.name(HZ8000).value(mHz8000);
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

    public short getHz125() {
        return mHz125;
    }

    public void setHz125(short mHz125) {
        this.mHz125 = mHz125;
    }

    public short getHz250() {
        return mHz250;
    }

    public void setHz250(short mHz250) {
        this.mHz250 = mHz250;
    }

    public short getHz500() {
        return mHz500;
    }

    public void setHz500(short mHz500) {
        this.mHz500 = mHz500;
    }

    public short getHz1000() {
        return mHz1000;
    }

    public void setHz1000(short mHz1000) {
        this.mHz1000 = mHz1000;
    }

    public short getHz2000() {
        return mHz2000;
    }

    public void setHz2000(short mHz2000) {
        this.mHz2000 = mHz2000;
    }

    public short getHz3000() {
        return mHz3000;
    }

    public void setHz3000(short mHz3000) {
        this.mHz3000 = mHz3000;
    }

    public short getHz4000() {
        return mHz4000;
    }

    public void setHz4000(short mHz4000) {
        this.mHz4000 = mHz4000;
    }

    public short getHz8000() {
        return mHz8000;
    }

    public void setHz8000(short mHz8000) {
        this.mHz8000 = mHz8000;
    }

}
