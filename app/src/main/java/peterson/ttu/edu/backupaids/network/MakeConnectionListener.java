package peterson.ttu.edu.backupaids.network;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Callbacks specific to making a connection
 */
public interface MakeConnectionListener extends ConnectionBaseListener {
    void supportedPeersChanged(List<String> buddiesWithOurService);
    void connectionReady(Socket socket) throws IOException;
}
