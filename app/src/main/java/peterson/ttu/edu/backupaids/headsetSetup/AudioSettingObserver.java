package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
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
}
