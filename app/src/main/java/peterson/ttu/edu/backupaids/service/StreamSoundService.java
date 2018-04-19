package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.activities.SendSoundActivity;
import peterson.ttu.edu.backupaids.network.ConnectionListener;
import peterson.ttu.edu.backupaids.network.ConnectionManager;

/**
 * Service for streaming sound to another device
 */
public class StreamSoundService extends IntentService implements ConnectionListener{

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
        connectionManager = new ConnectionManager(this, this);
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

    }

    @Override
    public void servicesStarted() {
        // Don't care?
    }

    @Override
    public void servicesStopped() {
        // We're the only ones who should be stopping anything, so ignore
    }

    @Override
    public void servicePublishingFailed() {
        // TODO: need a notification here...
    }

    @Override
    public void foundAPeer(String peerName) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SendSoundActivity.class), 0);

        if ( Build.VERSION.SDK_INT >= 26) {
            // Create the notification channel needed to show this notification...
            NotificationChannel channel = new NotificationChannel(Util.PEER_FOUND_CHANNEL_NAME, Util.PEER_FOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, STREAM_CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_stream_out)
                .setContentTitle("Make connection?")
                .setContentText("Connect to app with pin " + peerName + "?")
                .setContentIntent(pendingIntent);
    }

    @Override
    public void connectionReady(Socket socket) throws IOException {
        // TODO: stream...
    }

    @Override
    public void connectionFailed(IOException e) {
        // TODO: fill in...
    }

    @Override
    public void connectionClosed() {
        // TODO: fill in...
    }


    public interface Listener {
        void connectionMade();
        void connectionFailed();
        void connectionClosed();
    }
}
