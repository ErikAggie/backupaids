package peterson.ttu.edu.backupaids.service;

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

import peterson.ttu.edu.backupaids.util.Util;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;

public abstract class BaseStreamService extends BaseService implements ConnectionMaker.ConnectionListener {

    private static final String TAG = "BaseStreamService";

    private ConnectionMaker connectionMaker;

    private boolean thisServiceIsStreaming;
    private boolean streamingStopped = false;

    private final int notificationIcon;
    private final String notificationTitle;
    private final String notificationContent;
    private final Class activityToInvoke;

    public BaseStreamService(String name,
                             int icon,
                             String notificationTitle,
                             String notificationContent,
                             Class activityToInvoke) {
        super(name);
        this.notificationIcon = icon;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.activityToInvoke = activityToInvoke;
    }

    @Override
    public void onDestroy() {
        unregisterThisService();
        stopStreaming();
        if ( connectionMaker != null) {
            connectionMaker.close();
            connectionMaker = null;
        }
        super.onDestroy();
    }

    protected void setUpService(Intent intent,
                                String channelName,
                                int foregroundId) {

        if ( intent == null) {
            return;
        }

        registerThisService();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, activityToInvoke), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelName)
                .setOngoing(true)
                .setSmallIcon(notificationIcon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setContentIntent(pendingIntent);

        startForeground(foregroundId, notificationBuilder.build());

        connectionMaker = ConnectionMaker.getInstance();
        if ( connectionMaker == null) {
            throw new RuntimeException("Can't get Connection Manager (shouldn't be null)!");
        }
        connectionMaker.setConnectionListener(this);

        final String connectionName = intent.getStringExtra(Util.CONNECTION_NAME_EXTRA);
        if ( connectionName == null) {
            // Connection is waiting. This will call connectionReady() immediately
            connectionMaker.readyForConnection();
        } else {
            // We need to initiate the connection
            connectionMaker.makeConnection(connectionName);
        }
    }


    @Override
    public void connectionReady(Socket socket) throws IOException {
        thisServiceIsStreaming = true;
        streamingStarted();
        for ( BaseStreamService.Listener listener : getListeners()) {
            listener.streamingStarted();
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
            // Clean up the source and destination (created in this method)
            // The other bits will be taken care of when ConnectionMaker calls streamingStopped
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
        stopStreaming(false);
    }

    private void stopStreaming(boolean failed) {
        if ( streamingStopped) {
            // Already stopped
            return;
        }

        thisServiceIsStreaming = false;
        streamingStopped = true;
        streamingStopped();
        for ( BaseStreamService.Listener listener : getListeners()) {
            if ( failed) {
                listener.streamingFailed();
            } else {
                listener.streamingStopped();
            }
        }
    }

    @Override
    public void connectionFailed(IOException e) {
        Log.w(TAG, "Connection failed: " + e.getMessage(), e);
        stopStreaming(true);
    }

    @Override
    public void connectionClosed() {
        Log.i(TAG, "Streaming connection closed.");
        stopStreaming();
    }

    protected abstract void streamingStarted();

    protected abstract void streamingStopped();

    // Have to do this per-CLASS because these lists are static
    protected abstract List<Listener> getListeners();

    protected abstract SoundSource createSource(Socket socket) throws IOException;

    protected abstract SoundDestination createDestination(Socket socket) throws IOException;

    public interface Listener {
        void streamingStarted();
        void streamingFailed();
        void streamingStopped();
    }

}
