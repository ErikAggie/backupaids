package peterson.ttu.edu.backupaids.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import peterson.ttu.edu.backupaids.R;
import peterson.ttu.edu.backupaids.model.SoundPreset;

/**
 * Inspired by https://stackoverflow.com/questions/8166497/custom-adapter-for-list-view
 */
public class PresetListAdapter extends ArrayAdapter<SoundPreset> {

    private final int resource;


    public PresetListAdapter(@NonNull Context context, int resource, @NonNull List<SoundPreset> objects) {
        super(context, resource, objects);
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if ( view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate( resource, null);
        }

        SoundPreset soundPreset = getItem(position);

        TextView nameView = view.findViewById(R.id.presetPopupItemName);
        nameView.setText(soundPreset.getName());

        return view;
    }
}
