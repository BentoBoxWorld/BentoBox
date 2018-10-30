package world.bentobox.bentobox.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * @author Poslovitch
 */
public class VaultHook extends Hook {

    private Economy economy;

    public VaultHook() {
        super("Vault");
    }

    @Override
    public boolean hook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public String getFailureCause() {
        return "no plugin supporting economy has been found";
    }

    public Economy getEconomy() {
        return economy;
    }
}
