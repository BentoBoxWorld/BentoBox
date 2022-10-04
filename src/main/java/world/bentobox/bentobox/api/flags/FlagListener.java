package world.bentobox.bentobox.api.flags;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.admin.AdminSwitchCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Abstract class for flag listeners. Provides common code.
 * @author tastybento
 */
public abstract class FlagListener implements Listener {

    /**
     * Reason for why flag was allowed or disallowed
     * Used by admins for debugging player actions
     */
    protected enum Why {
        UNPROTECTED_WORLD,
        OP,
        BYPASS_EVERYWHERE,
        BYPASS_ISLAND,
        RANK_ALLOWED,
        ALLOWED_IN_WORLD,
        ALLOWED_ON_ISLAND,
        NOT_ALLOWED_ON_ISLAND,
        NOT_ALLOWED_IN_WORLD,
        ERROR_NO_ASSOCIATED_USER,
        NOT_SET,
        SETTING_ALLOWED_ON_ISLAND,
        SETTING_NOT_ALLOWED_ON_ISLAND,
        SETTING_ALLOWED_IN_WORLD,
        SETTING_NOT_ALLOWED_IN_WORLD,
        NULL_LOCATION
    }

    @NonNull
    private BentoBox plugin = BentoBox.getInstance();
    @Nullable
    private User user = null;

    /**
     * @return the plugin
     */
    @NonNull
    public BentoBox getPlugin() {
        return plugin;
    }

