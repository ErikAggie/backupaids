package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

/**
 * Interface for connection events
 */
public interface ConnectionListener {

    void servicesStarted();
    void servicesStopped();
    void servicePublishingFailed();
    void foundAPeer(String peerName);
    void findingPeerFailed(IOException e);
    void connectionReady() throws IOException;
    void connectionFailed(IOException e);
    void connectionClosed();
}
