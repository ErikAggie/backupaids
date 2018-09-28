package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.service.BaseStreamService;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class SpeakConnectionController extends ConnectionController implements StreamSoundService.Listener {

    public SpeakConnectionController(Context context, Activity activity, Listener listener) {
        super(context, activity, listener);
    }

    @Override
    protected ConnectionMaker getConnectionMaker() {
        if ( StreamSoundService.isCurrentlyStreaming()) {
            // We're already connected; so just hook up with what's there
            ConnectionMaker connectionMaker = StreamSoundService.getConnectionMaker();
            connectionMaker.changeConnectionListener(this);
            updateState(State.STREAMING);
            return connectionMaker;
        } else {
            return new ConnectionMaker(context, this, ConnectionType.SPEAK);
        }
    }

    @Override
    public void startService(int connectionNumber) {
        StreamSoundService.registerListener(this);
        Intent startIntent = new Intent(context, StreamSoundService.class);
        startIntent.putExtra(BaseStreamService.CONNECTION_NUMBER_EXTRA, connectionNumber);
        activity.startService(startIntent);
    }

    @Override
    protected void stopService() {
        StreamSoundService.unregisterListener(this);
        activity.stopService(new Intent(context, StreamSoundService.class));
    }

    @Override
    public void streamingErrored(String error) {
        failed(error);
    }
}
