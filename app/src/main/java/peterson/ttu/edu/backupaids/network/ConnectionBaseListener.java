package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

/**
 * Callback methods common to all connection classes
 */
public interface ConnectionBaseListener {

    void servicesStarted();
    void servicesStopped();
    void servicePublishingFailed();
    void connectionFailed(IOException e);
    void connectionClosed();
}
