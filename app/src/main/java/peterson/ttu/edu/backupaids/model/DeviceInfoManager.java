package peterson.ttu.edu.backupaids.model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
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
import java.util.Set;

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

        if ( deviceInfoList.isEmpty()) {
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDeviceList = bluetoothAdapter.getBondedDevices();

        // Check to make sure that the devices we have are still paired
        // Copy the list just to be safe since we may alter it...
        List<DeviceInfo> copyOfDeviceInfoList = new ArrayList<>(deviceInfoList);
        for ( DeviceInfo deviceInfo : copyOfDeviceInfoList) {
            boolean found = false;
            for ( BluetoothDevice pairedDevice : pairedDeviceList) {
                if ( pairedDevice.getAddress().equals(deviceInfo.getAddress())) {
                    found = true;
                    break;
                }
            }
            if ( !found) {
                Log.i(TAG, "Removing no-longer-paired device " + deviceInfo.getName());
                deleteDevice(deviceInfo);
            }
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

    /**
     * Call this when you're ready to save changes
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

    /**
     * Find the BluetoothDevice for a saved DeviceInfo
     * @param deviceInfo
     * @return
     */
    public static BluetoothDevice findBluetoothDevice(@NonNull DeviceInfo deviceInfo) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDeviceList = bluetoothAdapter.getBondedDevices();

        for ( BluetoothDevice bluetoothDevice : pairedDeviceList) {
            if ( bluetoothDevice.getAddress().equals(deviceInfo.getAddress())) {
                return bluetoothDevice;
            }
        }
        return null;
    }

    /**
     * Get the current list of connections
     * @return The current list of connections
     */
    public List<DeviceInfo> getCurrentList() {
        return Collections.unmodifiableList(deviceInfoList);
    }

    /**
     * Add a new device
     *
     * @param deviceInfo Device to add
     */
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

    public boolean hasDevices() {
        return !deviceInfoList.isEmpty();
    }

    public void deleteDevice(DeviceInfo deviceInfo) {
        deviceInfoList.remove(deviceInfo);
        saveDeviceList();
    }

}
