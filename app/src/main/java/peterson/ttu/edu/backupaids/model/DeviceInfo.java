package peterson.ttu.edu.backupaids.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

public class DeviceInfo implements Comparable<DeviceInfo> {

    private static final String NAME = "Name";
    private static final String ADDRESS = "Address";

    private String name;
    private String address;

    public DeviceInfo(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public DeviceInfo(BluetoothDevice bluetoothDevice) {
        this.name = bluetoothDevice.getName();
        this.address = bluetoothDevice.getAddress();
    }

    /**
     * Constructor from Json. Package-private since only SoundPresetManager
     * should call it.
     */
    DeviceInfo(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while ( jsonReader.hasNext()) {
            switch ( jsonReader.nextName()) {
                case NAME:
                    name = jsonReader.nextString();
                    break;
                case ADDRESS:
                    address = jsonReader.nextString();
                    break;
                default:
                    throw new RuntimeException("Unknown Json parameter " + jsonReader.nextName());
            }
        }
        jsonReader.endObject();
    }

    /**
     * Save this preset. Package-private since only SoundPresetManager
     * should call it
     * @param jsonWriter How to write the object
     */
    void saveDevice(JsonWriter jsonWriter) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(NAME).value(name);
        jsonWriter.name(ADDRESS).value(address);
        jsonWriter.endObject();
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof DeviceInfo)) {
            return false;
        }
        DeviceInfo other = (DeviceInfo) obj;

        // All that matters is the address (docs say it's a MAC address). This is unique to each device
        return address.equals(other.address);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public int compareTo(@NonNull DeviceInfo deviceInfo) {
        int nameComparison = name.compareTo(deviceInfo.name);
        if ( nameComparison != 0) {
            return nameComparison;
        }
        // Duplicate name. Tie break with device address
        return address.compareTo(deviceInfo.address);
    }
}
