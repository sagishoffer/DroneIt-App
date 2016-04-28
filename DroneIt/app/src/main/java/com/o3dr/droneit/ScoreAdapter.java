package com.o3dr.droneit;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ScoreAdapter extends ArrayAdapter<Score> {

    public ScoreAdapter(Context context, ArrayList<Score> scores) {
        super(context, 0, scores);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Score score = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_high_score, parent, false);
        }
        // Lookup view for data population
        TextView place = (TextView) convertView.findViewById(R.id.place);
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);
        // Populate the data into the template view using the data object
        place.setText("#" + score.place);
        tvName.setText(score.name);
        tvTime.setText(score.time + "s");
        // Return the completed view to render on screen
        return convertView;
    }
}
