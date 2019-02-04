package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;

import peterson.ttu.edu.backupaids.model.DeviceInfo;
import peterson.ttu.edu.backupaids.util.ConnectionType;

/**
 * Create a ConnectionMaker (based in the future, perhaps, on a preference if WiFi direct would become viable)
 */
public class ConnectionMakerFactory {

    /**
     * Create a ConnectionMaker and look for a new connection
     * @param context
     * @param connectionListener
     * @param connectionType
     * @return
     * @throws IOException
     */
    public static ConnectionMaker createConnectionMaker(@NonNull Context context,
                                                        @NonNull ConnectionListener connectionListener,
                                                        @NonNull ConnectionType connectionType) throws IOException {
        // In the future, this might be based on a preference, but for now, just create the single type we support
        //return new WiFiConnectionMaker(context, connectionListener, connectionType);
        return new BluetoothConnectionMaker(context, connectionListener, connectionType, null);
    }

    /**
     * Create a ConnectionMaker where we want to listen but not make ourselves discoverable
     *
     * @param context
     * @param connectionListener
     * @param connectionType
     * @return
     * @throws IOException
     */
    public static ConnectionMaker createConnectionMakerNoDiscovery(@NonNull Context context,
                                                                   @NonNull ConnectionListener connectionListener,
                                                                   @NonNull ConnectionType connectionType) throws IOException {
        // Use a blank DeviceInfo
        return new BluetoothConnectionMaker(context, connectionListener, connectionType, new DeviceInfo("", ""));
    }

    /**
     * Create a ConnectionMaker and have it connect to an existing connection
     * @param context
     * @param connectionListener
     * @param connectionType
     * @param deviceInfo
     * @return
     * @throws IOException
     */
    public static ConnectionMaker createConnectionMakerExistingConnection(@NonNull Context context,
                                                                          @NonNull ConnectionListener connectionListener,
                                                                          @NonNull ConnectionType connectionType,
                                                                          DeviceInfo deviceInfo) throws IOException {
        return new BluetoothConnectionMaker(context, connectionListener, connectionType, deviceInfo);
    }
}
