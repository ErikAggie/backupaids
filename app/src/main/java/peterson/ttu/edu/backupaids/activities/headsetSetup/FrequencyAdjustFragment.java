package peterson.ttu.edu.backupaids.activities.headsetSetup;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.Util;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FrequencyAdjustFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FrequencyAdjustFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FrequencyAdjustFragment extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
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
        args.putShort(ARG_BAND_ADJUSTMENT, bandAdjustment);
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

        String frequencyAdjustTitle;

        if ( mBandFrequency >= 1000) {
            frequencyAdjustTitle = getString(R.string.adjust_frequency, mBandFrequency / 1000, "MHz");
        } else {
            frequencyAdjustTitle = getString(R.string.adjust_frequency, mBandFrequency, "Hz");
        }
        ((TextView)fragmentView.findViewById(R.id.frequencyAdjustTitle)).setText(frequencyAdjustTitle);


        // Set up our button's onClickEvents (why can't they target this automatically???
        ImageButton playSoundsButton = fragmentView.findViewById(R.id.frequencyAdjustPlaySounds);
        playSoundsButton.setOnClickListener(this);
        Button nextStepButton = fragmentView.findViewById(R.id.frequencyAdjustNextStep);
        nextStepButton.setOnClickListener(this);

        SeekBar adjustmentBar = fragmentView.findViewById(R.id.frequencyAdjustSlider);
        // Band adjustment is -1500...1500, but SeekBar is 0...3000
        adjustmentBar.setProgress(mBandAdjustment + 1500);
        adjustmentBar.setOnSeekBarChangeListener(this);

        return fragmentView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        startPlaying();
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
            case R.id.frequencyAdjustNextStep:
                stopPlaying();
                mListener.frequencyAdjustmentComplete(mBandAdjustment);
                break;
            default:
                throw new RuntimeException("Unexpected button push!");
        }
    }

    /**
     * SeekBar has changed something
     * @param seekBar Seekbar
     * @param i New value
     * @param b Not used
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        // Convert SeekBar range (0..3000) to Equalizer range (-1500..1500)
        mBandAdjustment = (short) (i-1500);
        if ( mEqualizer != null) {
            short band = mEqualizer.getBand(mBandFrequency * 1000);
            mEqualizer.setBandLevel(band, mBandAdjustment);
        }
    }

    private void startPlaying() {
        if ( mAudioTrack != null)
        {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

        byte[] tone = new byte[100000];
        int amountRead = 0;
        try
        {
            BufferedInputStream inputStream = new BufferedInputStream(getResources().openRawResource(Util.FREQUENCIES_TO_SOUND_IDS.get(mBandFrequency)));
            amountRead = inputStream.read(tone, 0, tone.length);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        mAudioTrack = new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Util.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(tone.length)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        mAudioTrack.write(tone, 0, amountRead);
        mAudioTrack.setPlaybackHeadPosition(100); // To avoid a click

        // Play this forever
        mAudioTrack.setLoopPoints(100, amountRead / 2, -1);
        mAudioTrack.play();

        mEqualizer = new Equalizer(1, mAudioTrack.getAudioSessionId());
        short band = mEqualizer.getBand(mBandFrequency*1000); // Millihertz to hertz
        mEqualizer.setBandLevel(band, mBandAdjustment);
        mEqualizer.setEnabled(true);

        //noinspection ConstantConditions
        ImageButton playSoundsButton = getView().findViewById(R.id.frequencyAdjustPlaySounds);
        playSoundsButton.setImageResource(R.drawable.ic_power_button_green);
    }

    private void stopPlaying() {
        if ( mAudioTrack != null) {
            if ( mEqualizer != null) {
                mEqualizer.release();
                mEqualizer = null;
            }
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }

        //noinspection ConstantConditions
        ImageButton playSoundsButton = getView().findViewById(R.id.frequencyAdjustPlaySounds);
        playSoundsButton.setImageResource(R.drawable.ic_power_button_blue);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if ( mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Don't care
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Don't care
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void frequencyAdjustmentComplete(short amount);
    }
}
