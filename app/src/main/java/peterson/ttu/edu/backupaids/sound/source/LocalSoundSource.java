package peterson.ttu.edu.backupaids.sound.source;

import android.media.AudioRecord;
import android.util.Log;

/**
 * Sound coming from a local source (phone mics or connected headset)
 */
public class LocalSoundSource implements SoundSource {

    private final AudioRecord audioRecord;

    private boolean firstRead = true;

    public LocalSoundSource(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
    }

    @Override
    public void record() {
        audioRecord.startRecording();
    }

    @Override
    public int read(byte[] buffer) {
        if ( firstRead) {
            // Dump everything in the first read so we have as little latency as possible
            audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
            firstRead = false;
        }
        int amountRead = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
        return amountRead;
    }

    @Override
    public void stop() {
        audioRecord.stop();
        audioRecord.release();
    }
}
