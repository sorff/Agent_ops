package utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility methods for formatting and working with dates and times.
 * Centralising date formatting here means you only have to change the format
 * in one place if you ever want to update how dates look throughout the app.
 */
public class DateUtils {

    // ── Format patterns ───────────────────────────────────────
    private static final SimpleDateFormat FMT_DATE_SHORT  = new SimpleDateFormat("MMM d, yyyy");
    private static final SimpleDateFormat FMT_DATE_LONG   = new SimpleDateFormat("MMMM d, yyyy");
    private static final SimpleDateFormat FMT_DATETIME    = new SimpleDateFormat("MMM d, yyyy · h:mm a");
    private static final SimpleDateFormat FMT_TIME_ONLY   = new SimpleDateFormat("h:mm a");

    /**
     * Formats a Timestamp as "May 9, 2026 · 3:45 PM"
     * Returns "—" if the timestamp is null.
     */
    public static String formatDateTime(Timestamp ts) {
        if (ts == null) return "—";
        return FMT_DATETIME.format(new Date(ts.getTime()));
    }

    /**
     * Formats a java.sql.Date as "May 9, 2026"
     * Returns "—" if the date is null.
     */
    public static String formatDate(java.sql.Date date) {
        if (date == null) return "—";
        return FMT_DATE_SHORT.format(date);
    }

    /**
     * Formats a Timestamp as just the time: "3:45 PM"
     * Returns "—" if the timestamp is null.
     */
    public static String formatTime(Timestamp ts) {
        if (ts == null) return "—";
        return FMT_TIME_ONLY.format(new Date(ts.getTime()));
    }

    /**
     * Returns a human-friendly "time ago" string.
     * Examples: "just now", "5 min ago", "2 hours ago", "3 days ago"
     *
     * This is used in the activity feed and alerts list.
     */
    public static String timeAgo(Timestamp ts) {
        if (ts == null) return "unknown time";

        long diffMillis = System.currentTimeMillis() - ts.getTime();
        long diffSeconds = diffMillis / 1_000;
        long diffMinutes = diffSeconds / 60;
        long diffHours   = diffMinutes / 60;
        long diffDays    = diffHours   / 24;

        if (diffSeconds < 30)  return "just now";
        if (diffMinutes < 1)   return diffSeconds + "s ago";
        if (diffMinutes < 60)  return diffMinutes + " min ago";
        if (diffHours   < 24)  return diffHours   + " hour"   + (diffHours == 1 ? "" : "s") + " ago";
        if (diffDays    < 30)  return diffDays    + " day"    + (diffDays  == 1 ? "" : "s") + " ago";
        return FMT_DATE_SHORT.format(new Date(ts.getTime()));
    }

    /**
     * Returns a Timestamp for 'daysAgo' days before right now.
     * Used by MockDataGenerator to create realistic past timestamps.
     */
    public static Timestamp daysAgo(int days) {
        long millis = System.currentTimeMillis() - ((long) days * 24 * 60 * 60 * 1000);
        return new Timestamp(millis);
    }

    /**
     * Returns a Timestamp for 'minutesAgo' minutes before right now.
     * Used by the simulation engine to timestamp recent events.
     */
    public static Timestamp minutesAgo(int minutes) {
        long millis = System.currentTimeMillis() - ((long) minutes * 60 * 1000);
        return new Timestamp(millis);
    }

    /**
     * Returns a java.sql.Date that is 'daysFromNow' in the future.
     * Used for setting deadlines on tasks and goals.
     */
    public static java.sql.Date futureDate(int daysFromNow) {
        long millis = System.currentTimeMillis() + ((long) daysFromNow * 24 * 60 * 60 * 1000);
        return new java.sql.Date(millis);
    }

    /** Returns today's date as a java.sql.Date */
    public static java.sql.Date today() {
        return new java.sql.Date(System.currentTimeMillis());
    }

    /** Returns the current time as a Timestamp */
    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
