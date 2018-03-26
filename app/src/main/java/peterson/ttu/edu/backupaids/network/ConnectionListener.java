package peterson.ttu.edu.backupaids.network;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Callback methods common to all connection classes
 */
public interface ConnectionListener {
    void servicesStarted();
    void servicesStopped();
    void servicePublishingFailed();
    void connectionFailed(IOException e);
    void connectionClosed();
    void foundAPeer(String peerName);
    void connectionReady(Socket socket) throws IOException;
}
