package it.unisa.gruppo7.musicplayer.track;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.lang.IllegalArgumentException;
import java.time.Year;
import java.util.UUID;

/**
 * This class represents the model of the music track.
 *
 * @author francescoLemmo
 */

public class Track {

    private final UUID id;
    private String title;
    private String author;
    private int duration;   // track duration in seconds
    private String genre;
    private Year publicationYear;


    /**
     * Complete Track constructor
     *
     * @param id                The unique track identifier
     * @param title             The title of the track
     * @param author            The creator of the track
     * @param duration          The length (in seconds) of the track
     * @param genre             The music genre of the track
     * @param publicationYear   The year the track was released
     *
     * @throws IllegalArgumentException     If title or author strings are empty,
     *                                      if the duration is negative or the publication year is in the future.
     */

    @JsonCreator
    public Track(
            @JsonProperty("id") UUID id,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("duration") int duration,
            @JsonProperty("genre") String genre,
            @JsonProperty("publicationYear") Year publicationYear) {

        this.validateArguments(title, author, duration, genre, publicationYear);

        this.id = (id == null) ? UUID.randomUUID() : id;
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.genre = genre;
        this.publicationYear = publicationYear;
    }

    public Track(String title, String author, int duration, String genre, Year publicationYear) {
        this(null, title, author, duration, genre, publicationYear);
    }


    public Track(String title, String author, int duration) {
        this(title, author, duration, "not-specified", null);
    }

    public Track(String title, String author, int duration, String genre) {
        this(title, author, duration, genre, null);
    }


    public Track(String title, String author, int duration, Year publicationYear) {
        this(title, author, duration, "not-specified", publicationYear);
    }


    // --- Methods ---

    public void validateArguments(String title, String author, int duration, String genre, Year publicationYear) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Track title must be included");
        }
        if (author == null || author.isEmpty()) {
            throw new IllegalArgumentException("Track author must be included");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Track duration must be positive and non-zero");
        }
        if (publicationYear != null) {
            if (((publicationYear.compareTo(Year.of(1877)) < 0) || (publicationYear.compareTo(Year.now()) > 0))) {
                throw new IllegalArgumentException("Track Publication Year must be prior to the current one");
            }
        }
    }

    // -- getter and setter --

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Track duration must be positive and non-zero");
        }
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Year getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Year publicationYear) {
        if (publicationYear != null) {
            if ((publicationYear.compareTo(Year.of(1877)) < 0) || (publicationYear.compareTo(Year.now()) > 0)) {
                throw new IllegalArgumentException("Track Publication Year must be prior to the current one");
            }
        }
        this.publicationYear = publicationYear;
    }

    // -- toString --

    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", duration=" + duration +
                ", genre='" + genre + '\'' +
                ", publicationYear=" + publicationYear +
                '}';
    }

    // -- hashCode & equals --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(id, track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
