package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;

import peterson.ttu.edu.backupaids.util.ConnectionType;

/**
 * Create a ConnectionMaker (based in the future, perhaps, on a preference if WiFi direct would become viable)
 */
public class ConnectionMakerFactory {

    public static ConnectionMaker createConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) throws IOException {
        // In the future, this might be based on a preference, but for now, just create the single type we support
        //return new WiFiConnectionMaker(context, connectionListener, connectionType);
        return new BluetoothConnectionMaker(context, connectionListener, connectionType);
    }
}
