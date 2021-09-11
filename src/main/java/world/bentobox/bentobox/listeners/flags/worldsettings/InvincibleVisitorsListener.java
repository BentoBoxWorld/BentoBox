package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Arrays;
import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Listener for invincible visitor settings. Handles click listening and damage events
 * @author tastybento
 *
 */
public class InvincibleVisitorsListener extends FlagListener implements ClickHandler {

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        String reqPerm = getIWM().getPermissionPrefix(Util.getWorld(user.getWorld())) + "admin.settings.INVINCIBLE_VISITORS";
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        String ivPanelName = user.getTranslation("protection.flags.INVINCIBLE_VISITORS.name");
        if (panel.getName().equals(ivPanelName)) {
            // This is a click on the IV panel
            String itemName = panel.getItems().get(slot).getName();
            DamageCause c = getEnum(user, itemName);
            if (c == null) {
                user.sendMessage("general.errors.general");
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                BentoBox.getInstance().logError("Inv Visitor click did not match translated name: " + itemName);
                return true;
            }
            if (getIWM().getIvSettings(user.getWorld()).contains(c.name())) {
                getIWM().getIvSettings(user.getWorld()).remove(c.name());
            } else {
                getIWM().getIvSettings(user.getWorld()).add(c.name());
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, getPanelItem(c, user).getItem());
            // Save settings
            BentoBox.getInstance().getIWM().getAddon(Util.getWorld(user.getWorld())).ifPresent(GameModeAddon::saveWorldSettings);
        } else {
            // Open the IV Settings panel
            openPanel(user, ivPanelName);
        }
        return true;
    }

    private DamageCause getEnum(User user, String itemName) {
        return Arrays.stream(EntityDamageEvent.DamageCause.values()).filter(dc -> getTranslation(user, dc.name()).equals(itemName)).findFirst().orElse(null);
    }

    private void openPanel(User user, String ivPanelName) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel for visitor protection
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(ivPanelName);

        // Make panel items - sort alphabetically.
        Arrays.stream(EntityDamageEvent.DamageCause.values())
        .map(c -> getPanelItem(c, user)).sorted(Comparator.comparing(PanelItem::getName)).forEach(pb::item);
        pb.build();
    }



    private PanelItem getPanelItem(DamageCause c, User user) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(getTranslation(user, c.name()));
        pib.clickHandler(this);
        if (getIWM().getIvSettings(user.getWorld()).contains(c.name())) {
            pib.icon(Material.GREEN_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-active"));
        } else {
            pib.icon(Material.RED_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        }
        return pib.build();
    }

    /**
     * Get the translation of the DamageCause enum
     * @param user user seeing text
     * @param name enum name
     * @return translation or a prettified version of name
     */
    private String getTranslation(User user, String name) {
        String translation = user.getTranslationOrNothing("enums.DamageCause." + name);
        return translation.isEmpty() ? Util.prettifyText(name) : translation;
    }

    /**
     * Prevents visitors from getting damage if a particular damage type is listed in the config
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVisitorGetDamage(EntityDamageEvent e) {
        World world = e.getEntity().getWorld();
        if (!(e.getEntity() instanceof Player p)
                || !getIWM().inWorld(world)
                || e.getEntity().hasMetadata("NPC")
                || !getIWM().getIvSettings(world).contains(e.getCause().name())
                || getIslands().userIsOnIsland(world, User.getInstance(e.getEntity()))
                || PVPAllowed(p.getLocation())
                ) {
            return;
        }
        // Player is a visitor and should be protected from damage
        e.setCancelled(true);
        // Handle the void - teleport player back to island in a safe spot
        if(e.getCause().equals(DamageCause.VOID)) {
            if (getIslands().getIslandAt(p.getLocation()).isPresent()) {
                getIslands().getIslandAt(p.getLocation()).ifPresent(island ->
                // Teleport
                new SafeSpotTeleport.Builder(getPlugin())
                .entity(p)
                .location(island.getProtectionCenter().toVector().toLocation(p.getWorld()))
                .build());
            } else if (getIslands().hasIsland(p.getWorld(), p.getUniqueId())) {
                // No island in this location - if the player has an island try to teleport them back
                getIslands().homeTeleportAsync(p.getWorld(), p);
            } else {
                // Else die, sorry.
                e.setCancelled(false);
            }
        }
    }


}

