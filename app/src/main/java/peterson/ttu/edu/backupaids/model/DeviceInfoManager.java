package peterson.ttu.edu.backupaids.model;

import android.content.Context;
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
import java.util.Comparator;
import java.util.List;

import peterson.ttu.edu.backupaids.R;

public class DeviceInfoManager {

    private static final String TAG = "DeviceInfoManager";

    private static DeviceInfoManager instance;

    private final Context context;
    private final List<DeviceInfo> deviceInfoList = new ArrayList<>();

    public static DeviceInfoManager getInstance(Context context) {
        if ( instance == null) {
            synchronized (SoundPresetManager.class) {
                if ( instance == null) {
                    instance = new DeviceInfoManager(context);
                }
            }
        }
        return instance;
    }

    private DeviceInfoManager(Context context) {
        this.context = context;
        try (JsonReader jsonReader =
                     new JsonReader(
                             new InputStreamReader(
                                     new FileInputStream(
                                             new File(context.getFilesDir(), context.getString(R.string.device_info_file_name)))));) {

            readAllPresets(jsonReader);
        } catch (FileNotFoundException e) {
            // Okay, just no presets
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: handle this better...
        }
    }

    private void readAllPresets(JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        while(jsonReader.hasNext()) {
            if ( jsonReader.peek().equals(JsonToken.END_DOCUMENT)) {
                return;
            }
            DeviceInfo deviceInfo = new DeviceInfo(jsonReader);
            deviceInfoList.add(deviceInfo);
        }
        jsonReader.endArray();
        Collections.sort(deviceInfoList);
    }

    public void addDevice(DeviceInfo deviceInfo) {
        if ( deviceInfoList.contains(deviceInfo)) {
            Log.d(TAG, "Device already known: " + deviceInfo.getName());
            return;
        }
        Log.d(TAG, "Saving device " + deviceInfo.getName());
        deviceInfoList.add(deviceInfo);
        Collections.sort(deviceInfoList);
        saveDeviceList();
    }

    /**
     * Call this when you're ready to save changes to 1+ presets
     */
    private void saveDeviceList() {
        try (JsonWriter jsonWriter = new JsonWriter(
                new PrintWriter(
                        new File(context.getFilesDir(),
                                context.getString(R.string.device_info_file_name))))) {
            jsonWriter.beginArray();
            for (DeviceInfo deviceInfo : deviceInfoList) {
                deviceInfo.saveDevice(jsonWriter);
            }
            jsonWriter.endArray();
        } catch (IOException e) {
            // TODO: handle this better...
            e.printStackTrace();
        }
        // We tried...
    }

    public boolean hasDevices() {
        return !deviceInfoList.isEmpty();
    }

    public void deletePreset(DeviceInfo deviceInfo) {
        deviceInfoList.remove(deviceInfo);
        saveDeviceList();
    }

}
