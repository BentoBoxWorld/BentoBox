/**
 * API for BentoBox commands
 */
/**
 * <p>
 * The workhorse class is the abstract class CompositeCommand. It provides all the functionality for
 * a command including automatic help, sub-commands, convenience methods, etc. See examples of how to use
 * it in the sub-folders admin and island.
 * </p>
 * <p>
 * The package also includes abstract confirmable command and delayed teleport command classes. These can
 * be extended for commands that need them. There is also a default help command. Commands can implement
 * their own custom help if required, but most of the time it is not.
 * </p>
 * @author tastybento
 *
 */
package world.bentobox.bentobox.api.commands;