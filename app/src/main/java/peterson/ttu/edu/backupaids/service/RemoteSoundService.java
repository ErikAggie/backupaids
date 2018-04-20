package peterson.ttu.edu.backupaids.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;
import peterson.ttu.edu.backupaids.network.ConnectionManager;
import peterson.ttu.edu.backupaids.sound.destination.DestinationFactory;
import peterson.ttu.edu.backupaids.sound.destination.SoundDestination;
import peterson.ttu.edu.backupaids.sound.source.SoundSource;
import peterson.ttu.edu.backupaids.sound.source.SourceFactory;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RemoteSoundService extends BaseStreamService {

    private static final String TAG = "RemoteSoundService";

    private static final int FOREGROUND_ID = 1236;
    private static final String STREAM_CHANNEL_NAME = "Stream In";

    private static boolean currentlyStreaming = false;
    private static final List<StreamSoundService.Listener> listeners = new ArrayList<>();
    private static String preset;

    public static String getCurrentPreset() {
        return preset;
    }

    public static void setCurrentPreset(String newPreset) {
        preset = newPreset;
    }


    public static boolean isCurrentlyStreaming() {
        return currentlyStreaming;
    }

    public static void registerListener(StreamSoundService.Listener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(StreamSoundService.Listener listener) {
        listeners.remove(listener);
    }



    public RemoteSoundService() {
        super("RemoteSoundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        setUpService(intent, STREAM_CHANNEL_NAME, FOREGROUND_ID);
    }

    @Override
    protected void streamingStarted() {
        currentlyStreaming = true;
    }

    @Override
    protected void streamingStopped() {
        currentlyStreaming = false;
    }

    @Override
    protected List<Listener> getListeners() {
        return listeners;
    }

    @Override
    protected SoundSource createSource(Socket socket) throws IOException {
        return SourceFactory.createStreamSource(socket);
    }

    @Override
    protected SoundDestination createDestination(Socket socket) throws IOException {
        SoundPreset soundPreset = null;
        if ( preset != null && !preset.isEmpty()) {
            soundPreset = SoundPresetManager.getInstance(this).getPreset(preset);
        }

        return DestinationFactory.createLocalAudioDestination(soundPreset);
    }
}
