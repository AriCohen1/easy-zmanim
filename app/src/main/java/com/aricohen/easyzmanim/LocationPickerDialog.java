package com.aricohen.easyzmanim;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationPickerDialog extends DialogFragment {

    public interface Listener {
        void onLocationPicked(LocationItem.City city);
    }

    private static final String ARG_IGNORE = "x"; // placeholder; no heavy args

    private Listener listener;
    private final List<LocationItem> fullList = new ArrayList<>();
    private final List<LocationItem> filteredList = new ArrayList<>();

    public static LocationPickerDialog newInstance() {
        LocationPickerDialog dlg = new LocationPickerDialog();
        Bundle b = new Bundle();
        b.putString(ARG_IGNORE, "1");
        dlg.setArguments(b);
        return dlg;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<LocationItem> items) {
        fullList.clear();
        fullList.addAll(items);
        rebuildFilter("");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle("Choose location");

        android.view.View v = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_location_picker, null);

        SearchView searchView = v.findViewById(R.id.searchView);
        RecyclerView rv = v.findViewById(R.id.locationRecycler);

        LocationPickerAdapter adapter = new LocationPickerAdapter(city -> {
            if (listener != null) listener.onLocationPicked(city);
            dismiss();
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Initial list
        adapter.setItems(filteredList);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                rebuildFilter(newText == null ? "" : newText);
                adapter.setItems(filteredList);
                return true;
            }
        });

        b.setView(v);
        b.setNegativeButton("Cancel", (d, which) -> dismiss());

        return b.create();
    }

    private void rebuildFilter(String query) {
        filteredList.clear();

        String q = query.trim().toLowerCase(java.util.Locale.ROOT);

        if (q.isEmpty()) {
            filteredList.addAll(fullList);
            return;
        }

        String currentGroup = null;
        List<LocationItem.City> currentCities = new ArrayList<>();

        // Helper: does query match a group name? (also allow some short aliases)
        java.util.function.Predicate<String> groupMatches = (groupTitle) -> {
            String g = groupTitle.toLowerCase(java.util.Locale.ROOT);
            if (g.contains(q)) return true;

            // handy aliases
            if (q.equals("uk") && g.contains("united kingdom")) return true;
            if (q.equals("usa") && g.contains("usa")) return true;
            if ((q.equals("us") || q.equals("u.s.")) && g.contains("usa")) return true;

            return false;
        };

        // Walk the full list, buffering one group at a time
        for (int i = 0; i < fullList.size(); i++) {
            LocationItem item = fullList.get(i);

            if (item instanceof LocationItem.Header) {
                // flush previous group
                if (currentGroup != null) {
                    flushGroup(q, currentGroup, currentCities, groupMatches);
                }

                // start new group
                currentGroup = ((LocationItem.Header) item).title;
                currentCities = new ArrayList<>();
                continue;
            }

            // city
            currentCities.add((LocationItem.City) item);
        }

        // flush last group
        if (currentGroup != null) {
            flushGroup(q, currentGroup, currentCities, groupMatches);
        }
    }

    private void flushGroup(
            String q,
            String groupTitle,
            List<LocationItem.City> cities,
            java.util.function.Predicate<String> groupMatches
    ) {
        // If query matches the group title, show entire group
        if (groupMatches.test(groupTitle)) {
            filteredList.add(new LocationItem.Header(groupTitle));
            filteredList.addAll(cities);
            return;
        }

        // Otherwise: show only matching cities, with header if any match
        boolean headerAdded = false;
        for (LocationItem.City city : cities) {
            if (city.name.toLowerCase(java.util.Locale.ROOT).contains(q)) {
                if (!headerAdded) {
                    filteredList.add(new LocationItem.Header(groupTitle));
                    headerAdded = true;
                }
                filteredList.add(city);
            }
        }
    }

}