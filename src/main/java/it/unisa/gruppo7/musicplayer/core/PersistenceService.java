package it.unisa.gruppo7.musicplayer.core;

/**
 * Contract for components that persist their state to a file in storage.
 * Defines fundamental serialization and deserialization hook operations.
 * @author Francesco Lemmo
 */
public interface PersistenceService {
    /**
     * Saves the current state of the implementing component to the configured storage file.
     */
    void save();

    /**
     * Loads the previously persisted state into the implementing component from the configured storage file.
     */
    void load();
}