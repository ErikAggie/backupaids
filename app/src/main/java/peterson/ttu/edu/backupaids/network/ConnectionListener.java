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
    void foundPeers(List<String> peerNames);
    void findingPeerFailed(IOException e);
    void connectionReady() throws IOException;
    void connectionFailed(IOException e);
    void connectionClosed();
}
