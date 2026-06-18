package it.unisa.gruppo7.musicplayer.core;

/**
 * Utility for formatting time durations.
 * <p>
 * Extracts the "MM:SS" formatting out of the facade: rendering a duration for
 * display is a presentation concern, not part of coordinating the subsystems.
 *
 * @author Gruppo 7
 */
public final class TimeFormatUtil {

    private TimeFormatUtil() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Formats a duration given in seconds as "MM:SS".
     *
     * @param totalSeconds the total duration in seconds.
     * @return the duration formatted as zero-padded "MM:SS".
     */
    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
