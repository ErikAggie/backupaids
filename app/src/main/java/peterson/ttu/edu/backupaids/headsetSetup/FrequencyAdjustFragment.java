package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.HashMap;

import peterson.ttu.edu.backupaids.R;

import static android.content.Context.AUDIO_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FrequencyAdjustFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FrequencyAdjustFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrequencyAdjustFragment extends DialogFragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_BAND_FREQUENCY = "argBandFrequency";
    private static final String ARG_BAND_ADJUSTMENT = "argBandAdjustment";
    private static final String TITLE_BASE = "Adjust Frequency: ";

    private int mBandFrequency;
    private int mBandAdjustment;

    private OnFragmentInteractionListener mListener;
    private SoundPool mSoundPool;
    private int mSoundId;
    private boolean mSoundsLoaded;

    private static final HashMap<Integer, Integer> FREQUENCY_SOUND_MAP = new HashMap<>();

    static {
        FREQUENCY_SOUND_MAP.put(125, R.raw.hz125);
        FREQUENCY_SOUND_MAP.put(250, R.raw.hz250);
        FREQUENCY_SOUND_MAP.put(500, R.raw.hz500);
        FREQUENCY_SOUND_MAP.put(1000, R.raw.hz1000);
        FREQUENCY_SOUND_MAP.put(2000, R.raw.hz2000);
        FREQUENCY_SOUND_MAP.put(4000, R.raw.hz4000);
        FREQUENCY_SOUND_MAP.put(8000, R.raw.hz8000);
    }

    public FrequencyAdjustFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bandFrequency Frequency we're adjusting
     * @param bandAdjustment Initial adjustment (default is 0)
     * @return A new instance of fragment FrequencyAdjustFragment.
     */
    public static FrequencyAdjustFragment newInstance(int bandFrequency, int bandAdjustment) {
        FrequencyAdjustFragment fragment = new FrequencyAdjustFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BAND_FREQUENCY, bandFrequency);
        args.putInt(ARG_BAND_ADJUSTMENT, bandAdjustment);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBandFrequency = getArguments().getInt(ARG_BAND_FREQUENCY);
            mBandAdjustment = getArguments().getInt(ARG_BAND_ADJUSTMENT);
        }
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
        mSoundId = mSoundPool.load(context,  FREQUENCY_SOUND_MAP.get(mBandFrequency), 1);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_frequency_adjust, container, false);

        updateTitleText(((TextView)fragmentView.findViewById(R.id.frequencyAdjustTitle)));


        // Set up our button's onClickEvents (why can't they target this automatically???
        ImageButton playSoundsButton = fragmentView.findViewById(R.id.frequencyAdjustPlaySounds);
        playSoundsButton.setOnClickListener(this);
        ImageButton adjustDownButton = fragmentView.findViewById(R.id.frequencyAdjustDown);
        adjustDownButton.setOnClickListener(this);
        ImageButton adjustUpButton = fragmentView.findViewById(R.id.frequencyAdjustUp);
        adjustUpButton.setOnClickListener(this);

        return fragmentView;
    }

    private void updateTitleText(TextView titleView) {
        titleView.setText(TITLE_BASE + mBandFrequency + ": " + mBandAdjustment);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        // TODO: fill in!
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
            mSoundPool.play(mSoundId, volume, volume, 1, 0, 1f);
        }

        final Equalizer equalizer = new Equalizer(1, audioManager.get);
        equalizer.setEnabled(true);
    }

    private void stopPlaying() {
        if ( mSoundsLoaded) {
            mSoundPool.stop(mSoundId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSoundPool.release();
    }

    private void playSounds() {
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
