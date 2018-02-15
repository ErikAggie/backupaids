package peterson.ttu.edu.backupaids.model;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peterson.ttu.edu.backupaids.R;

/**
 * Controls access to the presets
 */

public class SoundPresetManager {


    private static SoundPresetManager smInstance;

    private Map<String, SoundPreset> mPresets = new HashMap<>();

    public static SoundPresetManager getInstance(Context context) {
        if ( smInstance == null) {
            synchronized (SoundPresetManager.class) {
                if ( smInstance == null) {
                    smInstance = new SoundPresetManager(context);
                }
            }
        }
        return smInstance;
    }

    private final Context mContext;

    private SoundPresetManager(Context context) {
        mContext = context;
        JsonReader jsonReader = null;
        try {
            InputStream inputStream = new FileInputStream(new File(context.getFilesDir(), context.getString(R.string.preset_file_name)));
            jsonReader = new JsonReader(new InputStreamReader(inputStream));
            readAllPresets(jsonReader);
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

    private void readAllPresets(JsonReader jsonReader) throws IOException {
        while(jsonReader.hasNext()) {
            SoundPreset preset = new SoundPreset(jsonReader);
            mPresets.put(preset.getName(), preset);
        }
    }

    public void addPreset(SoundPreset preset) {
        mPresets.put(preset.getName(), preset);
        savePresets();
    }

    /**
     * Call this when you're ready to save changes to 1+ presets
     */
    public void savePresets() {
        JsonWriter jsonWriter = null;
        try {
            jsonWriter = new JsonWriter(
                            new PrintWriter(
                               new File(mContext.getFilesDir(),
                                        mContext.getString(R.string.preset_file_name))));
            for ( String presetName : mPresets.keySet()) {
                mPresets.get(presetName).savePreset(jsonWriter);
            }
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

    public List<String> getSortedPresetNames() {
        List<String> list = new ArrayList<>(mPresets.keySet());
        Collections.sort(list);
        return list;
    }

    public SoundPreset getPreset(String name) {
        return mPresets.get(name);
    }
}
