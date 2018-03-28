package peterson.ttu.edu.backupaids.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import peterson.ttu.edu.backupaids.BluetoothMonitor;
import peterson.ttu.edu.backupaids.Util;
import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Plays sounds from the phone's microphones into the headset
 * Created by Erik Peterson on 1/30/2018.
 */

/*public class SoundPassthrough {

    private static final String TAG = "SoundPassthrough";

    private Context context;
    private Runnable playRunnable;
    private Socket remoteConnection;
    private boolean playing = false;
    private boolean threadRunning = false;
    private Object notifyObject = new Object();
    private int bufferSize;

    public boolean isPlaying()
    {
        return playing;
    }

    public void start(SoundPreset preset) throws IOException
    {
        if ( playing)
        {
            stop();
        }

        playing = true;
        new Thread(new RunSingleInput(createHeadsetAudioRecord(), createAudioTrack(preset))).start();
    }

    public void stream(Socket socket) throws IOException {
        // We're already on a separate thread (to ensure that the socket is cleaned up correctly), so don't create another one
        threadRunning = true;
        playing = true;
        AudioRecord audioRecord = null;
        if (BluetoothMonitor.createIfNeeded(context).isHeadsetConnected()) {
            audioRecord = createBluetoothAudioRecord();
        } else {
            audioRecord = createHeadsetAudioRecord();
        }

        try {

            OutputStream outputStream = socket.getOutputStream();

            byte[] byteBuffer = new byte[bufferSize];
            audioRecord.startRecording();

            while ( playing) {
                int amountRead = audioRecord.read(byteBuffer, 0, byteBuffer.length);
                outputStream.write(byteBuffer, 0, amountRead);
                outputStream.flush();
            }

        } finally {
            threadRunning = false;

            // Clean up
            audioRecord.stop();
            audioRecord.release();
            synchronized (notifyObject) {
                threadRunning = false;
                notifyObject.notify();
            }
        }
    }

    public void playRemoteConnection(Socket socket, SoundPreset preset) throws IOException {
        this.remoteConnection = socket;

        byte[] dataRead = new byte[bufferSize];
        playing = true;

        BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
        final AudioTrack audioTrack = createAudioTrack(preset);
        audioTrack.play();
        // Calling stop will close the connection for us, so we need not worry about stopping
        // ourselves.
        try {
            while ( playing) {
                int amountRead = inputStream.read(dataRead);
                while ( inputStream.available() > dataRead.length * 2) {
                    // We've fallen behind
                    inputStream.read(dataRead);
                }
                if ( amountRead < 0) {
                    Log.w(TAG, "Error reading data, amount read is " + amountRead);
                    playing = false;
                    break;
                }
                audioTrack.write(dataRead, 0, amountRead);
            }
            // DO NOT CATCH IOExceptions. Allow them to bubble up so the connection is finished
        } finally {
            audioTrack.stop();
            audioTrack.release();
        }
    }

    private void stopPlayingRemoteConnection() {
        if ( remoteConnection != null) {
            try {
                remoteConnection.close();
            } catch ( Exception e) {
                // Do nothing
            } finally {
                remoteConnection = null;
            }
        }
    }

    public void stop()
    {
        stopPlayingRemoteConnection();
        playing = false;
        if ( playRunnable != null) {
            // This really shouldn't be done on the GUI thread, but it shouldn't take long
            // for the other thread to stop
            if ( threadRunning ) {
                synchronized (notifyObject) {
                    if ( threadRunning) {
                        try {
                            notifyObject.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // Stop the SCO session, if any
            if ( BluetoothMonitor.getInstance().isHeadsetConnected()) {

                // TODO: use a variable to determine if we STARTED using a Bluetooth device
            }
        }
    }

    private class RunSingleInput implements Runnable {

        private final AudioRecord audioRecord;
        private final AudioTrack audioTrack;

        public RunSingleInput(AudioRecord audioRecord, AudioTrack audioTrack) {
            this.audioRecord = audioRecord;
            this.audioTrack = audioTrack;
        }

        @Override
        public void run() {
            threadRunning = true;
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            short[] audioBuffer = new short[bufferSize / 2];

            audioTrack.play();

            audioRecord.startRecording();

            // Here's the playing loop!
            while ( playing)
            {
                int amountRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                audioTrack.write(audioBuffer, 0, amountRead);
            }

            // Clean up
            audioRecord.stop();
            audioRecord.release();
            audioTrack.stop();
            audioTrack.release();
            synchronized (notifyObject) {
                threadRunning = false;
                notifyObject.notify();
            }
        }
    }

}*/
