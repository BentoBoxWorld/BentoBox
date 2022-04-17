package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class VaultHook extends Hook {

    private static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be positive.";
	private static final String PLAYER_OR_OFFLINEPLAYER_REQUIRED = "User must be a Player or an OfflinePlayer";
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
    
    public String format(double amount) {
    	return economy.format(amount);
    }

    /**
     * Gets balance of this User.
     * If this User is not a Player (or OfflinePlayer), it will always return {@code 0.0D}.
     * If player is online, it should return the balance of the world of the player
     * If offline, it should return the general balance
     *
     * @param user the User to get the balance from.
     * @return the balance of this User.
     * @see #getBalance(User, World)
     */
    public double getBalance(User user) {
        return this.getBalance(user, user.getWorld());
    }
    
    /**
     * Get balance of this User for this world.
     * If this User is not a Player (or OfflinePlayer), it will always return {@code 0.0D}.
     * If the economy plugin don't support world, it should return general balance
     * 
     * @param user the User to get the balance from.
     * @param world the world where is the balance. If null, return general balance.
     * @return the balance of this User for this world.
     */
    public double getBalance(User user, World world) {
    	if (!user.isOfflinePlayer())
    		return 0.0D;
    	
    	if (world == null)
    		return economy.getBalance(user.getOfflinePlayer());
    	
    	return economy.getBalance(user.getOfflinePlayer(), world.getName());
    }

    /**
     * Withdraws an amount from this User.
     * On the balance of the world where is the user
     * 
     * @param user the User to withdraw from. Must be a Player or an OfflinePlayer.
     * @param amount the amount to withdraw. Must be positive.
     * @return the EconomyResponse of this withdrawal.
     * @see #withdraw(User, double, World)
     */
    public EconomyResponse withdraw(User user, double amount) {
        return withdraw(user, amount, user.getWorld());
    }
    
    /**
     * Withdraws an amount from this User on the balance from this World.
     * If the economy plugin don't support world or world is null, It will return general balance.
     * 
     * @param user the User to withdraw from. Must be a Player or an OfflinePlayer.
     * @param amount amount the amount to withdraw. Must be positive.
     * @param world the world where is the balance, can be null.
     * @return the EconomyResponse of this withdrawal.
     */
    public EconomyResponse withdraw(User user, double amount, World world) {
        if (!user.isOfflinePlayer()) {
            throw new IllegalArgumentException(PLAYER_OR_OFFLINEPLAYER_REQUIRED);
        }
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        
        if (world == null)
        	return economy.withdrawPlayer(user.getOfflinePlayer(), amount);
        
        EconomyResponse response = economy.withdrawPlayer(user.getOfflinePlayer(), world.getName(), amount);
        
        if (response == null || response.type == ResponseType.NOT_IMPLEMENTED)
        	return economy.withdrawPlayer(user.getOfflinePlayer(), amount);
        return response;
    }

    /**
     * Deposits an amount to this User.
     * On the balance of the world where is the user
     * 
     * @param user the User to deposit to. Must be a Player or an OfflinePlayer.
     * @param amount the amount to deposit. Must be positive.
     * @return the EconomyResponse of this deposit.
     * @see #deposit(User, double, World)
     */
    public EconomyResponse deposit(User user, double amount) {
        return deposit(user, amount, user.getWorld());
    }
    
    /**
     * Deposits an amount to this User on the balance from this World.
     * If the economy plugin don't support world or world is null, It will return general balance.
     * 
     * @param user the User to deposit to. Must be a Player or an OfflinePlayer.
     * @param amount the amount to deposit. Must be positive.
     * @param world the world where is the balance, can be null.
     * @return the EconomyResponse of this deposit.
     */
    public EconomyResponse deposit(User user, double amount, World world) {
        if (!user.isOfflinePlayer()) {
            throw new IllegalArgumentException(PLAYER_OR_OFFLINEPLAYER_REQUIRED);
        }
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        
        if (world == null)
        	return economy.depositPlayer(user.getOfflinePlayer(), amount);
        
        EconomyResponse response = economy.depositPlayer(user.getOfflinePlayer(), world.getName(), amount);
        
        if (response == null || response.type == ResponseType.NOT_IMPLEMENTED)
        	return economy.depositPlayer(user.getOfflinePlayer(), amount);
        return response;
    }

    /**
     * Checks if this User has the amount.
     * On the balance of the world where is the user
     * If this User is not a Player (or OfflinePlayer), it will always return {@code false}.
     *
     * @param user the User to check.
     * @param amount the amount to check. Must be positive.
     * @return whether the User has the amount or not.
     * @see #has(User, double, World)
     */
    public boolean has(User user, double amount) {
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        return user.isOfflinePlayer() && economy.has(user.getOfflinePlayer(), amount);
    }
    
    /**
     * Checks if this User has the amount on the balance from this World.
     * If this User is not a Player (or OfflinePlayer), it will always return {@code false}.
     *  If the economy plugin don't support world or world is null, It will return general balance.
     *
     * @param user the User to check.
     * @param amount the amount to check. Must be positive.
     * @param world the world where is the balance, can be null.
     * @return whether the User has the amount or not.
     */
    public boolean has(User user, double amount, World world) {
        if (amount < 0.0D) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        
        if (!user.isOfflinePlayer()) {
            throw new IllegalArgumentException(PLAYER_OR_OFFLINEPLAYER_REQUIRED);
        }
        
        if (world == null)
        	return economy.has(user.getOfflinePlayer(), amount);
        
        return economy.has(user.getOfflinePlayer(), world.getName(), amount);
    }
}
