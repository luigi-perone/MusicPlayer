package it.unisa.gruppo7.musicplayer.core;

/**
 * Utility per la formattazione di durate temporali.
 * <p>
 * Estrae dalla facade la formattazione "MM:SS", che è una responsabilità di
 * presentazione e non di coordinamento dei sottosistemi.
 *
 * @author Gruppo 7
 */
public final class TimeFormatUtil {

    private TimeFormatUtil() {
        // classe di utilità: non istanziabile
    }

    /**
     * Formatta una durata in secondi nel formato "MM:SS".
     *
     * @param totalSeconds la durata totale in secondi.
     * @return la durata formattata come "MM:SS" con zero-padding.
     */
    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
