package it.unisa.gruppo7.musicplayer.undo;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import org.junit.jupiter.api.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US-020: the temporary undo stack.
 * Covers LIFO ordering, availability, clearing and time-to-live expiry driven by an
 * injectable clock, so the expiry logic is verified without any real waiting.
 */
class UndoManagerTest {

    private MutableClock clock;

    /** Initializes a mutable clock at a fixed instant before each test. */
    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    }

    // ------------------------------------------------------------------
    // Stack behaviour
    // ------------------------------------------------------------------

    /** Tests covering the undo stack's LIFO ordering, availability and clearing. */
    @Nested
    @DisplayName("Stack behaviour")
    class StackBehaviour {

        /** Verifies that undoLast reverts the most recent command and reports success. */
        @Test
        @DisplayName("undoLast reverts the most recent command and reports success")
        void undoLast_revertsMostRecent() throws Exception {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            FakeCommand command = new FakeCommand();
            manager.push(command);

            boolean undone = manager.undoLast();

            assertTrue(undone, "undoLast must report success when something was undone");
            assertTrue(command.undone, "the command's undo() must have been invoked");
        }

        /** Verifies that undoLast on an empty stack reports failure. */
        @Test
        @DisplayName("undoLast on an empty stack reports failure")
        void undoLast_emptyStack_returnsFalse() throws Exception {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            assertFalse(manager.undoLast(), "undoLast must return false when there is nothing to undo");
        }

        /** Verifies that commands are undone in last-in-first-out order. */
        @Test
        @DisplayName("commands are undone in LIFO order")
        void undo_isLifo() throws Exception {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            FakeCommand first  = new FakeCommand();
            FakeCommand second = new FakeCommand();
            manager.push(first);
            manager.push(second);

            manager.undoLast();
            assertTrue(second.undone, "the last pushed command must be undone first");
            assertFalse(first.undone, "the earlier command must not be undone yet");

            manager.undoLast();
            assertTrue(first.undone, "the earlier command must be undone on the second call");
        }

        /** Verifies that clear discards every pending action. */
        @Test
        @DisplayName("clear discards every pending action")
        void clear_emptiesStack() {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            manager.push(new FakeCommand());
            manager.push(new FakeCommand());

            manager.clear();

            assertEquals(0, manager.size(), "the stack must be empty after clear()");
            assertFalse(manager.isUndoAvailable(), "no undo must be available after clear()");
        }
    }

    // ------------------------------------------------------------------
    // Time-to-live expiry
    // ------------------------------------------------------------------

    /** Tests covering the time-to-live expiry of pending undo actions. */
    @Nested
    @DisplayName("Time-to-live expiry")
    class TimeToLiveExpiry {

        /** Verifies that an action is still undoable within the TTL window. */
        @Test
        @DisplayName("an action is still undoable within the TTL window")
        void withinTtl_stillAvailable() {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            manager.push(new FakeCommand());

            clock.advance(Duration.ofSeconds(9));

            assertTrue(manager.isUndoAvailable(),
                    "the action must still be undoable before the TTL elapses");
        }

        /** Verifies that an action expires and is dropped once the TTL is exceeded. */
        @Test
        @DisplayName("an action expires once the TTL is exceeded")
        void pastTtl_expiresAndIsDropped() throws Exception {
            UndoManager manager = new UndoManager(Duration.ofSeconds(10), clock);
            FakeCommand command = new FakeCommand();
            manager.push(command);

            clock.advance(Duration.ofSeconds(11));

            assertFalse(manager.isUndoAvailable(), "the action must no longer be undoable after the TTL");
            assertFalse(manager.undoLast(), "undoLast must report failure once the action expired");
            assertFalse(command.undone, "an expired command must never be undone");
        }
    }

    // ------------------------------------------------------------------
    // Test doubles
    // ------------------------------------------------------------------

    /** Records whether undo() was invoked. */
    private static final class FakeCommand implements UndoableCommand<String> {
        private boolean undone;

        @Override
        public String execute() {
            return "ok";
        }

        @Override
        public void undo() {
            this.undone = true;
        }
    }

    /** Clock whose current instant can be advanced manually, to drive TTL tests. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
