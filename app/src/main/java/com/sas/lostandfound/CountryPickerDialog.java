package com.sas.lostandfound;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CountryPickerDialog {

    public interface OnCountrySelectedListener {
        void onCountrySelected(Country country);
    }

    public static void show(Context context, OnCountrySelectedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_country_picker, null);
        EditText etSearch = dialogView.findViewById(R.id.etSearch);
        ListView listView = dialogView.findViewById(R.id.listViewCountries);

        List<Country> allCountries = ValidationUtils.getCountries();
        List<Country> filteredList = new ArrayList<>(allCountries);

        CountryAdapter adapter = new CountryAdapter(context, filteredList);
        listView.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(dialogView);

        // Make the bottom sheet background transparent to respect the rounded corners
        // of dialogView
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior
                    .from(bottomSheet);
            behavior.setDraggable(false);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase(Locale.getDefault()).trim();
                filteredList.clear();
                for (Country c : allCountries) {
                    if (c.getName().toLowerCase(Locale.getDefault()).contains(query) ||
                            c.getCode().toLowerCase(Locale.getDefault()).contains(query) ||
                            c.getShortCode().toLowerCase(Locale.getDefault()).contains(query)) {
                        filteredList.add(c);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Country selected = filteredList.get(position);
            listener.onCountrySelected(selected);
            dialog.dismiss();
        });

        dialog.show();
    }

    private static class CountryAdapter extends BaseAdapter {
        private final Context context;
        private final List<Country> countries;

        public CountryAdapter(Context context, List<Country> countries) {
            this.context = context;
            this.countries = countries;
        }

        @Override
        public int getCount() {
            return countries.size();
        }

        @Override
        public Object getItem(int position) {
            return countries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_country, parent, false);
            }
            Country country = countries.get(position);
            TextView tvFlag = convertView.findViewById(R.id.tvFlag);
            TextView tvName = convertView.findViewById(R.id.tvCountryName);
            TextView tvCode = convertView.findViewById(R.id.tvCountryCode);

            tvFlag.setText(country.getFlagEmoji());
            tvName.setText(country.getName());
            tvCode.setText(country.getCode());

            return convertView;
        }
    }
}
