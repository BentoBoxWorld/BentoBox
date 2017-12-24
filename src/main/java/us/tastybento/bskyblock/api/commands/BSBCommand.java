package us.tastybento.bskyblock.api.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Interface for BSkyBlock Commands
 * @author ben
 *
 */
public interface BSBCommand {

    /**
     * Anything that needs to be set up for this command.
     * This is where you register subcommands. This will be run if it exists.
     */
    default void setup() {};

    /**
     * What will be executed when this command is run
     * @param user
     * @param list
     * @return true or false - true if the command executed successfully
     */
    public abstract boolean execute(User user, List<String> list);

    /**
     * Tab Completer for CompositeCommands. Note that any registered sub-commands will be automatically
     * added to the list must not be manually added. Use this to add tab-complete for things like names.
     * @param sender
     * @param alias
     * @param args
     * @return List of strings that could be used to complete this command.
     */
     default Optional<List<String>> tabComplete(User user, String alias, LinkedList<String> args) {
        return Optional.empty();
    }

}
