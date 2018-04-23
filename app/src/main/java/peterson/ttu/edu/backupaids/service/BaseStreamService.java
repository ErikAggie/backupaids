package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.SendSoundActivity;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

public abstract class BaseStreamService extends IntentService implements ConnectionManager.ConnectionListener {

    private static final String TAG = "BaseStreamService";

    private ConnectionManager connectionManager;

    private boolean thisServiceIsStreaming;

    public BaseStreamService(String name) {
        super(name);
    }

    @Override
    public void onDestroy() {
        stopStreaming();
        if ( connectionManager != null) {
            connectionManager.close();
            connectionManager = null;
        }
        super.onDestroy();
    }

    protected void setUpService(Intent intent, String channelName, int foregroundId) {

        if ( intent == null) {
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SendSoundActivity.class), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelName)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_stream_out)
                .setContentTitle("Streaming mic audio")
                .setContentText("Sending audio to another device")
                .setContentIntent(pendingIntent);

        startForeground(foregroundId, notificationBuilder.build());

        connectionManager = ConnectionManager.getInstance();
        if ( connectionManager == null) {
            throw new RuntimeException("Can't get Connection Manager (shouldn't be null)!");
        }
        connectionManager.setConnectionListener(this);

        final String connectionName = intent.getStringExtra("ConnectionName");
        if ( connectionName == null) {
            // Connection is waiting. This will call connectionReady() immediately
            connectionManager.readyForConnection();
        } else {
            // We need to initiate the connection
            connectionManager.makeConnection(connectionName);
        }
    }


    @Override
    public void connectionReady(Socket socket) throws IOException {
        thisServiceIsStreaming = true;
        streamingStarted();
        for ( BaseStreamService.Listener listener : getListeners()) {
            listener.connectionMade();
        }

        SoundSource soundSource = null;
        SoundDestination soundDestination = null;

        try {
            soundSource = createSource(socket);
            soundDestination = createDestination(socket);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.INPUT_MIN_BUFFER_SIZE];

            Log.i(TAG, "Ready to send!");

            soundSource.record();
            soundDestination.play();

            // Here's the playing loop!
            while (thisServiceIsStreaming) {
                int amountRead = soundSource.read(audioBuffer);
                if (amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
        } catch ( Exception e) {
            Log.w(TAG, "Stopping playback/streaming: " + e.getMessage());
        } finally {
            stopStreaming();

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

    private void stopStreaming() {
        if ( !thisServiceIsStreaming) {
            // Already stopped
            return;
        }

        thisServiceIsStreaming = false;
        streamingStopped();
        for ( BaseStreamService.Listener listener : getListeners()) {
            listener.connectionClosed();
        }
    }

    @Override
    public void connectionFailed(IOException e) {
        Log.w(TAG, "Connection failed: " + e.getMessage(), e);
        thisServiceIsStreaming = false;
        streamingStopped();
        for ( BaseStreamService.Listener listener : getListeners()) {
            listener.connectionFailed();
        }
    }

    @Override
    public void connectionClosed() {
        Log.i(TAG, "Streaming (out) connection closed.");
        thisServiceIsStreaming = false;
        streamingStopped();
        for ( BaseStreamService.Listener listener : getListeners()) {
            listener.connectionClosed();
        }
    }

    protected abstract void streamingStarted();

    protected abstract void streamingStopped();

    // Have to do this per-CLASS because these lists are static
    protected abstract List<Listener> getListeners();

    protected abstract SoundSource createSource(Socket socket) throws IOException;

    protected abstract SoundDestination createDestination(Socket socket) throws IOException;

    public interface Listener {
        void connectionMade();
        void connectionFailed();
        void connectionClosed();
    }

}
