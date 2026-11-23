package com.aricohen.easyzmanim;

import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;


public final class ZmanimService {

    private ZmanimService() {
    }

    public static Map<String, Date> computeForDate(double lat, double lon, TimeZone tz, LocalDate date) {

        GeoLocation location = new GeoLocation("Here", lat, lon, 0, tz);
        ComplexZmanimCalendar czc = new ComplexZmanimCalendar(location);

        ZoneId zoneId = tz.toZoneId();
        ZonedDateTime startOfDay = date.atStartOfDay(zoneId);
        czc.getCalendar().setTime(Date.from(startOfDay.toInstant()));

        Map<String, Date> out = new LinkedHashMap<>();

        // Morning
        out.put("Alos (16.1°)", czc.getAlos16Point1Degrees());
        out.put("Misheyakir (10.2°)", czc.getMisheyakir10Point2Degrees());
        out.put("Sunrise (Netz)", czc.getSunrise());
        out.put("Sof Zman Shema (MGA 72 min / 16.1°)", czc.getSofZmanShmaMGA16Point1Degrees());
        out.put("Sof zman shema (Gra)", czc.getSofZmanShmaGRA());
        out.put("Sof zman tefilla (Gra)", czc.getSofZmanTfilaGRA());

        // Midday
        Date chatzos = czc.getChatzos();
        out.put("Chatzos", chatzos);

        // Mincha Gedola: two definitions
        Date sunrise = czc.getSunrise();
        Date sunset  = czc.getSunset();

        Date mgZmaniyos = null;
        if (chatzos != null && sunrise != null && sunset != null) {
            long daySeconds = (sunset.getTime() - sunrise.getTime()) / 1000L;
            long shaahZmanit = daySeconds / 12L;
            mgZmaniyos = Date.from(chatzos.toInstant().plusSeconds(shaahZmanit / 2L));
        }

        Date mgFixed30 = null;
        if (chatzos != null) {
            mgFixed30 = Date.from(chatzos.toInstant().plusSeconds(30L * 60L));
        }

        out.put("Mincha Gedolah (0.5 sha'ah zmanit)", mgZmaniyos);
        out.put("Mincha Gedolah (fixed +30 min)", mgFixed30);

        // Afternoon / night
        out.put("Plag Hamincha (GRA)", czc.getPlagHamincha());
        out.put("Sunset (Shkiah)", czc.getSunset());
        out.put("Tzais (Geonim 8.5°)", czc.getTzaisGeonim8Point5Degrees());

        return out;
    }



}

