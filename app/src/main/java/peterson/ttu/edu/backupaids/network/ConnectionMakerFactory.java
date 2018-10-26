package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.support.annotation.NonNull;

import peterson.ttu.edu.backupaids.util.ConnectionType;

public class ConnectionMakerFactory {

    public static ConnectionMaker createWiFiConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) {
        return new WiFiConnectionMaker(context, connectionListener, connectionType);
    }
}
