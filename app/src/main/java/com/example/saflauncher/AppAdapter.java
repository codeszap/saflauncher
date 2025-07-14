package com.example.saflauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppAdapter extends BaseAdapter {

    private Context context;
    private List<AppDetail> apps;

    public AppAdapter(Context context, List<AppDetail> apps) {
        this.context = context;
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_icon, parent, false);

        ImageView appIcon = view.findViewById(R.id.app_icon);
        TextView appLabel = view.findViewById(R.id.app_label);

        appIcon.setImageDrawable(apps.get(position).icon);
        appLabel.setText(apps.get(position).label);

        return view;
    }
}
