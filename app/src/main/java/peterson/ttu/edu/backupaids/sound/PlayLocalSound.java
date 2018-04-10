package peterson.ttu.edu.backupaids.sound;

import java.io.IOException;

import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Convenience class for playing local sound on this headset
 */
public class PlayLocalSound extends BaseSound {

    public PlayLocalSound(SoundPreset preset) throws IOException {
        super(createCamcorderAudioRecord(), createLocalAudioDestination(preset));
    }
}
