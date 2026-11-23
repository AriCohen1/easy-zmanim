package com.aricohen.easyzmanim;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 1001;

    private Button locationButton;
    private Button dateButton;
    private TextView subtitleText;
    private RecyclerView recyclerView;

    private ZmanimAdapter zmanimAdapter;

    private FusedLocationProviderClient fused;

    private LocalDate selectedDate;
    private LocationItem.City selectedCity;

    private final List<LocationItem> locationItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationButton = findViewById(R.id.locationButton);
        dateButton = findViewById(R.id.dateButton);
        subtitleText = findViewById(R.id.subtitleText);
        recyclerView = findViewById(R.id.zmanimRecycler);

        zmanimAdapter = new ZmanimAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(zmanimAdapter);

        fused = LocationServices.getFusedLocationProviderClient(this);

        ZoneId zoneId = TimeZone.getDefault().toZoneId();
        selectedDate = LocalDate.now(zoneId);

        // default: current location (GPS) - no specific timezone override (null)
        selectedCity = new LocationItem.City("GPS", "Current location (GPS)", null, null, null);

        buildLocationItems(); // grouped list

        updateDateButtonText();
        updateLocationButtonText();

        dateButton.setOnClickListener(v -> openDatePicker());
        locationButton.setOnClickListener(v -> openLocationPicker());

        refreshZmanim();
    }

    // -------------------------
    // Location picker
    // -------------------------

    private void buildLocationItems() {
        locationItems.clear();

        // Always keep Current location (GPS) pinned at top
        locationItems.add(new LocationItem.Header("Current"));
        locationItems.add(new LocationItem.City("Current", "Current location (GPS)", null, null, null));

        try {
            String json = readRawTextFile(R.raw.locations);
            JSONObject root = new JSONObject(json);

            JSONArray groups = root.getJSONArray("groups");
            for (int i = 0; i < groups.length(); i++) {
                JSONObject g = groups.getJSONObject(i);
                String title = g.getString("title");
                locationItems.add(new LocationItem.Header(title));

                JSONArray cities = g.getJSONArray("cities");
                for (int j = 0; j < cities.length(); j++) {
                    JSONObject c = cities.getJSONObject(j);

                    String name = c.getString("name");
                    double lat = c.getDouble("lat");
                    double lon = c.getDouble("lon");

                    // Read TimeZone (default to null if missing, though our JSON has it)
                    String tz = c.optString("timeZone", null);

                    locationItems.add(new LocationItem.City(title, name, lat, lon, tz));
                }
            }
        } catch (Exception e) {
            // If JSON fails for any reason, show something useful but don't crash
            locationItems.add(new LocationItem.Header("Error"));
            locationItems.add(new LocationItem.City("Error", "Failed to load locations: " + e.getMessage(), 51.5074, -0.1278, null));
            e.printStackTrace();
        }
    }

    private void openLocationPicker() {
        LocationPickerDialog dlg = LocationPickerDialog.newInstance();
        dlg.setListener(city -> {
            selectedCity = city;
            updateLocationButtonText();
            refreshZmanim();
        });
        dlg.setItems(locationItems);
        dlg.show(getSupportFragmentManager(), "locationPicker");
    }

    private void updateLocationButtonText() {
        locationButton.setText("Location: " + (selectedCity == null ? "?" : selectedCity.name));
    }

    // -------------------------
    // Date picker
    // -------------------------

    private void updateDateButtonText() {
        dateButton.setText("Date: " + selectedDate);
    }

    private void openDatePicker() {
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDateButtonText();
                    refreshZmanim();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );
        dlg.show();
    }

    // -------------------------
    // Zmanim rendering
    // -------------------------

    private void refreshZmanim() {
        if (selectedCity == null || selectedCity.isCurrent()) {
            ensureLocationThenShowZmanim();
        } else {
            showZmanimForLatLon(selectedCity.name, selectedCity.lat, selectedCity.lon);
        }
    }

    private void ensureLocationThenShowZmanim() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION
            );
            subtitleText.setText("Location permission needed for GPS mode.");
            zmanimAdapter.setItems(new ArrayList<>(), -1);
            return;
        }

        subtitleText.setText("Getting current location...");

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location == null) {
                        subtitleText.setText("Could not get location. Make sure GPS is on.");
                        zmanimAdapter.setItems(new ArrayList<>(), -1);
                        return;
                    }
                    showZmanimForLatLon("Current location", location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(this, e -> {
                    subtitleText.setText("Location error: " + e.getMessage());
                    zmanimAdapter.setItems(new ArrayList<>(), -1);
                });
    }

    private void showZmanimForLatLon(String locName, double lat, double lon) {
        // 1. Determine the correct TimeZone
        TimeZone tz;
        if (selectedCity != null && selectedCity.timeZoneId != null) {
            // Use the specific city timezone from JSON (e.g. "America/New_York")
            tz = TimeZone.getTimeZone(selectedCity.timeZoneId);
        } else {
            // Use device default (for GPS or if ID is missing)
            tz = TimeZone.getDefault();
        }

        ZoneId zoneId = tz.toZoneId();

        // 2. Compute Zmanim using that specific timezone
        // The service will now correctly calculate sunrise/sunset for that zone's offsets
        Map<String, Date> zmanim = ZmanimService.computeForDate(lat, lon, tz, selectedDate);

        // 3. Format Strings for Display
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter zoneNameFmt = DateTimeFormatter.ofPattern("z"); // e.g. "EST", "GMT", "IDT"

        // Update Subtitle: Show user the timezone we are using.
        // Use midday on the selected date (not "now") so the label matches that date's
        // daylight-saving state, e.g. BST for a summer date even when viewed in winter.
        String tzDisplayName = selectedDate.atTime(12, 0).atZone(zoneId).format(zoneNameFmt);
        subtitleText.setText(locName + " • " + selectedDate + " (" + tzDisplayName + ")");

        // 4. Highlight Logic
        // We compare the 'now' epoch millis vs the Zman epoch millis.
        // Time flows universally, so comparing millis works regardless of zones.
        int highlightIndex = -1;
        long nowMillis = System.currentTimeMillis();
        boolean isToday = selectedDate.equals(LocalDate.now(zoneId));

        List<ZmanRow> rows = new ArrayList<>();
        List<Date> datesInOrder = new ArrayList<>();

        for (Map.Entry<String, Date> entry : zmanim.entrySet()) {
            Date d = entry.getValue();
            datesInOrder.add(d);

            String timeStr;
            if (d == null) {
                timeStr = "—";
            } else {
                // Convert the universal instant to the TARGET timezone for display
                // If it's 14:00 London (UTC), this will print 09:00 for New York (EST)
                timeStr = ZonedDateTime.ofInstant(d.toInstant(), zoneId).format(timeFmt);
            }

            rows.add(new ZmanRow(entry.getKey(), timeStr));
        }

        if (isToday) {
            long bestDeltaMs = Long.MAX_VALUE;
            for (int i = 0; i < datesInOrder.size(); i++) {
                Date d = datesInOrder.get(i);
                if (d == null) continue;

                // Compare universal time
                long delta = d.getTime() - nowMillis;

                if (delta >= 0 && delta < bestDeltaMs) {
                    bestDeltaMs = delta;
                    highlightIndex = i;
                }
            }
        }

        zmanimAdapter.setItems(rows, highlightIndex);

        if (highlightIndex >= 0) recyclerView.scrollToPosition(highlightIndex);
    }

    private String readRawTextFile(int resId) throws Exception {
        InputStream is = getResources().openRawResource(resId);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        return sb.toString();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            refreshZmanim();
        } else {
            // Don't call refreshZmanim() here: it would request the permission again, and once
            // the user picks "Don't ask again" Android denies instantly, causing an endless loop.
            subtitleText.setText("Location permission denied. Choose a city from the location list instead.");
            zmanimAdapter.setItems(new ArrayList<>(), -1);
        }
    }
}