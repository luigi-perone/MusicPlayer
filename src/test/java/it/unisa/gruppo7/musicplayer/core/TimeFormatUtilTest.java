package it.unisa.gruppo7.musicplayer.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TimeFormatUtil}, the "MM:SS" rendering used by the track
 * duration column and by the playlist summary labels.
 */
class TimeFormatUtilTest {

    /** Tests covering durations shorter than a full minute. */
    @Nested
    @DisplayName("Durations below one minute")
    class BelowOneMinute {

        /** Verifies that a zero duration, the empty-playlist case, renders as "00:00". */
        @Test
        @DisplayName("zero renders as 00:00")
        void zero_rendersAsDoubleZero() {
            assertEquals("00:00", TimeFormatUtil.formatDuration(0),
                    "an empty playlist must show a zeroed duration, not a blank label");
        }

        /** Verifies that single-digit seconds are zero-padded. */
        @Test
        @DisplayName("single-digit seconds are zero-padded")
        void singleDigitSeconds_arepadded() {
            assertEquals("00:05", TimeFormatUtil.formatDuration(5),
                    "seconds must always occupy two digits");
        }

        /** Verifies the last second before the minute rolls over. */
        @Test
        @DisplayName("59 seconds stay in the zero-minute bucket")
        void justBeforeTheMinute_staysAtZeroMinutes() {
            assertEquals("00:59", TimeFormatUtil.formatDuration(59));
        }
    }

    /** Tests covering durations of one minute or more. */
    @Nested
    @DisplayName("Durations of one minute or more")
    class OneMinuteOrMore {

        /** Verifies that an exact minute rolls the seconds back to zero. */
        @Test
        @DisplayName("60 seconds roll over to 01:00")
        void exactMinute_rollsOver() {
            assertEquals("01:00", TimeFormatUtil.formatDuration(60));
        }

        /** Verifies a typical track length, with both fields in double digits. */
        @Test
        @DisplayName("a typical track length renders both fields padded")
        void typicalTrackLength_rendersBothFields() {
            assertEquals("05:54", TimeFormatUtil.formatDuration(354),
                    "354 seconds must render as 5 minutes and 54 seconds");
        }

        /** Verifies that minutes are zero-padded below ten. */
        @Test
        @DisplayName("single-digit minutes are zero-padded")
        void singleDigitMinutes_arePadded() {
            assertEquals("09:01", TimeFormatUtil.formatDuration(541));
        }
    }

    /** Tests covering the aggregated durations shown in the playlist summary. */
    @Nested
    @DisplayName("Aggregated durations beyond one hour")
    class BeyondOneHour {

        /**
         * Verifies that durations past the hour keep counting in minutes instead of
         * rolling over into an hours field. Playlist totals routinely exceed an hour,
         * so this documents what the summary label actually shows.
         */
        @Test
        @DisplayName("minutes keep accumulating past 59 instead of becoming hours")
        void pastOneHour_keepsCountingInMinutes() {
            assertEquals("61:01", TimeFormatUtil.formatDuration(3661),
                    "one hour, one minute and one second must render as 61 minutes");
        }

        /** Verifies a long playlist total, well beyond the two-digit minute range. */
        @Test
        @DisplayName("a two-hour total renders as 120 minutes")
        void twoHours_rendersAsMinutes() {
            assertEquals("120:00", TimeFormatUtil.formatDuration(7200));
        }
    }
}

