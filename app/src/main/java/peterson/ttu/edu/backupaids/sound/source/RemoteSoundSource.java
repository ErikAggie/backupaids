package peterson.ttu.edu.backupaids.sound.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public void record() {
        // Nothing to do
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        if ( firstTime) {
            inputStream.read(buffer);
            firstTime = false;
        }
        if ( inputStream.available() > buffer.length) {
            // We've fallen behind, so throw everything away and start with the most current data
            //noinspection ResultOfMethodCallIgnored
            inputStream.skip(inputStream.available());
        }

        return inputStream.read(buffer);
    }

    @Override
    public void stop() throws IOException {
        inputStream.close();
    }
}
