package peterson.ttu.edu.backupaids.model;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.R;

public class Preferences {
    private static Preferences smInstance;

    private static final String SELECTED_PRESET = "SelectedPreset";
    private static final String MIC_TO_USE = "MicToUse";

    public enum MicToUse {
        PHONE_MIC,
        HEADSET_MIC;
    }

    private final Context context;
    private String selectedPreset;
    private MicToUse micToUse = MicToUse.PHONE_MIC;

    public static Preferences getInstance(Context context) {
        if ( smInstance == null) {
            synchronized (Preferences.class) {
                if ( smInstance == null) {
                    smInstance = new Preferences(context);
                }
            }
        }
        return smInstance;
    }

    private Preferences(Context context) {
        this.context = context;
        readPreferences();
    }

    private void readPreferences() {
        JsonReader jsonReader = null;
        try {
            InputStream inputStream = new FileInputStream(new File(context.getFilesDir(), context.getString(R.string.preferences_file_name)));
            jsonReader = new JsonReader(new InputStreamReader(inputStream));
            jsonReader.beginObject();
            while ( jsonReader.hasNext()) {
                readPreference(jsonReader);
            }
            jsonReader.endObject();
        } catch (FileNotFoundException e) {
            // Okay, just no presets
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: handle this better...
        } finally {
            if ( jsonReader != null) {
                try {
                    jsonReader.close();
                } catch ( Exception e) {
                    // We tried...
                }
            }
        }

    }

    private void readPreference(JsonReader jsonReader) throws IOException {
        switch(jsonReader.nextName()) {
            case SELECTED_PRESET:
                selectedPreset = jsonReader.nextString();
                break;
            case MIC_TO_USE:
                micToUse = micToUseFromInt(jsonReader.nextInt());
                break;
            default:
                // Nothing to do, really. It'll get destroyed the next time we save...
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
        JsonWriter jsonWriter = null;
        try {
            jsonWriter = new JsonWriter(
                    new PrintWriter(
                            new File(context.getFilesDir(),
                                    context.getString(R.string.preferences_file_name))));
            jsonWriter.beginObject();
            if ( selectedPreset != null) {
                jsonWriter.name(SELECTED_PRESET).value(selectedPreset);
            }
            jsonWriter.name(MIC_TO_USE).value(intFromMicToUse(micToUse));
            jsonWriter.endObject();
        } catch ( IOException e) {
            // TODO: handle this better...
            e.printStackTrace();
        } finally {
            if ( jsonWriter != null) {
                try {
                    jsonWriter.close();
                } catch ( IOException e) {
                    // We tried...
                }
            }
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
        if ( selectedPreset.equals(this.selectedPreset)) {
            // No change made
            return;
        }
        this.selectedPreset = selectedPreset;
        writePreferences();
    }

    public MicToUse getMicToUse() {
        return micToUse;
    }

    public void setMicToUse(MicToUse micToUse) {
        this.micToUse = micToUse;
        writePreferences();
    }
}
