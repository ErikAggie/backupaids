package peterson.ttu.edu.backupaids.activities.headsetSetup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ProgressBar;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.model.SoundPreset;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;

public class PresetSetupActivity extends FragmentActivity implements VolumeSetFragment.OnFragmentInteractionListener, FrequencyAdjustFragment.OnFragmentInteractionListener {

    private enum SetupSteps {
        Start(-1), // This isn't a fragment, but a "-1" so that we can initialize to a non-existent fragment
        VolumeSet(0),
        Hz125(125),
        Hz250(250),
        Hz500(500),
        Hz1000(1000),
        Hz2000(2000),
        Hz3000(3000),
        Hz4000(4000),
        Hz8000(8000),
        End(-1); // Another non-value to note that we're at the end

        private final int frequencyIfAny;

        SetupSteps(int frequency) {
            frequencyIfAny = frequency;
        }

        private int getFrequency() { return frequencyIfAny;}

        private final static SetupSteps[] values = values();
        private SetupSteps next()
        {
            return values[(this.ordinal()+1) % values.length];
        }
    }

    private static final String TAG = "PresetSetupActivity";

    private SoundPreset soundPreset = new SoundPreset();
    private SetupSteps currentStep = SetupSteps.Start;
    private Fragment mCurrentFragment;

    public void setSoundPreset(SoundPreset soundPreset) {
        this.soundPreset = soundPreset;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: check for an "extra" value that shows the index of the preset to edit...
        setContentView(R.layout.activity_preset_setup);
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setMax(SetupSteps.values().length - 2);

        if ( savedInstanceState == null) {
            nextFragment();
        }
    }

    private void nextFragment() {
        if ( currentStep == SetupSteps.End || currentStep.next() == SetupSteps.End) {
            setupComplete();
            return;
        }
        currentStep = currentStep.next();
        ProgressBar progressBar = findViewById(R.id.presetSetupProgress);
        progressBar.setProgress(currentStep.ordinal());

        switch ( currentStep)
        {
            case Start:
                throw new RuntimeException("Shouldn't be on the START case...");
            case VolumeSet:
                showVolumeFragment();
                break;
            case Hz125:
            case Hz250:
            case Hz500:
            case Hz1000:
            case Hz2000:
            case Hz3000:
            case Hz4000:
            case Hz8000:
                showFrequencyFragment(currentStep);
                break;
            // End state is handled above
            default:
                throw new RuntimeException("Unexpected setup state " + currentStep);
        }
    }

    /**
     * Called when the setup is finished so we can save off the preset
     */
    private void setupComplete() {
        requestName();
    }

    private void requestName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString();
                if ( SoundPresetManager.getInstance(getApplicationContext()).presetExists(name)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PresetSetupActivity.this)
                            .setTitle("This preset exists.")
                            .setMessage("The preset already exists. Please choose a different name.")
                            .setPositiveButton("OK", null);
                    // When dismissed, just show the original popop again
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            requestName();
                        }
                    });
                    builder.show();
                    return;
                }
                if ( name.isEmpty()) {
                    // Need a name, people...
                    requestName();
                    return;
                }
                soundPreset.setName(input.getText().toString());
                presetComplete();
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });

        builder.show();
    }

    private void presetComplete() {
        SoundPresetManager.getInstance(getApplicationContext()).addOrReplacePreset(soundPreset);
        Preferences.getInstance(getApplicationContext()).setSelectedPreset(soundPreset.getName());
    }

    private void showVolumeFragment() {
        int volume = soundPreset.getVolumeAdjust();
        // If this is a new preset (volume = 0), don't set anything...the fragment
        // will use the current headset volume to start
        if ( volume == 0) {
            volume = -1;
        }
        showFragment(VolumeSetFragment.newInstance(volume));
    }

    public void volumeAdjustmentComplete(int volumeLevel) {
        soundPreset.setVolumeAdjust(volumeLevel);
        nextFragment();
    }
    
    private void showFrequencyFragment(SetupSteps step) {
        showFragment(
                FrequencyAdjustFragment.newInstance(step.getFrequency(),
                                                    soundPreset.getFrequencyAdjustment(step.getFrequency())));
    }

    public void frequencyAdjustmentComplete(short amount) {
        soundPreset.setFrequencyAdjustment(currentStep.getFrequency(), amount);
        nextFragment();
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if ( mCurrentFragment != null) {
            fragmentTransaction.remove(mCurrentFragment);
        }
        mCurrentFragment = fragment;
        fragmentTransaction.add(R.id.presetSetupFragmentLocation, mCurrentFragment);
        fragmentTransaction.commit();
    }
}
