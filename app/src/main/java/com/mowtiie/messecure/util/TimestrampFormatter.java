package com.mowtiie.messecure.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestrampFormatter {

    private static final long GROUP_THRESHOLD_MS = 5 * 60 * 1000; // 5 min

    public static boolean shouldShowTime(Date previousSentAt, String previousSenderId,
                                         Date currentSentAt, String currentSenderId) {
        if (previousSentAt == null || currentSentAt == null) return true;
        if (previousSenderId == null || currentSenderId == null) return true;
        if (!previousSenderId.equals(currentSenderId)) return true;
        long gap = currentSentAt.getTime() - previousSentAt.getTime();
        return gap > GROUP_THRESHOLD_MS;
    }

    public static boolean shouldShowDateChip(Date previousSentAt, Date currentSentAt) {
        if (currentSentAt == null) return false;
        if (previousSentAt == null) return true;
        return !sameDay(previousSentAt, currentSentAt);
    }

    private static boolean sameDay(Date a, Date b) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return fmt.format(a).equals(fmt.format(b));
    }

    public static String formatDateChip(Date d) {
        if (d == null) return "";
        long diffDays = (System.currentTimeMillis() - d.getTime()) / (24L * 60 * 60 * 1000);
        if (sameDay(d, new Date()))                            return "Today";
        if (sameDay(d, new Date(System.currentTimeMillis() - 86_400_000))) return "Yesterday";
        if (diffDays < 7) return new SimpleDateFormat("EEEE", Locale.getDefault()).format(d);
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(d);
    }

    public static String formatTime(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(d);
    }
}