package peterson.ttu.edu.backupaids.network;

import java.io.IOException;
import java.net.Socket;

/**
 * Callback(s) specific to listening for connections
 */
public interface AcceptConnectionListener extends ConnectionBaseListener {
    void incomingConnection(Socket clientSocket) throws IOException;
}
