package it.unisa.gruppo7.musicplayer.command;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.errorHandling.ErrorHandlingStrategy;
import it.unisa.gruppo7.musicplayer.errorHandling.PopupErrorStrategy;

/**
 * Class responsible for invoking commands.
 * Manages the safe execution of commands and their error handling.
 *
 * @author Maxim Makhovskyy
 */
public class CommandInvoker {

    private static final ErrorHandlingStrategy DEFAULT_STRATEGY = new PopupErrorStrategy();

    /**
     * Executes a command using the default error handling strategy.
     *
     * @param command The command to execute.
     * @param <T>     The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public static <T> Optional<T> execute(Command<T> command) {
        return execute(command, DEFAULT_STRATEGY);
    }

    /**
     * Executes a command using a specific error handling strategy.
     *
     * @param command       The command to execute.
     * @param errorStrategy The strategy to use in case of exceptions.
     * @param <T>           The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public static <T> Optional<T> execute(Command<T> command, ErrorHandlingStrategy errorStrategy) {
        try {
            return Optional.ofNullable(command.execute());
        } catch (Exception e) {
            if (errorStrategy != null) {
                errorStrategy.handleError(e);
            }
            return Optional.empty();
        }
    }
}