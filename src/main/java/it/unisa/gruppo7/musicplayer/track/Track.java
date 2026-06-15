package it.unisa.gruppo7.musicplayer.track;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.lang.IllegalArgumentException;
import java.time.Year;
import java.util.UUID;

/**
 * Represents the data model of a music track.
 * Contains structural attributes, validation behaviors, and serialization bindings.
 *
 * @author Francesco Lemmo
 */
public class Track {

    private final UUID id;
    private String title;
    private String author;
    private int duration;   // track duration in seconds
    private String genre;
    private Year publicationYear;
    private int playCount;

    /**
     * Complete Track constructor used for initialization and JSON deserialization.
     *
     * @param id                The unique track identifier. If null, a random UUID is generated.
     * @param title             The title of the track.
     * @param author            The creator or artist of the track.
     * @param duration          The length of the track in seconds.
     * @param genre             The music genre of the track.
     * @param publicationYear   The year the track was released.
     * @param playCount         The number of times the track has been played.
     * @throws IllegalArgumentException If title or author are empty, duration is non-positive,
     * or publication year is invalid.
     */
    @JsonCreator
    public Track(
            @JsonProperty("id") UUID id,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("duration") int duration,
            @JsonProperty("genre") String genre,
            @JsonProperty("publicationYear") Year publicationYear,
            @JsonProperty("playCount") Integer playCount) {

        this.validateArguments(title, author, duration, genre, publicationYear, playCount);

        this.id = (id == null) ? UUID.randomUUID() : id;
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.genre = genre;
        this.publicationYear = publicationYear;
        this.playCount = (playCount == null) ? 0 : playCount;
    }

    /**
     * Constructs a Track with a generated ID.
     *
     * @param title           The title of the track.
     * @param author          The creator or artist of the track.
     * @param duration        The length of the track in seconds.
     * @param genre           The music genre of the track.
     * @param publicationYear The year the track was released.
     */
    public Track(String title, String author, int duration, String genre, Year publicationYear) {
        this(null, title, author, duration, genre, publicationYear, null);
    }

    /**
     * Constructs a Track with default genre ("not-specified") and no publication year.
     *
     * @param title    The title of the track.
     * @param author   The creator or artist of the track.
     * @param duration The length of the track in seconds.
     */
    public Track(String title, String author, int duration) {
        this(title, author, duration, "not-specified", null);
    }

    /**
     * No-arg constructor required by serialization frameworks or tests.
     */
    protected Track() {
        this.id = UUID.randomUUID();
    }

    /**
     * Constructs a Track with no publication year.
     *
     * @param title    The title of the track.
     * @param author   The creator or artist of the track.
     * @param duration The length of the track in seconds.
     * @param genre    The music genre of the track.
     */
    public Track(String title, String author, int duration, String genre) {
        this(title, author, duration, genre, null);
    }

    /**
     * Constructs a Track with default genre ("not-specified").
     *
     * @param title           The title of the track.
     * @param author          The creator or artist of the track.
     * @param duration        The length of the track in seconds.
     * @param publicationYear The year the track was released.
     */
    public Track(String title, String author, int duration, Year publicationYear) {
        this(title, author, duration, "not-specified", publicationYear);
    }

    // --- Methods ---

    /**
     * Validates field values against the domain business constraints.
     *
     * @param title           The title string to check.
     * @param author          The author string to check.
     * @param duration        The duration integer to check.
     * @param genre           The genre string to check.
     * @param publicationYear The publication year object to check.
     * @param playCount       The number of times the track has been played to check.
     * @throws IllegalArgumentException If any of the provided fields violate validation constraints.
     */
    public void validateArguments(String title, String author, int duration, String genre, Year publicationYear, Integer playCount) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Track title must be included");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Track author must be included");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Track duration must be positive and non-zero");
        }
        if (publicationYear != null) {
            if (((publicationYear.compareTo(Year.of(1877)) < 0) || (publicationYear.compareTo(Year.now()) > 0))) {
                throw new IllegalArgumentException("Track Publication Year must be a valid year and prior to the current one");
            }
        }
        if (playCount != null) {
            if (playCount.compareTo(0) < 0) {
                throw new IllegalArgumentException("Play Count must be non-negative");
            }
        }
    }

    /**
     * Mutates the track attributes in a single operation after running domain validation.
     *
     * @param newTitle           The new title.
     * @param newAuthor          The new author/artist.
     * @param newDuration        The new duration in seconds.
     * @param newGenre           The new music genre.
     * @param newPublicationYear The new release year.
     */
    public void modifyTrack(String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear) {
        this.validateArguments(newTitle, newAuthor, newDuration, newGenre, newPublicationYear, null);

        this.title = newTitle;
        this.author = newAuthor;
        this.duration = newDuration;
        this.genre = newGenre;
        this.publicationYear = newPublicationYear;
    }

    // -- getter and setter --

    /**
     * Gets the unique identifier of the track.
     *
     * @return The UUID of the track.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the title of the track.
     *
     * @return The track title string.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the track.
     *
     * @param title The track title string.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the author or artist of the track.
     *
     * @return The track author string.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author or artist of the track.
     *
     * @param author The track author string.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets the duration of the track in seconds.
     *
     * @return The track duration.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the track in seconds.
     *
     * @param duration The track duration in seconds.
     * @throws IllegalArgumentException If the duration is less than or equal to zero.
     */
    public void setDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Track duration must be positive and non-zero");
        }
        this.duration = duration;
    }

    /**
     * Gets the musical genre of the track.
     *
     * @return The track genre string.
     */
    public String getGenre() {
        return genre;
    }

    /**
     * Sets the musical genre of the track.
     *
     * @param genre The track genre string.
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Gets the publication year of the track.
     *
     * @return The release Year instance, or null if unspecified.
     */
    public Year getPublicationYear() {
        return publicationYear;
    }

    /**
     * Sets the publication year of the track after validating its constraints.
     *
     * @param publicationYear The release Year object.
     * @throws IllegalArgumentException If the year is before 1877 or in the future.
     */
    public void setPublicationYear(Year publicationYear) {
        if (publicationYear != null) {
            if ((publicationYear.compareTo(Year.of(1877)) < 0) || (publicationYear.compareTo(Year.now()) > 0)) {
                throw new IllegalArgumentException("Track Publication Year must be prior to the current one");
            }
        }
        this.publicationYear = publicationYear;
    }

    /**
     * Gets the play count of the track.
     *
     * @return The number of times the track has been played.
     */
    public int getPlayCount() {
        return playCount;
    }

    /**
     * Increments the play count of the track.
     *
     */
    public void incrementPlayCount() {
        this.playCount++;
    }

    // -- toString --

    /**
     * Returns a string representation of the track object containing its attributes.
     *
     * @return A formatted detail string.
     */
    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", duration=" + duration +
                ", genre='" + genre + '\'' +
                ", publicationYear=" + publicationYear +
                ", playCount=" + playCount +
                '}';
    }

    // -- hashCode & equals --

    /**
     * Compares this track with another object for equality based strictly on the unique ID.
     *
     * @param o The reference object to compare with.
     * @return true if the IDs match, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(id, track.id);
    }

    /**
     * Generates a hash code value for this track based on its unique ID.
     *
     * @return The generated integer hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}