package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.milkbowl.vault.economy.Economy;
import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for {@link BentoBoxHookRegistrar} Vault hook timing.
 */
class BentoBoxHookRegistrarTest extends CommonTestSetup {

    @Mock
    private Plugin vaultPlugin;

    private BentoBoxHookRegistrar registrar;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(vaultPlugin.isEnabled()).thenReturn(true);
        when(pim.getPlugin("Vault")).thenReturn(vaultPlugin);
        registrar = new BentoBoxHookRegistrar(plugin);
    }

    @SuppressWarnings("unchecked")
    private void registerEconomy() {
        RegisteredServiceProvider<Economy> rsp = mock(RegisteredServiceProvider.class);
        when(rsp.getProvider()).thenReturn(mock(Economy.class));
        ServicesManager servicesManager = mock(ServicesManager.class);
        when(servicesManager.getRegistration(Economy.class)).thenReturn(rsp);
        Server mockServer = mock(Server.class);
        when(mockServer.getServicesManager()).thenReturn(servicesManager);
        mockedBukkit.when(Bukkit::getServer).thenReturn(mockServer);
    }

    /**
     * Vault installed with no economy yet: the early hook is deferred, so no failure is logged.
     */
    @Test
    void testRegisterEarlyHooksDefersVaultWhenNoEconomy() {
        registrar.registerEarlyHooks();
        verify(hooksManager, never()).registerHook(argThat(VaultHook.class::isInstance));
    }

    /**
     * Vault installed with an economy already registered: Vault is hooked early as before.
     */
    @Test
    void testRegisterEarlyHooksHooksVaultWhenEconomyPresent() {
        registerEconomy();
        registrar.registerEarlyHooks();
        verify(hooksManager).registerHook(argThat(VaultHook.class::isInstance));
    }

    /**
     * After addons enable, Vault is (re)tried if not yet hooked, so a missing economy is reported then.
     */
    @Test
    void testRegisterVaultHookIfNeeded() {
        when(plugin.getVault()).thenReturn(Optional.empty());
        registrar.registerVaultHookIfNeeded();
        verify(hooksManager).registerHook(any(VaultHook.class));
    }

    @Test
    void testIsEconomyRegistered() {
        VaultHook hook = new VaultHook();
        assertFalse(hook.isEconomyRegistered());
        registerEconomy();
        assertTrue(hook.isEconomyRegistered());
    }

    @Test
    void testIsEconomyRegisteredVaultNotInstalled() {
        when(pim.getPlugin("Vault")).thenReturn(null);
        registerEconomy();
        assertFalse(new VaultHook().isEconomyRegistered());
    }
}
