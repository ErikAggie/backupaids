package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.SendSoundActivity;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * Service for streaming sound to another device
 */
public class StreamSoundService extends IntentService implements ConnectionManager.ConnectionListener {

    private static final String TAG = "StreamSoundService";

    private static final int FOREGROUND_ID = 1235;
    private static final String STREAM_CHANNEL_NAME = "Stream Out";

    private static boolean currentlyStreaming = false;
    private static final List<Listener> listeners = new ArrayList<>();

    private ConnectionManager connectionManager;

    public static boolean isCurrentlyStreaming() {
        return currentlyStreaming;
    }


    public static void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }


    public StreamSoundService() {
        super("StreamSoundService");
    }

    @Override
    public void onDestroy() {
        currentlyStreaming = false; // This will stop the thread
        connectionManager.close();
        connectionManager = null;

        super.onDestroy();
    }

    /**
     * Start looking for connections.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        if ( intent == null) {
            return;
        }

        final String connectionName = intent.getStringExtra("ConnectionName");

        if ( connectionName == null || connectionName.isEmpty()) {
            throw new RuntimeException("Cannot start StreamSoundService without a connection name!");
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SendSoundActivity.class), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(STREAM_CHANNEL_NAME, STREAM_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, STREAM_CHANNEL_NAME)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_stream_out)
                .setContentTitle("Streaming mic audio")
                .setContentText("Sending audio to another device")
                .setContentIntent(pendingIntent);

        startForeground(FOREGROUND_ID, notificationBuilder.build());

        ConnectionManager connectionManager = ConnectionManager.getInstance();
        if ( connectionManager == null) {
            throw new RuntimeException("Can't get Connection Manager (shouldn't be null)!");
        }
        connectionManager.setConnectionListener(this);

        connectionManager.makeConnection(connectionName);

        // Now we wait until ConnectionManager makes the connection
    }

    @Override
    public void connectionReady(Socket socket) throws IOException {
        for ( Listener listener : listeners) {
            listener.connectionMade();
        }


        SoundSource soundSource = null;
        SoundDestination soundDestination = null;

        try {
            soundSource = SourceFactory.createMicAudioRecord();
            soundDestination = DestinationFactory.createRemoteSoundDestination(socket);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.INPUT_MIN_BUFFER_SIZE];

            soundSource.record();
            soundDestination.play();

            // Here's the playing loop!
            while (currentlyStreaming) {
                int amountRead = soundSource.read(audioBuffer);
                if (amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
        } catch ( Exception e) {
            Log.w(TAG, "Stopping playback/streaming: " + e.getMessage());
        } finally {
            currentlyStreaming = false;

            // Clean up
            try {
                if ( soundSource != null) {
                    soundSource.stop();
                }
            } catch ( IOException e) {
                // Nothing to do
            }
            try {
                if ( soundDestination != null) {
                    soundDestination.stop();
                }
            } catch ( IOException e) {
                // Nothing to do
            }
        }
    }

    @Override
    public void connectionFailed(IOException e) {
        Log.w(TAG, "Connection failed: " + e.getMessage(), e);
        currentlyStreaming = false;
        for ( Listener listener : listeners) {
            listener.connectionFailed();
        }
    }

    @Override
    public void connectionClosed() {
        Log.i(TAG, "Streaming (out) connection closed.");
        currentlyStreaming = false;
        for ( Listener listener : listeners) {
            listener.connectionClosed();
        }
    }

    public interface Listener {
        void connectionMade();
        void connectionFailed();
        void connectionClosed();
    }
}
