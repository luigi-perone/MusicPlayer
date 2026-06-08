package it.unisa.gruppo7.musicplayer.command;

/**
 * Generic interface for implementing the Command pattern.
 *
 * @param <T> The return type of the command's execution.
 *
 * @author Maxim Makhovskyy
 */
public interface Command<T> {
    /**
     * Executes the operation encapsulated in the command.
     *
     * @return The result of the execution.
     * @throws Exception If an error occurs during the execution of the command.
     */
    T execute() throws Exception;
}