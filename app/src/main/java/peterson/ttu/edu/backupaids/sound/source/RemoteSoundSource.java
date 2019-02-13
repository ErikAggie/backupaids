package peterson.ttu.edu.backupaids.sound.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for getting sound from a socket
 */
public class RemoteSoundSource implements SoundSource {

    private final InputStream inputStream;

    public RemoteSoundSource(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream);
    }

    @Override
    public void record() {
        // Nothing to do
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        while ( inputStream.available() > buffer.length * 2) {
            // We've fallen behind
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(buffer);
        }

        return inputStream.read(buffer);
    }

    @Override
    public void stop() throws IOException {
        inputStream.close();
    }
}
