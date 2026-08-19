package com.greenbuddy.acevosetupengineer.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public final class DarkSpinnerAdapter<T> extends ArrayAdapter<T> {
    public DarkSpinnerAdapter(Context context, List<T> items) {
        super(context, android.R.layout.simple_spinner_item, items);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        return color(super.getView(position, convertView, parent));
    }

    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        view.setBackgroundColor(Color.rgb(23, 23, 23));
        return color(view);
    }

    private static View color(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.WHITE);
            ((TextView) view).setTextSize(15f);
            view.setPadding(12, 8, 12, 8);
        }
        return view;
    }
}
