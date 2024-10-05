package world.bentobox.bentobox.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 * @since 1.17.3
 */
public class IslandInfo {

    private static final String XZ1 = "[xz1]";
    private static final String RANGE = "[range]";
    private final BentoBox plugin;
    private final Island island;
    private final @Nullable UUID owner;
    private final World world;

    /**
     * Get island Info
     * @param island Island to show info
     */
    public IslandInfo(Island island) {
        this.plugin = BentoBox.getInstance();
        this.island = island;
        this.owner = island.getOwner();
        this.world = island.getWorld();
    }

    /**
     * Shows admin info of this island
     * @param user user asking
     * @param addon Addon executing this command
     */
    public void showAdminInfo(User user, Addon addon) {
        user.sendMessage("commands.admin.info.title");
        user.sendMessage("commands.admin.info.island-uuid", TextVariables.UUID, island.getUniqueId());
        if (owner == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(owner),
                    TextVariables.UUID, owner.toString());

            // Fixes #getLastPlayed() returning 0 when it is the owner's first connection.
            long lastPlayed = (Bukkit.getOfflinePlayer(owner).getLastPlayed() != 0)
                    ? Bukkit.getOfflinePlayer(owner).getLastPlayed()
                    : Bukkit.getOfflinePlayer(owner).getFirstPlayed();
            String formattedDate;
            try {
                String dateTimeFormat = plugin.getLocalesManager()
                        .get("commands.admin.info.last-login-date-time-format");
                formattedDate = new SimpleDateFormat(dateTimeFormat).format(new Date(lastPlayed));
            } catch (Exception ignored) {
                formattedDate = new Date(lastPlayed).toString();
            }
            user.sendMessage("commands.admin.info.last-login", "[date]", formattedDate);

            user.sendMessage("commands.admin.info.deaths", TextVariables.NUMBER,
                    String.valueOf(plugin.getPlayers().getDeaths(world, owner)));
            String resets = String.valueOf(plugin.getPlayers().getResets(world, owner));
            String total = plugin.getIWM().getResetLimit(world) < 0 ? "Unlimited"
                    : String.valueOf(plugin.getIWM().getResetLimit(world));
            user.sendMessage("commands.admin.info.resets-left", TextVariables.NUMBER, resets, "[total]", total);
            // Show team members
            showMembers(user);
        }
        Vector location = island.getProtectionCenter().toVector();
        user.sendMessage("commands.admin.info.island-protection-center", TextVariables.XYZ, Util.xyz(location));
        user.sendMessage("commands.admin.info.island-center", TextVariables.XYZ,
                Util.xyz(island.getCenter().toVector()));
        user.sendMessage("commands.admin.info.island-coords", XZ1,
                Util.xyz(new Vector(island.getMinX(), 0, island.getMinZ())), "[xz2]",
                Util.xyz(new Vector(island.getMaxX(), 0, island.getMaxZ())));
        user.sendMessage("commands.admin.info.protection-range", RANGE, String.valueOf(island.getProtectionRange()));
        if (!island.getBonusRanges().isEmpty()) {
            user.sendMessage("commands.admin.info.protection-range-bonus-title");
        }
        island.getBonusRanges().forEach(brb -> {
            if (brb.getMessage().isBlank()) {
                user.sendMessage("commands.admin.info.protection-range-bonus", TextVariables.NUMBER,
                        String.valueOf(brb.getRange()));
            } else {
                user.sendMessage(brb.getMessage(), TextVariables.NUMBER, String.valueOf(brb.getRange()));
            }
        });
        user.sendMessage("commands.admin.info.max-protection-range", RANGE,
                String.valueOf(island.getMaxEverProtectionRange()));
        user.sendMessage("commands.admin.info.protection-coords", XZ1,
                Util.xyz(new Vector(island.getMinProtectedX(), 0, island.getMinProtectedZ())), "[xz2]",
                Util.xyz(new Vector(island.getMaxProtectedX() - 1, 0, island.getMaxProtectedZ() - 1)));
        if (island.isSpawn()) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        if (!island.getBanned().isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            island.getBanned().forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME,
                    plugin.getPlayers().getName(u)));
        }
        if (island.getPurgeProtected()) {
            user.sendMessage("commands.admin.info.purge-protected");
        }
        // Show bundle info if available
        island.getMetaData("bundle").ifPresent(mdv -> {
            user.sendMessage("commands.admin.info.bundle", TextVariables.NAME, mdv.asString());
        });
        // Fire info event to allow other addons to add to info
        IslandEvent.builder().island(island).location(island.getCenter()).reason(IslandEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId()).addon(addon).admin(true).build();
    }

    /**
     * Shows info of this island to this user.
     * @param user the User who is requesting it
     * @return always true
     */
    public boolean showInfo(User user) {
        user.sendMessage("commands.admin.info.title");
        if (owner == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(owner),
                    TextVariables.UUID, owner.toString());
            user.sendMessage("commands.admin.info.deaths", TextVariables.NUMBER,
                    String.valueOf(plugin.getPlayers().getDeaths(world, owner)));
            String resets = String.valueOf(plugin.getPlayers().getResets(world, owner));
            String total = plugin.getIWM().getResetLimit(world) < 0 ? "Unlimited"
                    : String.valueOf(plugin.getIWM().getResetLimit(world));
            user.sendMessage("commands.admin.info.resets-left", TextVariables.NUMBER, resets, "[total]", total);
            // Show team members
            showMembers(user);
        }
        int maxHomes = island.getMaxHomes() == null ? plugin.getIWM().getMaxHomes(island.getWorld())
                : island.getMaxHomes();
        user.sendMessage("commands.admin.info.max-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
        Vector location = island.getProtectionCenter().toVector();
        user.sendMessage("commands.admin.info.island-center", TextVariables.XYZ, Util.xyz(location));
        user.sendMessage("commands.admin.info.protection-range", RANGE, String.valueOf(island.getProtectionRange()));
        user.sendMessage("commands.admin.info.protection-coords", XZ1,
                Util.xyz(new Vector(island.getMinProtectedX(), 0, island.getMinProtectedZ())), "[xz2]",
                Util.xyz(new Vector(island.getMaxProtectedX() - 1, 0, island.getMaxProtectedZ() - 1)));
        if (island.isSpawn()) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        if (!island.getBanned().isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            island.getBanned().forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME,
                    plugin.getPlayers().getName(u)));
        }
        // Fire info event
        IslandEvent.builder().island(island).location(island.getCenter()).reason(IslandEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId()).build();
        return true;
    }

    /**
     * Shows the members of this island to this user.
     * @param user the User who is requesting it
     */
    public void showMembers(User user) {
        user.sendMessage("commands.admin.info.team-members-title");
        island.getMembers().forEach((u, i) -> {
            if (owner.equals(u)) {
                user.sendMessage("commands.admin.info.team-owner-format", TextVariables.NAME,
                        plugin.getPlayers().getName(u), "[rank]",
                        user.getTranslation(RanksManager.getInstance().getRank(i)));
            } else if (i > RanksManager.VISITOR_RANK) {
                user.sendMessage("commands.admin.info.team-member-format", TextVariables.NAME,
                        plugin.getPlayers().getName(u), "[rank]",
                        user.getTranslation(RanksManager.getInstance().getRank(i)));
            }
        });
    }
}