    /**
     * Used for unit testing only to set the plugin
     * @param plugin - plugin object
     */
    public void setPlugin(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /*
     * The following methods cover the cancellable events and enable a simple noGo(e) to be used to cancel and send the error message
     */

    /**
     * Cancels the event and sends the island public message to user
     * @param e - event
     * @param flag - the flag that has been checked
     */
    public void noGo(@NonNull Event e, @NonNull Flag flag) {
        noGo(e, flag, false, "protection.protected");
    }

    /**
     * Cancels the event and sends the island protected message to user unless silent is true
     * @param e - event
     * @param flag - the flag that has been checked
     * @param silent - if true, message is not sent
     * @param string - translation reference
     */
    public void noGo(@NonNull Event e, @NonNull Flag flag, boolean silent, String string) {
        if (e instanceof Cancellable) {
            ((Cancellable)e).setCancelled(true);
        }
        if (user != null && !silent) {
            user.notify(string, TextVariables.DESCRIPTION, user.getTranslation(flag.getHintReference()));
        }
    }

    /**
     * Check if flag is allowed at location. Uses player object because Bukkit events provide player.
     * @param e - event
     * @param player - player affected by this flag, or null if none
     * @param loc - location, will return true if null
     * @param flag - flag {@link world.bentobox.bentobox.lists.Flags}
     * @return true if allowed, false if not
     */
    public boolean checkIsland(@NonNull Event e, @Nullable Player player, @Nullable Location loc, @NonNull Flag flag) {
        return checkIsland(e, player, loc, flag, false);
    }

    /**
     * Check if flag is allowed at location
     * @param e - event
     * @param player - player affected by this flag, or null if none
     * @param loc - location, will return true if null
     * @param flag - flag {@link world.bentobox.bentobox.lists.Flags}
     * @param silent - if true, no attempt is made to tell the user
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(@NonNull Event e, @Nullable Player player, @Nullable Location loc, @NonNull Flag flag, boolean silent) {
        // Set user
        user = player == null ? null : User.getInstance(player);
        if (loc == null) {
            if (user != null && user.getLocation() != null && user.getLocation().getWorld() != null) {
                report(user, e, user.getLocation(), flag, Why.NULL_LOCATION);
            }
            return true;
        }

        // If this is not an Island World or a standard Nether or End, skip
        if (!plugin.getIWM().inWorld(loc)) {
            report(user, e, loc, flag, Why.UNPROTECTED_WORLD);
            return true;
        }

        // Get the island and if present
        Optional<Island> island = getIslands().getProtectedIslandAt(loc);
        // Handle Settings Flag
        if (flag.getType().equals(Flag.Type.SETTING)) {
            // If the island exists, return the setting, otherwise return the default setting for this flag
            if (island.isPresent()) {
                report(user, e, loc, flag,  island.map(x -> x.isAllowed(flag)).orElse(false) ? Why.SETTING_ALLOWED_ON_ISLAND : Why.SETTING_NOT_ALLOWED_ON_ISLAND);
            } else {
                report(user, e, loc, flag,  flag.isSetForWorld(loc.getWorld()) ? Why.SETTING_ALLOWED_IN_WORLD : Why.SETTING_NOT_ALLOWED_IN_WORLD);
            }
            return island.map(x -> x.isAllowed(flag)).orElseGet(() -> flag.isSetForWorld(loc.getWorld()));
        }

        // Protection flag

        // Ops or "bypass everywhere" moderators can do anything unless they have switched it off
        if (!user.getMetaData(AdminSwitchCommand.META_TAG).map(MetaDataValue::asBoolean).orElse(false)
                && (user.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypassprotect")
                        || user.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypass." + flag.getID() + ".everywhere"))) {
            if (user.isOp()) {
                report(user, e, loc, flag,  Why.OP);
            } else {
                report(user, e, loc, flag,  Why.BYPASS_EVERYWHERE);
            }
            return true;
        }

        // Handle World Settings
        if (flag.getType().equals(Flag.Type.WORLD_SETTING)) {
            if (flag.isSetForWorld(loc.getWorld())) {
                report(user, e, loc, flag,  Why.ALLOWED_IN_WORLD);
                return true;
            }
            report(user, e, loc, flag,  Why.NOT_ALLOWED_IN_WORLD);
            noGo(e, flag, silent, "protection.world-protected");
            return false;
        }

        // Check if the plugin is set in User (required for testing)
        User.setPlugin(plugin);

        if (island.isPresent()) {
            // If it is not allowed on the island, "bypass island" moderators can do anything
            if (island.get().isAllowed(user, flag)) {
                report(user, e, loc, flag,  Why.RANK_ALLOWED);
                return true;
            } else if (!user.getMetaData(AdminSwitchCommand.META_TAG).map(MetaDataValue::asBoolean).orElse(false)
                    && (user.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypass." + flag.getID() + ".island"))) {
                report(user, e, loc, flag,  Why.BYPASS_ISLAND);
                return true;
            }
            report(user, e, loc, flag,  Why.NOT_ALLOWED_ON_ISLAND);
            noGo(e, flag, silent, island.get().isSpawn() ? "protection.spawn-protected" : "protection.protected");
            return false;
        }
        // The player is in the world, but not on an island, so general world settings apply
        if (flag.isSetForWorld(loc.getWorld())) {
            report(user, e, loc, flag,  Why.ALLOWED_IN_WORLD);
            return true;
        } else {
            report(user, e, loc, flag,  Why.NOT_ALLOWED_IN_WORLD);
            noGo(e, flag, silent, "protection.world-protected");
            return false;
        }
    }

    /**
     * Report why something did or did not happen for the admin why command
     * @param user user involved
     * @param e associated event
     * @param loc location
     * @param flag flag
     * @param why reason enum
     */
    protected void report(@Nullable User user, @NonNull Event e, @NonNull Location loc, @NonNull Flag flag, @NonNull Why why) {
        // A quick way to debug flag listener unit tests is to add this line here: System.out.println(why.name()); NOSONAR
        if (user != null && user.isPlayer() && user.getPlayer().getMetadata(loc.getWorld().getName() + "_why_debug").stream()
                .filter(p -> p.getOwningPlugin().equals(getPlugin())).findFirst().map(MetadataValue::asBoolean).orElse(false)) {
            String whyEvent = "Why: " + e.getEventName() + " in world " + loc.getWorld().getName() + " at " + Util.xyz(loc.toVector());
            String whyBypass = "Why: " + user.getName() + " " + flag.getID() + " - " + why.name();

            plugin.log(whyEvent);
            plugin.log(whyBypass);

            // See if there is a player that issued the debug
            String issuerUUID = user.getPlayer().getMetadata(loc.getWorld().getName() + "_why_debug_issuer").stream()
                    .filter(p -> getPlugin().equals(p.getOwningPlugin())).findFirst().map(MetadataValue::asString).orElse("");
            if (!issuerUUID.isEmpty()) {
                User issuer = User.getInstance(UUID.fromString(issuerUUID));
                if (issuer != null && issuer.isPlayer()) {
                    user.sendRawMessage(whyEvent);
                    user.sendRawMessage(whyBypass);
                }
            }
        }
    }

    /**
     * Get the flag for this ID
     * @param id the flag ID
     * @return Optional of the Flag denoted by the id
     * @since 1.1
     */
    @NonNull
    protected Optional<Flag> getFlag(@NonNull String id) {
        return plugin.getFlagsManager().getFlag(id);
    }

    /**
     * Get the island database manager
     * @return the island database manager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }

    /**
     * Get the island database manager
     * @return the island database manager
     */
    protected IslandsManager getIslandsManager() {
        return plugin.getIslands();
    }

    /**
     * Get the island world manager
     * @return Island World Manager
     */
    protected IslandWorldManager getIWM() {
        return plugin.getIWM();
    }

    /**
     * Check if PVP is allowed here or not
     * @param location location where action is taking
     * @return true if PVP is allowed, false if not
     */
    protected boolean PVPAllowed(Location location) {
        return plugin.getIslands().getIslandAt(location).map(i -> i.isAllowed(this.getFlag(location.getWorld()))).orElse(false);
    }

    protected Flag getFlag(World w) {
        return switch (w.getEnvironment()) {
        case NETHER -> Flags.PVP_NETHER;
        case THE_END -> Flags.PVP_END;
        default -> Flags.PVP_OVERWORLD;
        };
    }

}
