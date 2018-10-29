package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.support.annotation.NonNull;

import peterson.ttu.edu.backupaids.util.ConnectionType;

public class ConnectionMakerFactory {

    public static ConnectionMaker createConnectionMaker(@NonNull Context context, ConnectionListener connectionListener, ConnectionType connectionType) {
        // In the future, this might be based on a preference, but for now, just create the single type we support.
        return new WiFiConnectionMaker(context, connectionListener, connectionType);
    }
}
