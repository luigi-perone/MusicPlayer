package it.unisa.gruppo7.musicplayer.undo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;


/**
 * Caretaker for the undo history (Memento pattern).
 * 
 * Keeps a LIFO stack of recently executed {@link UndoableCommand}s, each stamped
 * with the instant it was recorded. Entries older than a configurable time-to-live
 * are considered expired and discarded, so an undo is only offered within a short
 * window after the action.
 */
public class UndoManager {
    /** Default time window during which the last action can still be undone. */
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);
    private final Deque<UndoEntry> history = new ArrayDeque<>();
    private final Duration ttl;
    private final Clock clock;


    /**
     * Creates a manager with the default time-to-live and the system clock.
     */
    public UndoManager(){
        this(DEFAULT_TTL, Clock.systemDefaultZone());
    }
    
    /**
     * Creates a manager with a custom time-to-live and clock.
     *
     * @param ttl   how long an entry stays undoable after being recorded.
     * @param clock the time source used to stamp and expire entries.
     */
    public UndoManager(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * Immutable pairing of a command with the instant it was recorded.
     */
    private static final class UndoEntry {
        private final UndoableCommand<?> command;
        private final Instant timestamp;

        UndoEntry(UndoableCommand<?> command, Instant timestamp) {
            this.command = command;
            this.timestamp = timestamp;
        }
    }

    /**
     * Records a successfully executed command as the most recent undoable action.
     *
     * @param command the command to make undoable; ignored if null.
     */
    public void push(UndoableCommand<?> command) {
        if (command == null) return;
        purgeExpired();
        history.push(new UndoEntry(command, clock.instant()));
    }

    /**
     * Reverts the most recent non-expired action, if any.
     *
     * @return true if an action was undone, false if there was nothing to undo.
     * @throws Exception if the command's {@link UndoableCommand#undo()} fails.
     */
    public boolean undoLast() throws Exception {
        purgeExpired();
        if (history.isEmpty()) return false;
        UndoEntry entry = history.pop();
        entry.command.undo();
        return true;
    }

    /**
     * Tells whether there is at least one non-expired action available to undo.
     *
     * @return true if an undo is currently possible.
     */
    public boolean isUndoAvailable() {
        purgeExpired();
        return !history.isEmpty();
    }

    /**
     * Drops every recorded action, regardless of age.
     */
    public void clear() {
        history.clear();
    }

    /**
     * Number of currently retained entries. Expired entries are not purged by this
     * call; it is exposed mainly for testing.
     *
     * @return the current history size.
     */
    public int size() {
        return history.size();
    }

    /**
     * Removes entries whose age exceeds the time-to-live. Because newer entries sit
     * on top of the stack, the expired ones are always the oldest, at the bottom.
     */
    private void purgeExpired() {
        Instant now = clock.instant();
        while (!history.isEmpty()) {
            UndoEntry oldest = history.peekLast();
            if (Duration.between(oldest.timestamp, now).compareTo(ttl) > 0) {
                history.pollLast();
            } else {
                break;
            }
        }
    }

    /**
     * Returns the time-to-live applied to pushed undo actions.
     *
     * @return the undo time-to-live
     */
    public Duration getTtl() {
        return ttl;
    }
}
