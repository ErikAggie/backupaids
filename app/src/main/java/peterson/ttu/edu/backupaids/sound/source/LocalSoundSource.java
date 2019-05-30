package peterson.ttu.edu.backupaids.sound.source;

import android.media.AudioRecord;

import peterson.ttu.edu.backupaids.util.Util;

/**
 * Sound coming from a local source (phone mics or connected headset)
 */
public class LocalSoundSource implements SoundSource {

    private static final String TAG = "LocalSoundSource";

    private final AudioRecord audioRecord;

    private boolean firstRead = true;

    int sleeptime = 0;
    long lastReadTime = 0;

    public LocalSoundSource(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
    }

    @Override
    public byte[] getByteBuffer() {
        return new byte[Util.getLocalMinBufferSize()];
    }

    @Override
    public void record() {
        audioRecord.startRecording();
    }

    @Override
    public int read(byte[] buffer) {
        if ( firstRead) {
            // What we're trying to do here is establish the amount of time it takes to get data
            firstRead = false;
            // Dump everything in the first read so we have as little latency as possible
            audioRecord.read(buffer, 0, buffer.length);
        }
        return audioRecord.read(buffer, 0, buffer.length / 2);
    }

    @Override
    public void stop() {
        audioRecord.stop();
        audioRecord.release();
    }
}
