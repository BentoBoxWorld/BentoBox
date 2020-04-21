//
// Created by BONNe
// Copyright - 2020
//


package world.bentobox.bentobox.api.commands.admin;


import org.bukkit.World;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


/**
 * This command sets spawn point for island at admin location for island on which admin is located.
 * This command is only for player entity.
 * @author BONNe
 * @since 1.13.0
 */
public class AdminSetSpawnPointCommand extends ConfirmableCommand
{
	/**
	 * Sub-command constructor
	 *
	 * @param parent - the parent composite command
	 */
	public AdminSetSpawnPointCommand(CompositeCommand parent)
	{
		super(parent, "setspawnpoint");
	}


	/**
	 * Setups anything that is needed for this command. <br/><br/> It is recommended you do the following in this
	 * method:
	 * <ul>
	 *     <li>Register any of the sub-commands of this command;</li>
	 *     <li>Define the permission required to use this command using {@link CompositeCommand#setPermission(String)};</li>
	 *     <li>Define whether this command can only be run by players or not using {@link CompositeCommand#setOnlyPlayer(boolean)};</li>
	 * </ul>
	 */
	@Override
	public void setup()
	{
		this.setPermission("admin.setspawnpoint");
		this.setOnlyPlayer(true);
		this.setDescription("commands.admin.setspawnpoint.description");
	}


	/**
	 * Defines what will be executed when this command is run.
	 *
	 * @param user the {@link User} who is executing this command.
	 * @param label the label which has been used to execute this command. It can be {@link CompositeCommand#getLabel()}
	 * or an alias.
	 * @param args the command arguments.
	 * @return {@code true} if the command executed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean execute(User user, String label, List<String> args)
	{
		Optional<Island> optionalIsland = this.getIslands().getIslandAt(user.getLocation());

		if (optionalIsland.isPresent() &&
			(optionalIsland.get().hasNetherIsland() ||
			!World.Environment.NETHER.equals(user.getLocation().getWorld().getEnvironment())) &&
			(optionalIsland.get().hasEndIsland() ||
			!World.Environment.THE_END.equals(user.getLocation().getWorld().getEnvironment())))
		{
			// Everything's fine, we can set the location as spawn point for island :)
			this.askConfirmation(user, user.getTranslation("commands.admin.setspawnpoint.confirmation"),
				() -> this.setSpawnPoint(user, optionalIsland.get()));

			return true;
		}
		else
		{
			user.sendMessage("commands.admin.setspawnpoint.no-island-here");
			return false;
		}
	}


	/**
	 * This method changes spawn point for island at given user location.
	 * @param user User who initiate spawn point change.
	 * @param island Island which is targeted by user.
	 */
	private void setSpawnPoint(User user, Island island)
	{
		island.setSpawnPoint(Objects.requireNonNull(user.getLocation().getWorld()).getEnvironment(),
			user.getLocation());
		user.sendMessage("commands.admin.setspawnpoint.success");

		if (!island.isSpawn())
		{
			island.getPlayersOnIsland().forEach(player ->
				User.getInstance(player).sendMessage("commands.admin.setspawnpoint.island-spawnpoint-changed",
					"[user]", user.getName()));
		}
	}
}
