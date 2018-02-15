package peterson.ttu.edu.backupaids;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.Spinner;

import java.io.IOException;
import java.util.List;

import peterson.ttu.edu.backupaids.headsetSetup.FrequencyAdjustFragment;
import peterson.ttu.edu.backupaids.headsetSetup.PresetSetupActivity;
import peterson.ttu.edu.backupaids.headsetSetup.VolumeSetFragment;
import peterson.ttu.edu.backupaids.model.SoundPresetManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private SoundPassthrough soundPassthrough;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request audio recording permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        setContentView(R.layout.activity_main);

        updateSpinner();
    }

    private void updateSpinner() {
        SoundPresetManager presetManager = SoundPresetManager.getInstance(getApplicationContext());
        String[] presetNames = presetManager.getSortedPresetNames();
        Spinner presetSpinner = findViewById(R.id.presetSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, presetNames);
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        presetSpinner.setAdapter(arrayAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionToRecordAccepted = false;
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) {
            finish();
            return;
        }

        // Getting here means permission is granted!
        setUpAudioRecordingAndPlayback();
    }


    private void setUpAudioRecordingAndPlayback()
    {
        soundPassthrough = new SoundPassthrough();
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }

    public void playSound(View view) {
        ImageButton playButton = findViewById(R.id.playSound);
        if ( !soundPassthrough.isPlaying())
        {
            // Start playing!
            try
            {
                soundPassthrough.start();
                playButton.setImageResource(R.drawable.power_button_green2);
            }
            catch(IOException e)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Error initializing app");
                builder.setMessage("Unable to set up audio recording/playback.");
                builder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                builder.create().show();
            }

        }
        else
        {
            soundPassthrough.stop();
            playButton.setImageResource(R.drawable.power_button_blue2);
        }
    }

    public void newPreset(View view) {
        // TODO: In edit we also need to send the current preset
        Intent intent = new Intent(this, PresetSetupActivity.class);
        startActivity(intent);
        /*FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // TODO: get this from the current preset (if any)
        VolumeSetFragment volumeSetFragment = VolumeSetFragment.newInstance(-1);
        fragmentTransaction.add(volumeSetFragment, VOLUME_SET_FRAGMENT_TAG);
        //FrequencyAdjustFragment frequencyAdjust = FrequencyAdjustFragment.newInstance(Util.TEST_FREQUENCIES[6], (short)0);
        //fragmentTransaction.add(frequencyAdjust, Util.FREQUENCY_FRAGMENT_NAMES[6]);
        fragmentTransaction.commit();*/
    }
}
