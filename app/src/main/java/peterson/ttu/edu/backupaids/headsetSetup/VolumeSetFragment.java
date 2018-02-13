package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;

import static android.content.Context.AUDIO_SERVICE;

/**
 * Fragment for setting volume.
 * Activities that contain this fragment must implement the
 * {@link VolumeSetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VolumeSetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VolumeSetFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_INITIAL_VOLUME = "initialVolume";

    private int mVolumeLevel;
    private boolean mPlaying = false;
    private AudioSettingObserver mVolumeObserver;

    private AudioTrack mAudioTrack;

    private OnFragmentInteractionListener mListener;

    public VolumeSetFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param volumeSetting The initial volume; <0 means to use the current headset volume
     * @return A new instance of fragment VolumeSetFragment.
     */
    public static VolumeSetFragment newInstance(int volumeSetting) {
        VolumeSetFragment fragment = new VolumeSetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_VOLUME, volumeSetting);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mVolumeLevel = getArguments().getInt(ARG_INITIAL_VOLUME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.volume_setting, container, false);

        // Set up our button's onClickEvents (why can't they target this automatically???
        ImageButton playSoundsButton = view.findViewById(R.id.volumeTestPlaySounds);
        playSoundsButton.setOnClickListener(this);
        ImageButton volumeDownButton = view.findViewById(R.id.volumeSettingsVolumeDown);
        volumeDownButton.setOnClickListener(this);
        ImageButton volumeUpButton = view.findViewById(R.id.volumeSettingsVolumeUp);
        volumeUpButton.setOnClickListener(this);
        Button nextStepButton = view.findViewById(R.id.volumeSettingNextStep);
        nextStepButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        updateVolumePercentage(true);
        startPlaying((ImageButton)getView().findViewById(R.id.volumeTestPlaySounds));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        mVolumeObserver = new AudioSettingObserver(new Handler(), new AudioSettingObserver.IListenToVolumeChange() {
            @Override
            public void volumeChanged() {
                updateVolumePercentage(false);
            }
        });
        getContext().getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mVolumeObserver);

    }

    /**
     * Update the volume (percentage)
     */
    private void updateVolumePercentage(boolean reset)
    {
        AudioManager audio = (AudioManager) this.getContext().getSystemService(AUDIO_SERVICE);

        // If this is a "reset" (i.e. this fragment was just created AND we have a valid
        // volume level) force that; otherwise grab the current volume
        if ( reset && mVolumeLevel >= 0) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeLevel, 0);
        } else {
            mVolumeLevel = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        // Put into a double to force floating-point division below
        double maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int percentage = (int)(mVolumeLevel / maxVolume * 100);
        String volumeInfo = getString(R.string.volume_with_value, percentage);
        TextView volumeText = getView().findViewById(R.id.volumeLabelWithAmount);
        volumeText.setText(volumeInfo);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        if ( mVolumeObserver != null) {
            getContext().getApplicationContext().getContentResolver().unregisterContentObserver(mVolumeObserver);
            mVolumeObserver = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if ( mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.volumeTestPlaySounds:
                if ( !mPlaying) {
                    startPlaying((ImageButton)view);
                }
                else {
                    stopPlaying((ImageButton)view);
                }
                break;
            case R.id.volumeSettingsVolumeUp:
                volumeUp();
                break;
            case R.id.volumeSettingsVolumeDown:
                volumeDown();
                break;
            case R.id.volumeSettingNextStep:
                mListener.volumeAdjustmentComplete(mVolumeLevel);
                break;
            default:
                throw new RuntimeException("Unexpected button push!");
                // Do nothing
        }
    }

    private void startPlaying(ImageButton playButton) {
        if ( mAudioTrack != null)
        {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

        byte[] tone = new byte[100000];
        try
        {
            BufferedInputStream inputStream = new BufferedInputStream(getResources().openRawResource(R.raw.all_freqs));
            inputStream.read(tone, 0, tone.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAudioTrack = new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(tone.length)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        mAudioTrack.write(tone, 0, tone.length);
        mAudioTrack.setPlaybackHeadPosition(100); // To avoid a click

        // Play this forever
        mAudioTrack.setLoopPoints(100, tone.length / 2, -1);
        mAudioTrack.play();

        mPlaying = true;
        playButton.setImageResource(R.drawable.power_button_green2);

    }

    private void stopPlaying(ImageButton playButton) {
        if ( mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }

        playButton.setImageResource(R.drawable.power_button_blue2);
        mPlaying = false;
    }

    private void volumeUp() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
    }

    private void volumeDown() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
    }

    public int getVolumeLevel() {
        return mVolumeLevel;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void volumeAdjustmentComplete(int volumeLevel);
    }
}
