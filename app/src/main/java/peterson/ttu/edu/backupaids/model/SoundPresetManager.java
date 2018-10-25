package peterson.ttu.edu.backupaids.model;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peterson.ttu.edu.backupaids.R;

/**
 * Controls access to the presets
 */

public class SoundPresetManager {


    private static SoundPresetManager instance;

    private final Context context;
    private final Map<String, SoundPreset> presets = new HashMap<>();

    public static SoundPresetManager getInstance(Context context) {
        if ( instance == null) {
            synchronized (SoundPresetManager.class) {
                if ( instance == null) {
                    instance = new SoundPresetManager(context);
                }
            }
        }
        return instance;
    }

    private SoundPresetManager(Context context) {
        this.context = context;
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
        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            if ( jsonReader.peek().equals(JsonToken.END_DOCUMENT)) {
                return;
            }
            SoundPreset preset = new SoundPreset(jsonReader);
            presets.put(preset.getName(), preset);
        }
        jsonReader.endArray();
    }

    public void addOrReplacePreset(SoundPreset preset) {
        presets.put(preset.getName(), preset);
        savePresets();
    }

    /**
     * Call this when you're ready to save changes to 1+ presets
     */
    private void savePresets() {
        JsonWriter jsonWriter = null;
        try {
            jsonWriter = new JsonWriter(
                            new PrintWriter(
                               new File(context.getFilesDir(),
                                        context.getString(R.string.preset_file_name))));
            jsonWriter.beginArray();
            for ( String presetName : presets.keySet()) {
                presets.get(presetName).savePreset(jsonWriter);
            }
            jsonWriter.endArray();
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

    public List<SoundPreset> getAllSortedPresets() {
        List<SoundPreset> presetList = new ArrayList<SoundPreset>();
        presetList.addAll(presets.values());

        // Sort by preset name
        Collections.sort(presetList, new Comparator<SoundPreset>() {
            @Override
            public int compare(SoundPreset o1, SoundPreset o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return presetList;
    }

    public int getNumberOfPresets() {
        return presets.size();
    }

    public String[] getSortedPresetNames() {
        List<String> list = new ArrayList<>(presets.keySet());
        Collections.sort(list);
        String[] array = new String[list.size()];
        return list.toArray(array);
    }

    public boolean hasPreset(String name) {
        return (presets.get(name) != null);
    }

    public SoundPreset getPreset(int position) {
        List<String> list = new ArrayList<>(presets.keySet());
        Collections.sort(list);
        if ( list.size() <= position) {
            throw new RuntimeException("Non-existent preset position: " + position);
        }
        return getPreset(list.get(position));
    }

    public SoundPreset getPreset(String name) {
        SoundPreset original = presets.get(name);
        if ( original == null) {
            throw new RuntimeException("Asking for non-existent preset " + name);
        }
        // Make a copy so that any unsaved changes don't affect the "true" version
        return new SoundPreset(original);
    }

    public void deletePreset(SoundPreset soundPreset) {
        if (presets.remove(soundPreset.getName()) != null) {
            savePresets();
            Preferences preferences = Preferences.getInstance(context);
            // Removal was successful
            if ( presets.size() == 0) {
                preferences.setSelectedPreset(null);
            } else if ( preferences.getSelectedPreset() == null) {
                // We just deleted the selected preset...
                preferences.setSelectedPreset(getPreset(0).getName());
            }
        }
    }
}
