package peterson.ttu.edu.backupaids.headsetSetup;

import android.database.ContentObserver;
import android.os.Handler;

/**
 * Adapted from https://stackoverflow.com/questions/6896746/is-there-a-broadcast-action-for-volume-changes
 */

public class AudioSettingObserver extends ContentObserver {
    private IListenToVolumeChange mVolumeChangeListener;

    public AudioSettingObserver(Handler handler, IListenToVolumeChange volumeChangeListener) {
        super(handler);
        mVolumeChangeListener = volumeChangeListener;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
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
