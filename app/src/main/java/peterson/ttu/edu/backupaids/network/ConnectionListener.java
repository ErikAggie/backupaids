package peterson.ttu.edu.backupaids.network;

import java.io.IOException;
import java.util.List;

/**
 * Interface for connection events
 */
public interface ConnectionListener {

    void servicesStarted();
    void servicesStopped();
    void servicePublishingFailed();
    void discoveryStarted();
    void foundAPeer(String peerName);
    void discoveryFinished();
    void findingPeerFailed(IOException e);
    void connectionReady() throws IOException;
    void connectionFailed(IOException e);
    void connectionClosed();
}
