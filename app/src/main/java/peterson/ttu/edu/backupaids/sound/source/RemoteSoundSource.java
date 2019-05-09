package peterson.ttu.edu.backupaids.sound.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import peterson.ttu.edu.backupaids.util.Util;

/**
 * Class for getting sound from a socket
 */
public class RemoteSoundSource implements SoundSource {

    private final InputStream inputStream;

    private boolean firstTime = true;

    public RemoteSoundSource(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
    }

    @Override
    public byte[] getByteBuffer() {
        // Make it 2 to add in some extra buffering
        return new byte[Util.getLocalMinBufferSize() *2];
    }

    @Override
    public void record() {
        // Nothing to do
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        if ( inputStream.available() > buffer.length) {
            // We've fallen behind, so throw everything away and start with the most current data
            //noinspection ResultOfMethodCallIgnored
            inputStream.skip(inputStream.available()-buffer.length);
        }

        return inputStream.read(buffer);
    }

    @Override
    public void stop() throws IOException {
        inputStream.close();
    }
}
