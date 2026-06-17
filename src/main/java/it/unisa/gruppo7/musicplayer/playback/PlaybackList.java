package it.unisa.gruppo7.musicplayer.playback;

import java.util.*;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.undo.QueueMemento;

/**
 * Represents the playback queue of the music player.
 * Extends {@link TrackCollection} and observes track changes via {@link TrackObserver}.
 *
 * This class is the single owner of the playback cursor ({@code currentIndex}).
 * All index arithmetic lives here so that callers (the service, controllers) never
 * have to set the cursor by hand and risk de-syncing it from the playing track.
 */
public class PlaybackList extends TrackCollection implements TrackObserver {
    private static final String DEFAULT_PATH = null;

    /** A separate list maintaining the randomized order of tracks when shuffle mode is active. */
    private List<Track> shuffledTracks;

    /** Flag indicating whether the playback queue is currently operating in shuffle mode. */
    private boolean isShuffleActive;

    /** Cursor into {@link #getActiveList()} pointing at the track being played; -1 means "no current track". */
    private int currentIndex = -1;

    /**
     * Playlist-block ids, kept aligned 1:1 with the canonical (non-shuffled) track list.
     * Every load/append operation tags its tracks with a fresh incremental id, so that
     * a "block" corresponds to one playlist (or one batch) added to the queue. This is
     * what lets {@link #getNextPlaylistTrack()} / {@link #getPreviousPlaylistTrack()}
     * jump between playlists instead of single tracks.
     */
    private final List<Integer> blockIds = new ArrayList<>();

    /** Next playlist-block id to assign. */
    private int nextBlockId = 0;

    /**
     * Maps each playlist-block id to the source {@link Playlist} it was added from.
     * Ad-hoc blocks (a bare library load, or a single track appended outside of a
     * playlist) have no entry here. This is what lets a playlist reorder be
     * propagated to <b>every</b> queued copy of that playlist, not just the active one.
     */
    private final Map<Integer, Playlist> blockSources = new HashMap<>();


    /**
     * Constructs a new empty playback list.
     */
    public PlaybackList() {
        super(DEFAULT_PATH, new ArrayList<>());
        this.shuffledTracks = new ArrayList<>();
        this.isShuffleActive = false;
    }

    /**
     * Returns the canonical (non-shuffled) backing list.
     */
    @SuppressWarnings("unchecked")
    private List<Track> canonicalList() {
        return (List<Track>) this.tracks;
    }

