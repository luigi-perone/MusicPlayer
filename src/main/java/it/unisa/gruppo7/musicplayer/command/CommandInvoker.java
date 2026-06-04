package it.unisa.gruppo7.musicplayer.command;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.errorHandling.ErrorHandlingStrategy;
import it.unisa.gruppo7.musicplayer.errorHandling.PopupErrorStrategy;

public class CommandInvoker {

    private static final ErrorHandlingStrategy DEFAULT_STRATEGY = new PopupErrorStrategy();

    public static <T> Optional<T> execute(Command<T> command) {
        return execute(command, DEFAULT_STRATEGY);
    }

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
