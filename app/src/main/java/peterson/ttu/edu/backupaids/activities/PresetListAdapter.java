package peterson.ttu.edu.backupaids.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.model.Preferences;
import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Inspired by https://stackoverflow.com/questions/8166497/custom-adapter-for-list-view
 */
public class PresetListAdapter extends ArrayAdapter<SoundPreset> {

    private final ButtonListener buttonListener;
    private final PopupWindow popupWindow;
    private final int resource;

    public PresetListAdapter(@NonNull PopupWindow popupWindow, @NonNull ButtonListener buttonListener, @NonNull Context context, int resource, @NonNull List<SoundPreset> objects) {
        super(context, resource, objects);
        this.buttonListener = buttonListener;
        this.popupWindow = popupWindow;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if ( view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate( resource, null);
        }

        final SoundPreset soundPreset = getItem(position);

        TextView nameView = view.findViewById(R.id.presetPopupItemName);
        nameView.setText(soundPreset.getName());
        nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preferences.getInstance(getContext().getApplicationContext()).setSelectedPreset(soundPreset.getName());
                popupWindow.dismiss();
            }
        });

        ImageButton deleteButton = view.findViewById(R.id.presetPopupDeleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonListener.deletePresetButtonPushed(popupWindow, soundPreset);
            }
        });

        return view;
    }

    public interface ButtonListener {
        void deletePresetButtonPushed(PopupWindow popupWindow, SoundPreset soundPreset);
    }
}
