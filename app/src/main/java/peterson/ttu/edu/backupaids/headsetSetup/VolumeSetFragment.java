package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import peterson.ttu.edu.backupaids.R;

import static android.content.Context.AUDIO_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VolumeSetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VolumeSetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VolumeSetFragment extends DialogFragment implements View.OnClickListener{
    private static final String ARG_PRESET_NAME = "presetName";

    private String mPresetName;
    private boolean mPlaying = false;
    private AudioSettingObserver mVolumeObserver;

    private SoundPool mSoundPool;
    private int[] mSoundIds = new int[8];
    private boolean mSoundsLoaded;

    private OnFragmentInteractionListener mListener;

    public VolumeSetFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param presetName The name of the preference (if it's being modified; null/empty otherwise
     * @return A new instance of fragment VolumeSetFragment.
     */
    public static VolumeSetFragment newInstance(String presetName) {
        VolumeSetFragment fragment = new VolumeSetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_NAME, presetName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPresetName = getArguments().getString(ARG_PRESET_NAME);
        }
        setupSounds();
    }

    private void setupSounds() {
        mSoundPool = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                mSoundsLoaded = true;
            }
        });

        Context context = this.getContext();

        mSoundIds[0] = mSoundPool.load(context, R.raw.hz125, 1);
        mSoundIds[1] = mSoundPool.load(context, R.raw.hz250, 1);
        mSoundIds[2] = mSoundPool.load(context, R.raw.hz500, 1);
        mSoundIds[3] = mSoundPool.load(context, R.raw.hz1000, 1);
        mSoundIds[4] = mSoundPool.load(context, R.raw.hz2000, 1);
        mSoundIds[5] = mSoundPool.load(context, R.raw.hz3000, 1);
        mSoundIds[6] = mSoundPool.load(context, R.raw.hz4000, 1);
        mSoundIds[7] = mSoundPool.load(context, R.raw.hz8000, 1);
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

        return view;
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mPercentageTextView = (TextView) getView().findViewById(R.id.volumeText);
        updateVolumePercentage();
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
                updateVolumePercentage();
            }
        });
        getContext().getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mVolumeObserver);
    }

    /**
     * Update the volume (percentage)
     */
    private void updateVolumePercentage()
    {
        AudioManager audio = (AudioManager) this.getContext().getSystemService(AUDIO_SERVICE);
        double currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        double maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int percentage = (int)(currentVolume / maxVolume * 100);
        TextView volumeText = getView().findViewById(R.id.volumeText);
        volumeText.setText(percentage + "%");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        if ( mVolumeObserver != null)
        {
            getContext().getApplicationContext().getContentResolver().unregisterContentObserver(mVolumeObserver);
            mVolumeObserver = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSoundPool.release();
    }

    public void startStopPlaying(View view) {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.volumeTestPlaySounds:
                ImageButton playButton = view.findViewById(R.id.volumeTestPlaySounds);
                if ( !mPlaying) {
                    startPlaying();
                    playButton.setImageResource(R.drawable.power_button_green2);
                    mPlaying = true;
                }
                else {
                    stopPlaying();
                    playButton.setImageResource(R.drawable.power_button_blue2);
                    mPlaying = false;
                }
                break;
            case R.id.volumeSettingsVolumeUp:
                volumeUp();
                break;
            case R.id.volumeSettingsVolumeDown:
                volumeDown();
                break;
            default:
                // Do nothing
        }
    }

    private void startPlaying() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = actualVolume / maxVolume;
        // Is the sound loaded already?
        if (mSoundsLoaded) {
            for (int aSoundId : mSoundIds) {
                mSoundPool.play(aSoundId, volume, volume, 1, 0, 1f);
                Log.e("Test", "Played sound");
            }
        }
    }

    private void stopPlaying() {
        if ( mSoundsLoaded) {
            for ( int aSoundId : mSoundIds) {
                mSoundPool.stop(aSoundId);
            }
        }

    }

    private void volumeUp() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
        // Adjusting the volume kills our sound...so restart it
        /*if ( mPlaying) {
            startPlaying();
        }*/
    }

    private void volumeDown() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
        // Adjusting the volume kills our sound...so restart it
        /*if ( mPlaying) {
            startPlaying();
        }*/
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
