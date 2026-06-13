package it.unisa.gruppo7.musicplayer.playback;

import java.util.*;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Represents the playback queue of the music player.
 * Extends {@link TrackCollection} and observes track changes via {@link TrackObserver}.
 *
 * <p>This class is the single owner of the playback cursor ({@code currentIndex}).
 * All index arithmetic lives here so that callers (the service, controllers) never
 * have to set the cursor by hand and risk de-syncing it from the playing track.</p>
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

    /**
     * Replaces the tracks in the playback list with the provided ones.
     *
     * @param tracks The new list of tracks to load.
     */
    public void loadTracks(List<Track> tracks) {
        this.clear();
        this.tracks.addAll(tracks);

        if (!tracks.isEmpty()) {
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
        this.tracks.addAll(tracks);

        if (isShuffleActive) {
            shuffledTracks.addAll(tracks);
        }
    }

    /**
     * Removes all tracks from the playback list.
     */
    public void clear() {
        this.tracks.clear();
        this.shuffledTracks.clear();
        this.currentIndex = -1;
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
        List<Track> other  = isShuffleActive ? canonicalList() : shuffledTracks;

        boolean currentRemoved = currentIndex >= 0
                && currentIndex < active.size()
                && track.equals(active.get(currentIndex));

        // Walk the active list, recording where the track lives and removing every
        // occurrence. This is the "look up all the indices, then update the list" step.
        int removedBeforeCurrent = 0;
        boolean removedAny = false;
        for (int i = active.size() - 1; i >= 0; i--) {
            if (track.equals(active.get(i))) {
                active.remove(i);
                removedAny = true;
                if (i < currentIndex) {
                    removedBeforeCurrent++;
                }
            }
        }

        // Keep the non-active list consistent (no cursor lives there).
        other.removeIf(track::equals);

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
}
