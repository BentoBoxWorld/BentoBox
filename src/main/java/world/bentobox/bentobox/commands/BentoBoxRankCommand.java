package world.bentobox.bentobox.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Manages ranks
 *
 * @author tastybento
 * @since 2.0.0
 */
public class BentoBoxRankCommand extends CompositeCommand {

	private int rankValue;
	private String firstElement;
	private final RanksManager rm;

	/**
	 * Rank management. Add and remove
	 * @param parent command parent
	 */
	public BentoBoxRankCommand(CompositeCommand parent) {
		super(parent, "rank");
		rm = getPlugin().getRanksManager();
	}

	@Override
	public void setup() {
		setPermission("bentobox.admin.rank");
		setDescription("commands.bentobox.rank.description");
		this.setParametersHelp("commands.bentobox.rank.parameters");
	}

	@Override
	public boolean canExecute(User user, String label, List<String> args) {

		if (args.isEmpty()) {
			// Show help
			showHelp(this, user);
			return false;
		}
		// Check if the first element is "add" or "remove" or "list"
		firstElement = args.get(0);
		if (!("list".equals(firstElement) || "add".equals(firstElement) || "remove".equals(firstElement))) {
			// Show help
			showHelp(this, user);
			return false;
		}

		if ("remove".equals(firstElement) && args.size() != 2) {
			// Show help
			showHelp(this, user);
			return false;
		}

		// If the first element is "add", then check if the third element is an integer
		if ("add".equals(firstElement)) {
			// Check if there is a third element
			if (args.size() < 3) {
				// Show help
				showHelp(this, user);
				return false;
			}

			// Check if the third element is an integer
			String thirdElement = args.get(2);
			try {
				rankValue = Integer.parseInt(thirdElement);
			} catch (NumberFormatException e) {
				// Show help
				showHelp(this, user);
				return false;
			}
		}

		// If all checks passed, return true
		return true;
	}

	@Override
	public boolean execute(User user, String label, List<String> args) {

		if ("list".equals(firstElement)) {
			showRanks(user);
			return true;
		}
		if ("add".equals(firstElement)) {
			if (rm.addRank(args.get(1), rankValue)) {    			
				user.sendMessage("commands.bentobox.rank.add.success", "[rank]", args.get(1), TextVariables.NUMBER, String.valueOf(rankValue));
				showRanks(user);
			} else {
				user.sendMessage("commands.bentobox.rank.add.failure", "[rank]", args.get(1), TextVariables.NUMBER, String.valueOf(rankValue));
				return false;
			}
		} else {
			if (rm.removeRank(args.get(1))) {
				user.sendMessage("commands.bentobox.rank.remove.success", "[rank]", args.get(1));
				showRanks(user);
			} else {
				user.sendMessage("commands.bentobox.rank.remove.failure", "[rank]", args.get(1));
				return false;
			}
		}    	
		return true;
	}

	private void showRanks(User user) {
		user.sendMessage("commands.bentobox.rank.list");
		rm.getRanks().forEach((ref, rank) -> {
			user.sendRawMessage(user.getTranslation(ref) + ": " + ref + " " + String.valueOf(rank));
		});

	}

	@Override
	public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
		if (args.size() <=1) {
			return Optional.empty();
		}
		firstElement = args.get(1);
		if (args.size() <= 2) {
			return Optional.of(List.of("add","remove","list"));
		}
		if (args.size() > 1 && "add".equals(firstElement)) {
			List<String> options = new ArrayList<>(RanksManager.DEFAULT_RANKS.keySet());
			options.removeIf(rm.getRanks().keySet()::contains);
			return Optional.of(options);
		} 
		if (args.size() > 1 && "remove".equals(firstElement)) {
			return Optional.of(new ArrayList<>(rm.getRanks().keySet()));
		}
		return Optional.empty();

	}
}
