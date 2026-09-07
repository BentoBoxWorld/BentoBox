package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Sets a player's rank on an island directly, which covers both promotion and demotion.
 * <p>
 * Unlike the player-facing {@code /island team promote} and {@code demote} commands, this does
 * not step one rank at a time and does not depend on the rank of the admin running it. It is
 * intentionally not restricted to in-game players so that server staff can manage teams from the
 * console. The island is identified in one of three ways:
 * <ul>
 * <li>nothing - the island the target is a team member of, if there is exactly one</li>
 * <li>the name of the island's owner</li>
 * <li>the island's centre as {@code x,y,z}</li>
 * </ul>
 * Any rank between visitor and owner may be set, so this also grants or revokes coop and trusted
 * status. Owner rank is refused - ownership is transferred with {@code setowner}, which keeps the
 * island's owner field and the rank map in step.
 *
 * @author tastybento
 */
public class AdminSetrankCommand extends CompositeCommand {

    private static final String RANK_PREFIX = "ranks.";

    private int rankValue;
    private @Nullable UUID targetUUID;
    private @Nullable Island island;

    public AdminSetrankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "setrank");
    }

    @Override
    public void setup() {
        setPermission("admin.setrank");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.setrank.parameters");
        setDescription("commands.admin.setrank.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Syntax: <player> <rank> [island owner | x,y,z]
        if (args.size() != 2 && args.size() != 3) {
            showHelp(this, user);
            return false;
        }
        // Target
        targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Rank
        Optional<Integer> rank = parseRank(user, args.get(1));
        if (rank.isEmpty()) {
            user.sendMessage("commands.admin.setrank.unknown-rank", TextVariables.RANK, args.get(1), "[ranks]",
                    String.join(", ", getAllowedRankNames()));
            return false;
        }
        rankValue = rank.get();
        if (rankValue <= RanksManager.VISITOR_RANK) {
            user.sendMessage("commands.admin.setrank.not-possible");
            return false;
        }
        if (rankValue >= RanksManager.OWNER_RANK) {
            user.sendMessage("commands.admin.setrank.cannot-set-owner");
            return false;
        }
        // Island
        island = args.size() == 2 ? findTargetIsland(user) : findNamedIsland(user, args.get(2));
        if (island == null) {
            return false;
        }
        if (targetUUID.equals(island.getOwner())) {
            user.sendMessage("commands.admin.setrank.cannot-set-owner");
            return false;
        }
        if (island.getRank(targetUUID) == rankValue) {
            user.sendMessage("commands.admin.setrank.already-rank", TextVariables.NAME, args.getFirst(),
                    TextVariables.RANK, user.getTranslation(RanksManager.getInstance().getRank(rankValue)));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(island);
        Objects.requireNonNull(targetUUID);
        User target = User.getInstance(targetUUID);
        int currentRank = island.getRank(targetUUID);
        String ownerName = getPlayers().getName(island.getOwner());

        island.setRank(targetUUID, rankValue);
        IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(true)
                .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(currentRank, rankValue).build();

        user.sendMessage("commands.admin.setrank.rank-set", "[from]",
                user.getTranslation(RanksManager.getInstance().getRank(currentRank)), "[to]",
                user.getTranslation(RanksManager.getInstance().getRank(rankValue)), TextVariables.NAME, ownerName);
        if (target.isOnline()) {
            target.sendMessage("commands.admin.setrank.admin-changed-rank", TextVariables.RANK,
                    target.getTranslation(RanksManager.getInstance().getRank(rankValue)), TextVariables.NAME,
                    ownerName);
        }
        return true;
    }

    /**
     * Resolves a rank argument. Accepts the rank keyword ({@code member}, {@code sub-owner}, or any
     * addon-registered rank's reference without the {@code ranks.} prefix), the rank's translated
     * name in the caller's locale, or the numeric rank value.
     * @return the rank value, if the argument names any known rank
     */
    private Optional<Integer> parseRank(User user, String arg) {
        String wanted = arg.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> en : RanksManager.getInstance().getRanks().entrySet()) {
            String translated = Util.stripColor(user.getTranslation(en.getKey())).toLowerCase(Locale.ROOT);
            if (wanted.equals(keyword(en.getKey())) || wanted.equals(translated)
                    || wanted.equals(String.valueOf(en.getValue()))) {
                return Optional.of(en.getValue());
            }
        }
        return Optional.empty();
    }

    private static String keyword(String reference) {
        String key = reference.startsWith(RANK_PREFIX) ? reference.substring(RANK_PREFIX.length()) : reference;
        return key.toLowerCase(Locale.ROOT);
    }

    /**
     * @return the keywords of every rank this command can set, lowest first
     */
    private static List<String> getAllowedRankNames() {
        return RanksManager.getInstance().getRanks().entrySet().stream()
                .filter(en -> en.getValue() > RanksManager.VISITOR_RANK && en.getValue() < RanksManager.OWNER_RANK)
                .sorted(Map.Entry.comparingByValue()).map(en -> keyword(en.getKey())).toList();
    }

    /**
     * No island was named: use the one team island the target belongs to but does not own.
     */
    private @Nullable Island findTargetIsland(User user) {
        List<Island> memberOf = getIslands().getIslands(getWorld(), targetUUID).stream()
                .filter(i -> !Objects.equals(targetUUID, i.getOwner())).toList();
        if (memberOf.isEmpty()) {
            if (getIslands().hasIsland(getWorld(), targetUUID)) {
                user.sendMessage("commands.admin.setrank.cannot-set-owner");
            } else {
                user.sendMessage("general.errors.player-has-no-island");
            }
            return null;
        }
        if (memberOf.size() > 1) {
            user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
            memberOf.forEach(i -> user.sendMessage("commands.admin.unregister.errors.specify-island-location",
                    TextVariables.XYZ, Util.xyz(i.getCenter().toVector())));
            return null;
        }
        return memberOf.getFirst();
    }

    /**
     * An island was named, either by its x,y,z centre or by the name of its owner.
     */
    private @Nullable Island findNamedIsland(User user, String name) {
        Optional<Location> coords = parseXYZ(name);
        if (coords.isPresent()) {
            Optional<Island> at = getIslands().getIslandAt(coords.get());
            if (at.isEmpty()) {
                user.sendMessage("commands.admin.unregister.errors.unknown-island-location");
                return null;
            }
            return at.get();
        }
        UUID ownerUUID = Util.getUUID(name);
        if (ownerUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, name);
            return null;
        }
        List<Island> owned = getIslands().getIslands(getWorld(), ownerUUID).stream()
                .filter(i -> ownerUUID.equals(i.getOwner())).toList();
        if (owned.isEmpty()) {
            user.sendMessage("general.errors.player-is-not-owner", TextVariables.NAME, name);
            return null;
        }
        if (owned.size() > 1) {
            // Concurrent islands: the owner alone is ambiguous, so ask for the centre
            user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
            owned.forEach(i -> user.sendMessage("commands.admin.unregister.errors.specify-island-location",
                    TextVariables.XYZ, Util.xyz(i.getCenter().toVector())));
            return null;
        }
        return owned.getFirst();
    }

    /**
     * @return a location in this command's world if the argument is in {@code x,y,z} form
     */
    private Optional<Location> parseXYZ(String arg) {
        String[] parts = arg.split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Location(getWorld(), Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, @NonNull List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.size() == 1) {
            return Optional.of(Util.getOnlinePlayerList(user));
        }
        if (args.size() == 2) {
            return Optional.of(Util.tabLimit(getAllowedRankNames(), lastArg));
        }
        if (args.size() == 3) {
            // Island owners, plus the centres of the islands the target already belongs to
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            UUID targetId = getPlayers().getUUID(args.getFirst());
            if (targetId != null) {
                getIslands().getIslands(getWorld(), targetId).stream().map(i -> Util.xyz(i.getCenter().toVector()))
                        .forEach(options::add);
            }
            return Optional.of(Util.tabLimit(options, lastArg));
        }
        return Optional.empty();
    }
}
