package peterson.ttu.edu.backupaids.model;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peterson.ttu.edu.backupaids.R;

public class Preferences {
    private static Preferences smInstance;
    private final List<PresetUpdateListener> presetUpdateListeners = new ArrayList<>();

    private static final String SELECTED_PRESET = "SelectedPreset";
    private static final String MIC_TO_USE = "MicToUse";

    /**
     * This is Bluetooth connections we've made, not the full list from the phone...
     */
    private static final String BLUETOOTH_PEER_NAME = "BluetoothPeerName";

    public enum MicToUse {
        PHONE_MIC,
        HEADSET_MIC;
    }

    private final Context context;
    private String selectedPreset;
    private MicToUse micToUse = MicToUse.PHONE_MIC;

    public static Preferences getInstance(@NonNull Context context) {
        if ( smInstance == null) {
            synchronized (Preferences.class) {
                if ( smInstance == null) {
                    smInstance = new Preferences(context);
                }
            }
        }
        return smInstance;
    }

    private Preferences(@NonNull Context context) {
        this.context = context;
        readPreferences();
    }

    public void addPresetUpdateListener(PresetUpdateListener listener) {
        presetUpdateListeners.add(listener);
    }

    public void removePresetUpdateListner(PresetUpdateListener listener) {
        presetUpdateListeners.remove(listener);
    }

    private void readPreferences() {
        try (JsonReader jsonReader =
                     new JsonReader(
                             new InputStreamReader(
                                     new FileInputStream(
                                             new File(context.getFilesDir(), context.getString(R.string.preferences_file_name)))))) {
            jsonReader.beginObject();
            if ( jsonReader.hasNext()) {
                jsonReader.nextName();
                selectedPreset = jsonReader.nextString();
                if (selectedPreset.isEmpty()) {
                    selectedPreset = null;
                }
            }
            if ( jsonReader.hasNext()) {
                jsonReader.nextName();
                micToUse = micToUseFromInt(jsonReader.nextInt());
            }
            jsonReader.endObject();
        } catch (FileNotFoundException e) {
            // Okay, just no presets
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: handle this better...
        }
    }

    private MicToUse micToUseFromInt(int micToUse) {
        switch(micToUse) {
            case 0:
                return MicToUse.PHONE_MIC;
            case 1:
                return MicToUse.HEADSET_MIC;
            default:
                // An option pulled, most likely. Default to phone mic
                return MicToUse.PHONE_MIC;
        }
    }

    private int intFromMicToUse(MicToUse micToUse) {
        if ( micToUse == null) {
            return 0;
        }
        switch ( micToUse) {
            case PHONE_MIC:
                return 0;
            case HEADSET_MIC:
                return 1;
            default:
                // Means we forgot to add a new option here...
                throw new RuntimeException("Unexpected mic type " + micToUse);
        }
    }

    private void writePreferences() {
        try (JsonWriter jsonWriter = new JsonWriter(
                new PrintWriter(
                        new File(context.getFilesDir(),
                                context.getString(R.string.preferences_file_name))))) {
            jsonWriter.beginObject();
            if ( selectedPreset == null) {
                jsonWriter.name(SELECTED_PRESET).value("");
            } else {
                jsonWriter.name(SELECTED_PRESET).value(selectedPreset);
            }
            jsonWriter.name(MIC_TO_USE).value(intFromMicToUse(micToUse));
            jsonWriter.endObject();
        } catch (IOException e) {
            // TODO: handle this better...
            e.printStackTrace();
        }
    }

    public @Nullable SoundPreset getSelectedPreset() {
        // Check first to see if we have this preset...
        if ( selectedPreset != null) {
            if ( SoundPresetManager.getInstance(context).hasPreset(selectedPreset)) {
                return SoundPresetManager.getInstance(context).getPreset(selectedPreset);
            }
            // Preset doesn't exist. Null it out
            setSelectedPreset(null);
        }
        return null;
    }

    public void setSelectedPreset(String selectedPreset) {
        if ( selectedPreset != null && selectedPreset.equals(this.selectedPreset)) {
            // No change made
            return;
        }
        this.selectedPreset = selectedPreset;
        for ( PresetUpdateListener listener : presetUpdateListeners) {
            listener.selectedPresetUpdated(selectedPreset);
        }
        writePreferences();
    }

    public MicToUse getMicToUse() {
        return micToUse;
    }

    public void setMicToUse(MicToUse micToUse) {
        this.micToUse = micToUse;
        writePreferences();
    }

    public interface PresetUpdateListener {
        void selectedPresetUpdated(String newPresetName);
    }
}
