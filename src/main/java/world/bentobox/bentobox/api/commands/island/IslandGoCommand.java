package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * @author tastybento
 */
public class IslandGoCommand extends DelayedTeleportCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.go.parameters");
        setDescription("commands.island.go.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if mid-teleport
        if (getIslands().isGoingHome(user)) {
            // Tell them again that it's in progress
            user.sendMessage("commands.island.go.teleport");
            return false;
        }
        Set<Island> islands = getIslands().getIslands(getWorld(), user.getUniqueId());
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check if the island is reserved
        if (checkReserved(user, islands)) {
            return false;
        }
        // Prevent command if player is falling and its not allowed
        if ((getIWM().inWorld(user.getWorld()) && Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()))
                && user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check if the home is known
        if (!args.isEmpty()) {
            BentoBox.getInstance().logDebug("An arg has been supplied");
            Map<String, IslandInfo> names = getNameIslandMap(user);
            final String name = String.join(" ", args);
            if (!names.containsKey(name)) {
                BentoBox.getInstance().logDebug("Unknown arg");
                // Failed home name check
                user.sendMessage("commands.island.go.unknown-home");
                user.sendMessage("commands.island.sethome.homes-are");
                names.keySet().forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
                return false;
            } else {
                names.entrySet().forEach(n -> BentoBox.getInstance().logDebug(n.getKey() + " " + n.getValue().islandName + " " + n.getValue().island.getProtectionCenter()));
                IslandInfo info = names.get(name);
                getIslands().setPrimaryIsland(user.getUniqueId(), info.island);
                if (info.islandName) {
                    BentoBox.getInstance().logDebug("Teleporting to island");
                    this.delayCommand(user, () -> {
                        new SafeSpotTeleport.Builder(getPlugin())
                        .entity(user.getPlayer())
                        .location(getIslands().getHomeLocation(info.island))
                        .thenRun(() -> user.sendMessage("general.success"))
                        .build();
                        //getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), "");
                    });
                } else {
                    BentoBox.getInstance().logDebug("Teleporting to a home: " + name);
                    this.delayCommand(user, () -> new SafeSpotTeleport.Builder(getPlugin())
                            .entity(user.getPlayer())
                            .location(getIslands().getHomeLocation(info.island, name))
                            .thenRun(() -> user.sendMessage("general.success"))
                            .build());
                }
            }
        } else {
            BentoBox.getInstance().logDebug("Normal home go");
            this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer()));
        }
        return true;
    }

    private boolean checkReserved(User user, Set<Island> islands) {
        for (Island island : islands) {
            if (island.isReserved()) {
                // Send player to create an island
                getParent().getSubCommand("create").ifPresent(createCmd -> createCmd.call(user, createCmd.getLabel(), Collections.emptyList()));
                return true;
            }
        }
        return false;
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";

        return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(user).keySet()), lastArg));

    }

    private record IslandInfo(Island island, boolean islandName) {};

    private Map<String, IslandInfo> getNameIslandMap(User user) {
        Map<String, IslandInfo> islandMap = new HashMap<>();
        int index = 0;
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            index++;
            BentoBox.getInstance().logDebug("Processing island " + index + " at " + island.getProtectionCenter());
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                islandMap.put(island.getName(), new IslandInfo(island, true));
                BentoBox.getInstance().logDebug("name has been set " + island.getName());
            } else {
                // Name has not been set
                String text = user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName()) + " " + index;
                islandMap.put(text, new IslandInfo(island, true));
                BentoBox.getInstance().logDebug("Name has not been set. Giving " + index + " the temp name: " + text);
            }
            // Add homes. Homes do not need an island specified
            BentoBox.getInstance().logDebug("Adding homes for island " + index);
            island.getHomes().keySet().forEach(n -> islandMap.put(n, new IslandInfo(island, false)));
        }
        BentoBox.getInstance().logDebug("\n\n\n");

        return islandMap;

    }

}
