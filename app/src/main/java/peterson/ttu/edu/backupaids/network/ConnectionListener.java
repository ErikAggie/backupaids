package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

/**
 * Interface for connection events
 */
public interface ConnectionListener {

    void servicesStarted();
    void servicesStopped();
    void connectionDiscoveryFailed();
    void discoveryStarted();
    void foundAPeer(String peerName);
    void discoveryFinished();
    void nowDiscoverable();
    void findingPeerFailed(IOException e);
    void connectionReady();
    void connectionFailed(IOException e);
    void connectionClosed();
}
