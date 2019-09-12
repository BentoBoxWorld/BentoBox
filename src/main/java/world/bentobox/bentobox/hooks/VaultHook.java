package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class VaultHook extends Hook {

    private static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be positive.";
    private Economy economy;

    public VaultHook() {
        super("Vault", Material.GOLD_NUGGET);
    }

    @Override
    public boolean hook() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            economy = rsp.getProvider();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFailureCause() {
        return "no plugin supporting economy has been found";
    }

    public Economy getEconomy() {
        return economy;
    }

    // ------ CONVENIENCE METHODS ------

    /**
     * Gets balance of this User.
     * If this User is not a Player (or OfflinePlayer), it will always return {@code 0.0D}.
     *
     * @param user the User to get the balance from.
     * @return the balance of this User.
     */
    public double getBalance(User user) {
        return (user.isOfflinePlayer()) ? economy.getBalance(user.getOfflinePlayer()) : 0.0D;
    }

    /**
     * Withdraws an amount from this User.
     * @param user the User to withdraw from. Must be a Player or an OfflinePlayer.
     * @param amount the amount to withdraw. Must be positive.
     * @return the EconomyResponse of this withdrawal.
     */
    public EconomyResponse withdraw(User user, double amount) {
        if (!user.isOfflinePlayer()) {
            throw new IllegalArgumentException("User must be a Player or an OfflinePlayer");
        }
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        return economy.withdrawPlayer(user.getOfflinePlayer(), amount);
    }

    /**
     * Deposits an amount to this User.
     * @param user the User to deposit to. Must be a Player or an OfflinePlayer.
     * @param amount the amount to deposit. Must be positive.
     * @return the EconomyResponse of this deposit.
     */
    public EconomyResponse deposit(User user, double amount) {
        if (!user.isOfflinePlayer()) {
            throw new IllegalArgumentException("User must be a Player or an OfflinePlayer");
        }
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        return economy.depositPlayer(user.getOfflinePlayer(), amount);
    }

    /**
     * Checks if this User has the amount.
     * If this User is not a Player (or OfflinePlayer), it will always return {@code false}.
     *
     * @param user the User to check.
     * @param amount the amount to check. Must be positive.
     * @return whether the User has the amount or not.
     */
    public boolean has(User user, double amount) {
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        return user.isOfflinePlayer() && economy.has(user.getOfflinePlayer(), amount);
    }
}
