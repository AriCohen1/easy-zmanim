package com.aricohen.easyzmanim;

public abstract class LocationItem {
    public abstract boolean isHeader();

    public static final class Header extends LocationItem {
        public final String title;
        public Header(String title) { this.title = title; }
        @Override public boolean isHeader() { return true; }
    }

    public static final class City extends LocationItem {
        public final String name;
        public final Double lat; // null => current GPS
        public final Double lon;
        public final String group;
        public final String timeZoneId; // NEW: Holds "America/New_York", etc.

        // Updated constructor to accept timeZoneId
        public City(String group, String name, Double lat, Double lon, String timeZoneId) {
            this.group = group;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.timeZoneId = timeZoneId;
        }

        public boolean isCurrent() { return lat == null || lon == null; }

        @Override public boolean isHeader() { return false; }
    }
}