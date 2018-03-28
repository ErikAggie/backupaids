package peterson.ttu.edu.backupaids.sound.source;

import java.io.IOException;

/**
 * Created by erika on 3/28/2018.
 */

public interface SoundSource {

    void record() throws IOException;
    int read(byte[] buffer) throws IOException;
    void stop() throws IOException;
}
