package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.panels.IslandCreationPanel;

/**
 * /island create - Create an island.
 *
 * @author tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * @param islandCommand - parent command
     */
    public IslandCreateCommand(CompositeCommand islandCommand) {
        super(islandCommand, "create", "new");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.create.parameters");
        setDescription("commands.island.create.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if the island is reserved
        @Nullable
        Island island = getIslands().getIsland(getWorld(), user);
        if (island != null) {
            // Reserved islands can be made
            if (island.isReserved()) {
                return true;
            }
            // You cannot make an island
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIWM().getMaxIslands(getWorld()) > 0
                && getIslands().getIslandCount(getWorld()) >= getIWM().getMaxIslands(getWorld())) {
            // There is too many islands in the world :(
            user.sendMessage("commands.island.create.too-many-islands");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), args.get(0).toLowerCase(java.util.Locale.ENGLISH));
            if (name == null) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, args.get(0))) {
                return false;
            }
            // Make island
            return makeIsland(user, name);
        } else {
            // Show panel only if there are multiple bundles available
            if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).size() > 1) {
                // Show panel
                IslandCreationPanel.openPanel(this, user, label);
                return true;
            }
            return makeIsland(user, BlueprintsManager.DEFAULT_BUNDLE_NAME);
        }
    }

    private boolean makeIsland(User user, String name) {
        user.sendMessage("commands.island.create.creating-island");
        // Make the slime world
        if (getPlugin().getSwm() != null) {
            System.out.println("Got slimeworld");
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                try {
                    // Create a new and empty property map
                    SlimePropertyMap properties = new SlimePropertyMap();
                    properties.setString(SlimeProperties.DIFFICULTY, "normal");
                    properties.setInt(SlimeProperties.SPAWN_X, 0);
                    properties.setInt(SlimeProperties.SPAWN_Y, 125);
                    properties.setInt(SlimeProperties.SPAWN_Z, 0);
                    // Note that this method should be called asynchronously
                    System.out.println("Making slime world");
                    // Get name
                    String worldName = makeName();
                    SlimeWorld world = getPlugin().getSwm().createEmptyWorld(getPlugin().getSlimeLoader(),
                            worldName, false,  properties);
                    System.out.println("Made world");
                    // This method must be called synchronously
                    Bukkit.getScheduler().runTask(getPlugin(), () -> {
                        getPlugin().getSwm().generateWorld(world);
                        Bukkit.getScheduler().runTask(getPlugin(), () -> {
                            try {
                                World newWorld = Bukkit.getWorld(worldName);
                                getPlugin().getIWM().addWorld(newWorld, getAddon());
                                getPlayers().addWorld(user.getUniqueId(), worldName, getAddon());
                                System.out.println("Building island");
                                NewIsland.builder()
                                .player(user)
                                .addon(getAddon())
                                .reason(Reason.CREATE)
                                .name(name)
                                .world(newWorld)
                                .locationStrategy(w -> new Location(w, 0,120,0)) // TODO fix locs
                                .build();
                            } catch (IOException e) {
                                getPlugin().logError("Could not create island for player. " + e.getMessage());
                                user.sendMessage(e.getMessage());
                            }
                            if (getSettings().isResetCooldownOnCreate()) {
                                getParent().getSubCommand("reset").ifPresent(resetCommand -> resetCommand.setCooldown(user.getUniqueId(), getSettings().getResetCooldown()));
                            }
                        });

                    });
                } catch (Exception ex) {
                    /* Exception handling */
                }
            });
        }

        return true;
    }

    private String makeName() {
        String name = UUID.randomUUID().toString().replace("-", "").substring(0,16);
        if (Bukkit.getWorld(name) != null) return makeName();
        return name;
    }
}
