package it.unisa.gruppo7.musicplayer.command;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.errorHandling.ErrorHandlingStrategy;
import it.unisa.gruppo7.musicplayer.errorHandling.PopupErrorStrategy;
import it.unisa.gruppo7.musicplayer.undo.UndoManager;

/**
 * Invoker of the Command pattern: it asks a {@link Command} to carry out its request,
 * without knowing the receiver behind it.
 * 
 * <p>Two cross-cutting policies are applied around every execution, both delegated to
 * injected collaborators rather than implemented here:</p>
 * <ul>
 *   <li>failures are routed to an {@link ErrorHandlingStrategy};</li>
 *   <li>commands that declare themselves undoable are recorded in an {@link UndoManager},
 *       the caretaker of the undo history.</li>
 * </ul>
 *
 * <p>The command is passed as an argument instead of being held in a field.</p>
 *
 * <p>The {@code static} methods at the bottom are a compatibility facade over a single
 * shared instance, kept for the existing call sites; the invoker proper is the instance,
 * whose collaborators can be supplied by the composition root or by a test.</p>
 *
 * @author Maxim Makhovskyy
 */
public class CommandInvoker {

    /** Caretaker recording the undoable commands executed through this invoker. */
    private final UndoManager undoManager;

    /** Strategy applied when the caller does not supply one explicitly. */
    private final ErrorHandlingStrategy defaultStrategy;

    /** The application-wide invoker used by the static entry points. */
    private static final CommandInvoker DEFAULT =
            new CommandInvoker(new UndoManager(), new PopupErrorStrategy());

    /**
     * Creates an invoker over the given collaborators.
     *
     * @param undoManager     the undo history to record undoable commands into.
     * @param defaultStrategy the error handling strategy used by {@link #invoke(Command)}.
     */
    public CommandInvoker(UndoManager undoManager, ErrorHandlingStrategy defaultStrategy) {
        this.undoManager = undoManager;
        this.defaultStrategy = defaultStrategy;
    }

    /**
     * Executes a command using this invoker's default error handling strategy.
     *
     * @param command The command to execute.
     * @param <T>     The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public <T> Optional<T> invoke(Command<T> command) {
        return invoke(command, defaultStrategy);
    }

    /**
     * Executes a command using a specific error handling strategy.
     *
     * @param command       The command to execute.
     * @param errorStrategy The strategy to use in case of exceptions.
     * @param <T>           The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public <T> Optional<T> invoke(Command<T> command, ErrorHandlingStrategy errorStrategy) {
        try {
            T result = command.execute();
            if (command instanceof UndoableCommand) {
                undoManager.push((UndoableCommand<?>) command);
            }
            return Optional.ofNullable(result);
        } catch (Exception e) {
            if (errorStrategy != null) {
                errorStrategy.handleError(e);
            }
            return Optional.empty();
        }
    }

    /**
     * Returns the undo history this invoker records into.
     * Named differently from the static {@link #getUndoManager()} because a static and an
     * instance method cannot share the same signature.
     *
     * @return the undo manager collaborating with this invoker.
     */
    public UndoManager getUndoHistory() {
        return undoManager;
    }

    /**
     * Executes a command on the shared invoker, using the default error handling strategy.
     *
     * @param command The command to execute.
     * @param <T>     The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public static <T> Optional<T> execute(Command<T> command) {
        return DEFAULT.invoke(command);
    }

    /**
     * Executes a command on the shared invoker, using a specific error handling strategy.
     *
     * @param command       The command to execute.
     * @param errorStrategy The strategy to use in case of exceptions.
     * @param <T>           The return type of the command.
     * @return An Optional containing the result if the execution is successful, otherwise an empty Optional.
     */
    public static <T> Optional<T> execute(Command<T> command, ErrorHandlingStrategy errorStrategy) {
        return DEFAULT.invoke(command, errorStrategy);
    }

    /**
     * Getter for the undo manager of the shared invoker.
     * @return The current instance of the undo manager.
     */
    public static UndoManager getUndoManager() {
        return DEFAULT.getUndoHistory();
    }
}