package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;

import java.util.HashSet;
import java.util.stream.Collectors;


/**
 * @author francescoLemmo
 */
public class Library extends TrackCollection {

    // pattern singleton
    private static Library instance;
    private static final String DEFAULT_PATH = "src/main/java/it/unisa/gruppo7/musicplayer/library/track-library.json";


    private Library() {
        super(DEFAULT_PATH, new HashSet<>());
        this.load();
    }

    // --- Methods ---

    public static Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }

        return instance;
    }

    // -- toString --

    @Override
    public String toString() {
        return "Track Library:\n" +
                this.tracks.stream()
                        .map(String::valueOf)
                        .map(p -> " - " + p)
                        .collect(Collectors.joining(System.lineSeparator()));
    }

}
