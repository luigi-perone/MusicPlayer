package it.unisa.gruppo7.musicplayer.command;

/**
 * Specialization of {@link Command} for operations that can be reverted.
 * 
 * A command that implements this interface should capture the the state needed to undo
 * itself. The {@link #undo()} uses the captured state to restore the original state of
 * the application to how it was before the command ran.
 * 
 * @param <T> The return type of the command's execution.
 * 
 * @author Maxim Makhovskyy
 * 
 */
public interface UndoableCommand<T> extends Command<T>{

    /**
     * Revert the state of the application to how it was before the {@link #execute()} call.
     * @throws Exception If the operation cannot be reverted.
     */
    void undo() throws Exception;
}
