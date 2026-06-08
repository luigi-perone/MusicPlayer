package it.unisa.gruppo7.musicplayer.errorHandling;

/**
 * Strategy interface for handling exceptions within the application.
 * Defines a common contract for executing various error handling behaviors.
 *
 * @author Maxim Makhovskyy
 */
public interface ErrorHandlingStrategy {

    /**
     * Handles the thrown exception according to the specific implementation strategy.
     *
     * @param e The exception to handle.
     */
    void handleError(Exception e);
}