package peterson.ttu.edu.backupaids.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

import peterson.ttu.edu.backupaids.model.DeviceInfo;
import peterson.ttu.edu.backupaids.network.ConnectionMaker;
import peterson.ttu.edu.backupaids.network.ConnectionMakerFactory;
import peterson.ttu.edu.backupaids.service.StreamSoundService;
import peterson.ttu.edu.backupaids.util.ConnectionType;

public class SpeakConnectionController extends ConnectionController implements StreamSoundService.Listener {

    private DeviceInfo deviceInfo;

    public SpeakConnectionController(Context context, Activity activity, Listener listener) throws IOException {
        super(context, activity, listener);
    }

    public SpeakConnectionController(Context context, Activity activity, Listener listener, DeviceInfo deviceInfo) throws IOException {
        super(context, activity, listener);
        this.deviceInfo = deviceInfo;
    }

    @Override
    protected ConnectionMaker getConnectionMaker() throws IOException {
        if ( StreamSoundService.isCurrentlyStreaming()) {
            // We're already connected; so just hook up with what's there
            ConnectionMaker connectionMaker = StreamSoundService.getConnectionMaker();
            connectionMaker.changeConnectionListener(this);
            updateState(State.STREAMING);
            return connectionMaker;
        } else if ( deviceInfo != null) {
            updateState(State.CONNECTING);
            return ConnectionMakerFactory.createConnectionMakerExistingConnection(context, this, ConnectionType.SPEAK, deviceInfo);
        } else {
            return ConnectionMakerFactory.createConnectionMaker(context, this, ConnectionType.SPEAK);
        }
    }

    @Override
    protected void startService() {
        StreamSoundService.registerListener(this);
        Intent startIntent = new Intent(context, StreamSoundService.class);
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