    /**
     * Returns the currently active list of tracks based on the shuffle state.
     *
     * @return The shuffled list if shuffle is active, otherwise the canonical track list.
     */
    public List<Track> getActiveList() {
        return isShuffleActive() ? this.shuffledTracks : canonicalList();
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Returns the track the cursor currently points at, or null if the cursor is
     * out of range (e.g. the queue is empty or playback has run off the end).
     */
    public Track getCurrentTrack() {
        List<Track> trackList = getActiveList();
        if (currentIndex < 0 || currentIndex >= trackList.size()) {
            return null;
        }
        return trackList.get(currentIndex);
    }

    /**
     * Moves the cursor to the given track (first occurrence in the active list).
     * Use this instead of {@link #setCurrentIndex(int)} so the cursor always
     * matches the track that is actually being played.
     *
     * @param track the track to point the cursor at.
     * @return true if the track was found and the cursor moved, false otherwise.
     */
    public boolean jumpTo(Track track) {
        int idx = getActiveList().indexOf(track);
        if (idx >= 0) {
            this.currentIndex = idx;
            return true;
        }
        return false;
    }

    public Track getNextTrack() {
        List<Track> trackList = getActiveList();
        if (trackList == null || trackList.isEmpty()) return null;
        if (currentIndex < 0 || currentIndex >= trackList.size() - 1) return null;
        this.setCurrentIndex(currentIndex + 1);
        return trackList.get(currentIndex);
    }

    public Track getPreviousTrack() {
        List<Track> trackList = getActiveList();
        if (trackList == null || trackList.isEmpty()) return null;
        if (currentIndex <= 0) return null;
        this.setCurrentIndex(currentIndex - 1);
        return trackList.get(currentIndex);
    }

    // -- playlist-block navigation (US-029) --

    /**
     * Computes the canonical-list index of the first track of the <b>next</b>
     * playlist block, relative to the block the cursor is currently in.
     *
     * @return the target index, or -1 if there is no following block.
     */
    private int nextPlaylistStartIndex() {
        List<Track> canonical = canonicalList();
        if (canonical.isEmpty() || blockIds.isEmpty()) return -1;

        int idx = currentIndex;
        // Cursor unset: the "next playlist" is simply the first block.
        if (idx < 0) return 0;
        if (idx >= blockIds.size()) return -1;

        int currentBlock = blockIds.get(idx);
        for (int i = idx + 1; i < blockIds.size(); i++) {
            if (blockIds.get(i) != currentBlock) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Computes the canonical-list index of the first track of the <b>previous</b>
     * playlist block (always the start of the preceding block, regardless of the
     * cursor's position within the current block).
     *
     * @return the target index, or -1 if the cursor is already in the first block.
     */
    private int previousPlaylistStartIndex() {
        List<Track> canonical = canonicalList();
        if (canonical.isEmpty() || blockIds.isEmpty()) return -1;

        int idx = currentIndex;
        if (idx < 0) idx = 0;
        if (idx >= blockIds.size()) idx = blockIds.size() - 1;

        int currentBlock = blockIds.get(idx);
        // Walk back to the start of the current block.
        int start = idx;
        while (start > 0 && blockIds.get(start - 1) == currentBlock) {
            start--;
        }
        if (start == 0) return -1; // already in the first block

        // Walk back to the start of the previous block.
        int prevBlock = blockIds.get(start - 1);
        int prevStart = start - 1;
        while (prevStart > 0 && blockIds.get(prevStart - 1) == prevBlock) {
            prevStart--;
        }
        return prevStart;
    }

    /**
     * Moves the cursor to the first track of the next playlist block and returns it.
     *
     * @return the first track of the next block, or null if there is none.
     */
    public Track getNextPlaylistTrack() {
        int target = nextPlaylistStartIndex();
        if (target < 0) return null;
        this.currentIndex = target;
        return canonicalList().get(target);
    }

    /**
     * Moves the cursor to the first track of the previous playlist block and returns it.
     *
     * @return the first track of the previous block, or null if the cursor is in the first block.
     */
    public Track getPreviousPlaylistTrack() {
        int target = previousPlaylistStartIndex();
        if (target < 0) return null;
        this.currentIndex = target;
        return canonicalList().get(target);
    }

    /**
     * @return true if a following playlist block exists to skip to.
     */
    public boolean hasNextPlaylist() {
        return nextPlaylistStartIndex() >= 0;
    }

    /**
     * @return true if a preceding playlist block exists to skip to.
     */
    public boolean hasPreviousPlaylist() {
        return previousPlaylistStartIndex() >= 0;
    }

    /**
     * Replaces the tracks in the playback list with the provided ones.
     *
     * @param tracks The new list of tracks to load.
     */
    public void loadTracks(List<Track> tracks) {
        loadTracks(tracks, null);
    }

    /**
     * Replaces the tracks in the playback list with the provided ones, tagging the
     * resulting block with the {@link Playlist} they originate from.
     *
     * @param tracks The new list of tracks to load.
     * @param source The playlist the tracks come from, or null for an ad-hoc load.
     */
    public void loadTracks(List<Track> tracks, Playlist source) {
        this.clear();
        this.tracks.addAll(tracks);

        if (!tracks.isEmpty()) {
            // A fresh load is a single playlist block.
            int id = nextBlockId++;
            for (int i = 0; i < tracks.size(); i++) {
                blockIds.add(id);
            }
            if (source != null) {
                blockSources.put(id, source);
            }
            this.currentIndex = 0;
        }

        if (isShuffleActive) {
            shuffleTracks(null);
        }
    }

    /**
     * Appends the specified tracks to the end of the playback list.
     *
     * @param tracks The list of tracks to append.
     */
    public void appendTracks(List<Track> tracks) {
        appendTracks(tracks, null);
    }

    /**
     * Appends the specified tracks to the end of the playback list as a new block,
     * tagging that block with the {@link Playlist} they originate from.
     *
     * @param tracks The list of tracks to append.
     * @param source The playlist the tracks come from, or null for an ad-hoc append.
     */
    public void appendTracks(List<Track> tracks, Playlist source) {
        this.tracks.addAll(tracks);

        if (tracks != null && !tracks.isEmpty()) {
            // Each append (e.g. one appended playlist) becomes a new block.
            int id = nextBlockId++;
            for (int i = 0; i < tracks.size(); i++) {
                blockIds.add(id);
            }
            if (source != null) {
                blockSources.put(id, source);
            }
        }

        if (isShuffleActive) {
            shuffledTracks.addAll(tracks);
        }
    }

    /**
     * Moves a track within the canonical list from one position to another,
     * keeping the playlist-block ids aligned 1:1 and repositioning the cursor so
     * it still points at the same logical track that is currently playing.
     *
     * <p>This is the queue-side counterpart of a playlist reorder (US-027). It is
     * deliberately a pure list operation on the canonical (non-shuffled) list and
     * does not touch the playing track reference or the timer, so playback
     * continues without interruption:</p>
     *
     * <ul>
     *   <li>if the moved track <em>is</em> the current one, the cursor follows it
     *       to {@code to};</li>
     *   <li>if the move crosses the cursor, the cursor shifts by one to keep
     *       pointing at the same track.</li>
     * </ul>
     *
     * @param from the current index of the track in the canonical list.
     * @param to   the target index in the canonical list.
     */
    public void moveTrack(int from, int to) {
        List<Track> canonical = canonicalList();
        int size = canonical.size();

        // No-op on out-of-range indices or a move that changes nothing.
        if (from < 0 || from >= size || to < 0 || to >= size || from == to) {
            return;
        }

        Track track = canonical.remove(from);
        canonical.add(to, track);

        Integer blockId = blockIds.remove(from);
        blockIds.add(to, blockId);

        // Reposition the cursor so it keeps pointing at the same playing track.
        if (currentIndex == from) {
            currentIndex = to;
        } else if (from < currentIndex && to >= currentIndex) {
            currentIndex--;
        } else if (from > currentIndex && to <= currentIndex) {
            currentIndex++;
        }
    }

    /**
     * Propagates a playlist reorder (US-027) to <b>every</b> queue block that was
     * added from the given {@link Playlist}, moving the track at block-relative
     * index {@code from} to block-relative index {@code to} inside each such block.
     *
     * <p>This is what keeps multiple queued copies of the same playlist in sync with
     * a reorder performed on the playlist itself. It operates on the canonical
     * (non-shuffled) list and reuses {@link #moveTrack(int, int)} for each block, so
     * the cursor keeps pointing at the track currently playing and playback is not
     * interrupted.</p>
     *
     * <p>Because every move is internal to a single block (no net change in length),
     * the start positions of the other blocks do not drift, so the block ranges are
     * collected up front and the moves applied afterwards. Blocks whose size does not
     * cover both indices are skipped.</p>
     *
     * @param source the playlist whose queued blocks must be reordered.
     * @param from   the block-relative source index.
     * @param to     the block-relative target index.
     */
    public void reorderWithinPlaylistBlocks(Playlist source, int from, int to) {
        if (source == null || from == to) {
            return;
        }

        // Collect the start of every block belonging to this playlist first.
        List<Integer> blockStarts = new ArrayList<>();
        List<Integer> blockSizes = new ArrayList<>();
        int i = 0;
        int size = blockIds.size();
        while (i < size) {
            int blockId = blockIds.get(i);
            int start = i;
            while (i < size && blockIds.get(i) == blockId) {
                i++;
            }
            if (source.equals(blockSources.get(blockId))) {
                blockStarts.add(start);
                blockSizes.add(i - start);
            }
        }

        for (int b = 0; b < blockStarts.size(); b++) {
            int start = blockStarts.get(b);
            int blockSize = blockSizes.get(b);
            if (from >= 0 && from < blockSize && to >= 0 && to < blockSize) {
                moveTrack(start + from, start + to);
            }
        }
    }

    /**
     * Removes all tracks from the playback list.
     */
    public void clear() {
        this.tracks.clear();
        this.shuffledTracks.clear();
        this.blockIds.clear();
        this.blockSources.clear();
        this.currentIndex = -1;
        this.nextBlockId = 0;
    }

    /**
     * Retrieves the first track in the active playback list.
     *
     * @return The first track, or null if the list is empty.
     */
    public Track getFirstTrack() {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) {
            return null;
        }

        return trackList.get(0);
    }


    public List<Track> getUpNextQueue() {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) {
            return new ArrayList<>();
        }

        // if the cursor is unset or already at the last position, there is nothing "up next"
        if (currentIndex < 0 || currentIndex >= trackList.size() - 1) {
            return new ArrayList<>();
        }

        // return a list with only the up next tracks
        return new ArrayList<>(trackList.subList(currentIndex + 1, trackList.size()));
    }

    /**
     * Returns the current shuffle mode state.
     *
     * @return true if shuffle is active, false otherwise.
     */
    public boolean isShuffleActive() {
        return isShuffleActive;
    }

    /**
     * Sets a new shuffle mode state.
     *
     * @param shuffleState The new shuffle state.
     * @param currentTrack The current track playing.
     */
    public void setShuffle(boolean shuffleState, Track currentTrack) {
        this.isShuffleActive = shuffleState;
        if (shuffleState) {
            shuffleTracks(currentTrack);
            this.currentIndex = (currentTrack != null && !shuffledTracks.isEmpty()) ? 0 : -1;
        } else {
            shuffledTracks.clear();
            if (currentTrack != null) {
                int idx = canonicalList().indexOf(currentTrack);
                this.currentIndex = Math.max(idx, 0);
            }
        }
    }

    /**
     * Generates a randomized version of the current track list.
     * If a track is currently playing, it is moved to the head of the shuffled list.
     *
     * @param currentTrack The currently playing track, or null.
     */
    private void shuffleTracks(Track currentTrack) {
        shuffledTracks = new ArrayList<>(this.tracks);
        Collections.shuffle(shuffledTracks);

        // if a track is playing, it is positioned at the head of the queue
        if (currentTrack != null && shuffledTracks.contains(currentTrack)) {
            shuffledTracks.remove(currentTrack);
            shuffledTracks.add(0, currentTrack);
        }
    }

    /**
     * Inserts a track at a random position within the shuffled queue,
     * after the current track (index 0 is reserved for the playing track).
     * Falls back to appending at the end if the shuffled list is empty.
     *
     * @param track The track to insert.
     */
    public void insertTrackAtRandom(Track track) {
        if (shuffledTracks.isEmpty()) {
            shuffledTracks.add(track);
            return;
        }
        // Insert anywhere after position 0 (position 0 = currently playing)
        int insertIndex = shuffledTracks.size() == 1
                ? 1
                : 1 + new Random().nextInt(shuffledTracks.size() - 1);
        shuffledTracks.add(insertIndex, track);
    }

    /**
     * Adds a track to the canonical list without triggering the shuffle-append side-effect of appendTracks.
     *
     * @param track The track to add.
     */
    void addToCanonicalList(Track track) {
        this.tracks.add(track);
        // A single appended track is its own one-track block.
        this.blockIds.add(nextBlockId++);
    }

    /**
     * Removes a track from the queue.
     *
     * <p>Because the playback queue may legitimately contain the same {@link Track}
     * more than once (e.g. it was loaded from the library and later appended again
     * from a playlist), this method removes <b>every</b> occurrence from both the
     * canonical list and the shuffled list, and then repositions the cursor so it
     * keeps pointing at the same logical track:</p>
     *
     * <ul>
     *   <li>occurrences located <em>before</em> the cursor shift it left by their count;</li>
     *   <li>if the <em>current</em> track itself is removed, the cursor lands on the
     *       track that takes its place (the natural "next" track), or becomes -1 if
     *       playback ran off the end.</li>
     * </ul>
     *
     * @param track The track to remove.
     * @return true if at least one occurrence was removed from the queue.
     */
    @Override
    public boolean removeTrack(Track track) {
        if (track == null) {
            return false;
        }

        List<Track> active = getActiveList();

        boolean currentRemoved = currentIndex >= 0
                && currentIndex < active.size()
                && track.equals(active.get(currentIndex));

        // Count occurrences before the cursor in the ACTIVE list (the cursor lives here).
        int removedBeforeCurrent = 0;
        boolean removedAny = false;
        for (int i = active.size() - 1; i >= 0; i--) {
            if (track.equals(active.get(i))) {
                removedAny = true;
                if (i < currentIndex) {
                    removedBeforeCurrent++;
                }
            }
        }

        // Remove every occurrence from the canonical list, keeping blockIds aligned 1:1.
        List<Track> canonical = canonicalList();
        for (int i = canonical.size() - 1; i >= 0; i--) {
            if (track.equals(canonical.get(i))) {
                canonical.remove(i);
                blockIds.remove(i);
            }
        }

        // Keep the shuffled list consistent (no cursor or block info lives there).
        shuffledTracks.removeIf(track::equals);

        // Reposition the cursor.
        currentIndex -= removedBeforeCurrent;
        if (active.isEmpty()) {
            currentIndex = -1;
        } else if (currentRemoved && currentIndex >= active.size()) {
            // We removed the playing track and there is nothing after it.
            currentIndex = -1;
        } else if (currentIndex < 0) {
            currentIndex = 0;
        }

        return removedAny;
    }


    /**
     * Handles the track deletion event by removing every occurrence from the queue.
     *
     * @param track The track that was deleted.
     */
    @Override
    public void onTrackDeleted(Track track) {
        this.removeTrack(track);
    }

    /**
     * Handles the track edit event.
     * No-op: metadata changes do not affect queue order or identity.
     *
     * @param track The track that was edited.
     */
    @Override
    public void onTrackEdit(Track track) {

    }

    /**
     * Captures the full state of the queue (canonical order, shuffled order, cursor
     * and shuffle flag) into an immutable memento, for later restoration on undo.
     *
     * @return a snapshot of the current queue state.
     */
    public QueueMemento snapshot() {
        return new QueueMemento(canonicalList(), this.shuffledTracks,
                this.currentIndex, this.isShuffleActive);
    }

    /**
     * Restores the queue to a previously captured state, replacing the canonical and
     * shuffled lists, the cursor and the shuffle flag with the snapshot's values.
     * Only the queue structure is restored; the audio currently playing is not changed.
     *
     * @param memento the state to restore; ignored if null.
     */
    public void restore(QueueMemento memento) {
        if (memento == null) return;
        this.tracks.clear();
        this.tracks.addAll(memento.getCanonicalTracks());
        this.shuffledTracks  = memento.getShuffledTracks();
        this.isShuffleActive = memento.isShuffleActive();
        this.currentIndex    = memento.getCurrentIndex();
    }
}
