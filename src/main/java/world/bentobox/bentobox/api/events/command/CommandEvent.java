package world.bentobox.bentobox.api.events.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;

import world.bentobox.bentobox.api.events.PremadeEvent;

/**
 * Fired when a team event happens.
 *
 * @author tastybento
 * @since 1.0
 */
public class CommandEvent extends PremadeEvent implements Cancellable {

    private boolean cancelled;

    private final CommandSender sender;
    private final Command command;
    private final String label;
    private final String[] args;

    private CommandEvent(CommandSender sender, Command command, String label, String[] args) {
        super();
        this.sender = sender;
        this.command = command;
        this.label = label;
        this.args = args;
    }

    public static CommandEventBuilder builder() {
        return new CommandEventBuilder();
    }

    public static class CommandEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private CommandSender sender;
        private Command command;
        private String label;
        private String[] args;

        public CommandEventBuilder setSender(CommandSender sender) {
            this.sender = sender;
            return this;
        }

        public CommandEventBuilder setCommand(Command command) {
            this.command = command;
            return this;
        }

        public CommandEventBuilder setLabel(String label) {
            this.label = label;
            return this;
        }

        public CommandEventBuilder setArgs(String[] args) {
            this.args = args;
            return this;
        }

        public CommandEvent build() {
            return new CommandEvent(sender, command, label, args);
        }

    }

    public CommandSender getSender() {
        return sender;
    }

    public Command getCommand() {
        return command;
    }

    public String getLabel() {
        return label;
    }

    public String[] getArgs() {
        return args;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean arg0) {
        cancelled = arg0;
    }
}
