package peterson.ttu.edu.backupaids.activities.headsetSetup;

import android.database.ContentObserver;
import android.os.Handler;

/**
 * Adapted from https://stackoverflow.com/questions/6896746/is-there-a-broadcast-action-for-volume-changes
 */

class AudioSettingObserver extends ContentObserver {
    private final IListenToVolumeChange mVolumeChangeListener;

    public AudioSettingObserver(Handler handler, IListenToVolumeChange volumeChangeListener) {
        super(handler);
        mVolumeChangeListener = volumeChangeListener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        mVolumeChangeListener.volumeChanged();

    }

    /**
     * Listener to a volume change event
     */
    public interface IListenToVolumeChange {
        void volumeChanged();
    }

}
