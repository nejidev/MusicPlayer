package com.nejidev.media;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MediaDirAdapter extends ArrayAdapter<MediaItem> {
    private int resourceId;

    public MediaDirAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<MediaItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MediaItem mediaDir = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId, parent,false);
        TextView textViewName = view.findViewById(R.id.textView);

        textViewName.setText(mediaDir.getName());

        return view;
    }
}
