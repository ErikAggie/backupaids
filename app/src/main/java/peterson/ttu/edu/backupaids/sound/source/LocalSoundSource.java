package peterson.ttu.edu.backupaids.sound.source;

import android.media.AudioRecord;

import peterson.ttu.edu.backupaids.util.Util;

/**
 * Sound coming from a local source (phone mics or connected headset)
 */
public class LocalSoundSource implements SoundSource {

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
            audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
            long startTime = System.nanoTime();
            while ( true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Okay, just check again
                }
                int amountRead = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
                if ( amountRead != 0) {
                    lastReadTime = System.nanoTime();
                    // Shave off any sub-millisecond data so we
                    sleeptime = (int) (Math.floor((lastReadTime - startTime) / 1000000));
                    return amountRead;
                }
            }
        }

        // Getting here means this isn't the first time to read. Use the saved time to sleep until
        // just before we should have data.
        // Account for any delay since the last read just in case a GC run happened or something
        int amountToSleep = (int) (sleeptime - (System.nanoTime() - lastReadTime));
        try {
            Thread.sleep(amountToSleep);
        } catch ( InterruptedException e) {
            // Okay, just start checking
        }

        // There shouldn't be any data in this first check because we intentionally woke up early
        // (whatever sub-millisecond leftover that got rounded down in the Math.floor above).
        // If there is data now there was a delay in that first reading. Back off a millisecond
        // for the next read.
        int amountRead = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
        if ( amountRead < 0) {
            return amountRead;
        } else if ( amountRead > 0) {
            sleeptime--;
            return amountRead;
        }

        // Getting here means that, as expected, we don't have any data yet. Repeatedly check until we get something
        while ( true) {
            amountRead = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
            if ( amountRead != 0) {
                lastReadTime = System.nanoTime();
                return amountRead;
            }
        }
    }

    @Override
    public void stop() {
        audioRecord.stop();
        audioRecord.release();
    }
}
