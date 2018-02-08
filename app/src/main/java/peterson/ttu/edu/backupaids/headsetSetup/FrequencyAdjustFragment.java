package peterson.ttu.edu.backupaids.headsetSetup;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;

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
    private short mBandAdjustment;

    private OnFragmentInteractionListener mListener;
    private AudioTrack mAudioTrack;
    private Equalizer mEqualizer;

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
    public static FrequencyAdjustFragment newInstance(int bandFrequency, short bandAdjustment) {
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
            mBandAdjustment = getArguments().getShort(ARG_BAND_ADJUSTMENT);
        }
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
        switch ( view.getId()) {
            case R.id.frequencyAdjustPlaySounds:
                if ( mAudioTrack == null) {
                    startPlaying();
                } else {
                    stopPlaying();
                }
                break;
            default:
        }
    }

    private void startPlaying() {
        if ( mAudioTrack != null)
        {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

        mAudioTrack = new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        InputStream inputStream = getResources().openRawResource(Util.FREQUENCY_SOUND_MAP.get(mBandFrequency));
        int amountRead = 0;
        int totalRead = 0;
        int bufferSize = 2048;
        byte[] buffer = new byte[bufferSize];
        try {
            while ( (amountRead = inputStream.read(buffer, 0, bufferSize)) >= 0) {
                totalRead += amountRead;
                mAudioTrack.write(buffer, 0, amountRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mAudioTrack.play();

        // Play this forever
        mAudioTrack.setLoopPoints(0, amountRead, -1);

        mEqualizer = new Equalizer(1, mAudioTrack.getAudioSessionId());
        short band = mEqualizer.getBand(mBandFrequency);
        mEqualizer.setBandLevel(band, mBandAdjustment);
        mEqualizer.setEnabled(true);
    }

    private void stopPlaying() {
        if ( mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if ( mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
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
