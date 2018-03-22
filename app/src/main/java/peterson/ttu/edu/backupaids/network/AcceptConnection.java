package peterson.ttu.edu.backupaids.network;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import peterson.ttu.edu.backupaids.Util;

/**
 * Controller (kind of...) for listening for connections
 */
public class AcceptConnection extends ConnectionManager {
    private static final String TAG = "AcceptConnection";

    private final AcceptConnectionListener acceptConnectionListener;
    private ServerSocket serverSocket;


    /**
     * Sets everything up and begins service discovery.
     *
     * @param context Activity to use as context
     * @param listener Listener for events from us
     */
    public AcceptConnection(Context context, AcceptConnectionListener listener) {
        super(context, listener);
        acceptConnectionListener = listener;

        context.registerReceiver(this, Util.WIFI_P2P_INTENT_FILTER);
        listenForConnections();
        publishService();
        beginServiceDiscovery();
    }

    /**
     * Stop everything
     */
    public void close() {
        stopServiceDiscovery();
        unpublishService();
        stopListeningForConnections();
        context.unregisterReceiver(this);
    }

    private void listenForConnections() {
        if ( serverSocket != null) {
            try {
                serverSocket.close();
            } catch ( IOException e) {
                // Nothing we can do...
            } finally {
                serverSocket = null;
            }
        }
        try {
            serverSocket = new ServerSocket(0);
            listenPortNumber = serverSocket.getLocalPort();
        } catch ( IOException e) {
            Log.e(TAG, "Error creating server socket: " + e.getMessage(), e);
            acceptConnectionListener.connectionFailed(e);
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Do this until we're not listening for connections anymore
                    while ( serverSocket != null) {
                        Log.i(TAG, "Listening for connections.");
                        Socket clientSocket = serverSocket.accept();
                        if ( serverSocket == null) {
                            // We're not listening anymore, so stop
                            try {
                                clientSocket.close();
                            } catch ( Exception e) {

                            }
                            break;
                        }
                        handleSocket(clientSocket);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Connection listening stopped: " + e.getMessage(), e);
                    e.printStackTrace();
                } finally {
                    if ( serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            // Don't care
                        } finally {
                            serverSocket = null;
                        }
                    }
                }
            }
        }).start();
    }

    private void handleSocket(final Socket clientSocket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Accepted connection from " + clientSocket.getInetAddress().getCanonicalHostName());
                // TODO: do some sort of security verification (send a code phrase first, perhaps)
                try {
                    acceptConnectionListener.incomingConnection(clientSocket);
                } catch ( IOException e) {
                    // The connection might've been caught elsewhere. This is okay
                    Log.w(TAG, "Connection closed/failed: " + e.getMessage(), e);
                } finally {
                    if ( clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (Exception e) {
                            // Do nothing
                        }
                    }
                    acceptConnectionListener.connectionClosed();
                }
            }
        }).start();
    }

    private void stopListeningForConnections() {
        if ( serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverSocket = null;
            }
        }
        Log.i(TAG, "Not listening for connections.");
    }

}
