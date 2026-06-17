package it.unisa.gruppo7.musicplayer.playback;


import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.undo.QueueMemento;


/**
 * Command to append a track (or a whole playlist) to the playback queue.
 * Snapshots the queue before the append so {@link #undo()} can restore it exactly.
 * The append itself is supplied as a {@link Runnable}, so the same command serves both
 * the single-track and the whole-playlist enqueue paths.
 *
 * @author Maxim Makhovskyy
 */
public class AddToQueueCommand implements UndoableCommand<Void> {

    private final PlaybackService playbackService;
    private final Runnable        appendAction;

    /** Queue state captured before the append, used to reverse the command. */
    private QueueMemento queueSnapshot;

    /**
     * Constructs a new AddToQueueCommand.
     *
     * @param playbackService The service managing the playback queue.
     * @param appendAction    The actual enqueue operation to perform (e.g. append a track or a playlist).
     */
    public AddToQueueCommand(PlaybackService playbackService, Runnable appendAction) {
        this.playbackService = playbackService;
        this.appendAction    = appendAction;
    }

    /**
     * Captures the queue state, then performs the enqueue operation.
     *
     * @return null upon completion.
     */
    @Override
    public Void execute() {
        this.queueSnapshot = (playbackService != null) ? playbackService.captureQueueState() : null;
        if (appendAction != null) {
            appendAction.run();
        }
        return null;
    }

    /**
     * Reverts the enqueue by restoring the queue to its previous state.
     */
    @Override
    public void undo() {
        if (queueSnapshot != null) {
            playbackService.restoreQueueState(queueSnapshot);
        }
    }
}
