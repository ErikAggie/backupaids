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

import peterson.ttu.edu.backupaids.util.Util;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;

public abstract class BaseStreamService extends BaseService implements ConnectionMaker.SocketHandler {

    private static final String TAG = "BaseStreamService";

    public static final String CONNECTION_NUMBER_EXTRA = "ConnectionNumber";

    protected ConnectionMaker connectionMaker;

    private boolean thisServiceIsStreaming;
    private boolean streamingStopped = false;

    private final int notificationIcon;
    private final String notificationTitle;
    private final String notificationContent;
    private final Class activityToInvoke;

    private String channelName;
    private int foregroundId;

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
        super.onDestroy();
    }

    protected void setUpService(Intent intent,
                                String channelName,
                                int foregroundId) {
        this.channelName = channelName;
        this.foregroundId = foregroundId;

        if (intent == null) {
            return;
        }

        int connectionNumber = intent.getIntExtra(CONNECTION_NUMBER_EXTRA, -1);
        if (connectionNumber < 0) {
            throw new RuntimeException("Must provide a connection number!");
        }
        connectionMaker = ConnectionMaker.getConnectionMaker(connectionNumber);
        if (connectionMaker == null) {
            throw new RuntimeException("Can't find connection maker for connection " + connectionNumber);
        }

        registerThisService();

        // Calling this will in turn call handleSocket()
        connectionMaker.readyForSocket(this);
    }

    @Override
    public void handleSocket(Socket socket) {

        thisServiceIsStreaming = true;

        SoundSource soundSource = null;
        SoundDestination soundDestination = null;

        try {
            soundSource = createSource(socket);
            soundDestination = createDestination(socket);
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            // Short buffer would be half of the buffer size; byte buffer is the full size
            byte[] audioBuffer = new byte[Util.INPUT_MIN_BUFFER_SIZE];

            Log.i(TAG, "Ready to send/receive!");

            soundSource.record();
            soundDestination.play();

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, activityToInvoke), 0);

            if (Build.VERSION.SDK_INT >= 26) {
                // Create the notification channel needed to show this notification...
                NotificationChannel channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_DEFAULT);
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

            streamingStarted();

            // Here's the playing loop!
            while (thisServiceIsStreaming) {
                if ( soundDestination.hasStopped()) {
                    // Not playing anymore, likely due to headphones being removed
                    connectionMaker.disallowRetry();
                    break;
                }
                int amountRead = soundSource.read(audioBuffer);
                if (amountRead < 0) {
                    break;
                }
                soundDestination.write(audioBuffer, amountRead);
            }
        } catch ( ServiceSetupException e) {
            Log.w(TAG, "Service setup error: " + e.getMessage());
            noteError(e.getMessage());
        } catch( IOException e) {
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

    protected void stopStreaming() {
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
    }

    protected abstract void streamingStarted();

    protected abstract void streamingStopped();

    protected abstract SoundSource createSource(Socket socket) throws IOException;

    protected abstract SoundDestination createDestination(Socket socket) throws IOException;

    protected abstract void noteError(String error);
}
