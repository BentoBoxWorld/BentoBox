/**
 * API for BentoBox Users
 *
 * <p>
 * BentoBox has extended the Bukkit Player class to become a User with the primary reason
 * to enable localized messaging. Apart from that a User can be an OfflinePlayer, a Player,
 * or even the Console. Users are used throughout the BentoBox code base, e.g., for commands
 * instead of Players. It is possible to get a Player from a User if the User is a player.
 * </p>
 * <p>
 * Notifier is a special kind of messaging class that prevents spamming of messages to a
 * user in chat. It is useful for cases when the same message may be generated many times, e.g., in
 * a protection scenario.
 * </p>
 * @author tastybento
 *
 */
package world.bentobox.bentobox.api.user;