package it.unisa.gruppo7.musicplayer.core;

/**
 * Contract for components that persist their state in a file stored in the storage.
 */
public interface PersistenceService {
    /**
     * Saves the current state to the configured storage.
     */
    void save();

    /**
     * Loads the previous state from the configured storage.
     */
    void load();
}
