package peterson.ttu.edu.backupaids.network;

import java.io.IOException;

public interface ConnectionMaker {

    int OUR_PIN = (int)(Math.random() * 900000) + 100000;

    /**
     * Change who notifications go to (such as when we're resumed and are re-connecting to a running
     * service).
     * @param connectionListener Listener for callbacks
     */
    void changeConnectionListener(ConnectionListener connectionListener);

    /**
     * True if we're connected with a remote device
     */
    boolean isConnected();

    /**
     * Stop everything
     */
    void close();

    /**
     * Make a connection to a specified receiver
     * @param remoteAppInstanceName Who to connect to
     */
    void makeConnection(String remoteAppInstanceName);

    /**
     * Call when you're ready to start using an input stream. This will in turn
     * call handler on the same thread (i.e. so a service doesn't get stopped)
     * @param handler Handler for the input stream.
     */
    void readyForInputStream(InputStreamHandler handler) throws IOException;

    /**
     * Call when you're ready to start using an output stream. This will in turn
     * call handler on the same thread (i.e. so a service doesn't get stopped)
     * @param handler Handler for the input stream.
     */
    void readyForOutputStream(OutputStreamHandler handler) throws IOException;
}
