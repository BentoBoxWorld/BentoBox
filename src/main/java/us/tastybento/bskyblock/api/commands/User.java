package us.tastybento.bskyblock.api.commands;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Achievement;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

public class User implements Player {

    private Player player;
    private static Map<UUID, User> users = new HashMap<>();
    
    private User(Player player) {
        this.player = player;
        users.put(player.getUniqueId(), this);
    }
    
    public static User getInstance(Player player) {
        if (users.containsKey(player.getUniqueId())) {
            return users.get(player.getUniqueId());
        }
        return new User(player);
    }
    
    public static User getInstance(CommandSender sender) {
        if (sender instanceof Player) {
            return getInstance((Player)sender);
        }
        return null;
    }
    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    public static void removePlayer(Player player) {
        users.remove(player.getUniqueId());
    }

    /**
     * Send a message to sender if message is not empty. Does not include color codes or spaces
     * @param sender
     * @param message
     */
    @Override
    public void sendMessage(String message) {
        if (!ChatColor.stripColor(message).trim().isEmpty()) {
            player.sendMessage("DEBUG TEST:" + message);
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getName() {

        return player.getName();
    }

    @Override
    public PlayerInventory getInventory() {

        return player.getInventory();
    }

    @Override
    public Inventory getEnderChest() {

        return player.getEnderChest();
    }

    @Override
    public MainHand getMainHand() {

        return player.getMainHand();
    }

    @Override
    public boolean setWindowProperty(Property prop, int value) {

        return player.setWindowProperty(prop, value);
    }

    @Override
    public InventoryView getOpenInventory() {

        return player.getOpenInventory();
    }

    @Override
    public InventoryView openInventory(Inventory inventory) {

        return player.openInventory(inventory);
    }

    @Override
    public InventoryView openWorkbench(Location location, boolean force) {

        return player.openWorkbench(location, force);
    }

    @Override
    public InventoryView openEnchanting(Location location, boolean force) {

        return player.openEnchanting(location, force);
    }

    @Override
    public void openInventory(InventoryView inventory) {

        
    }

    @Override
    public InventoryView openMerchant(Villager trader, boolean force) {

        return player.openMerchant(trader, force);
    }

    @Override
    public InventoryView openMerchant(Merchant merchant, boolean force) {

        return player.openMerchant(merchant, force);
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
        
    }

    @Override
    public ItemStack getItemInHand() {

        return player.getItemInHand();
    }

    @Override
    public void setItemInHand(ItemStack item) {

        player.setItemInHand(item);
    }

    @Override
    public ItemStack getItemOnCursor() {

        return player.getItemOnCursor();
    }

    @Override
    public void setItemOnCursor(ItemStack item) {
        player.setItemOnCursor(item);
        
    }

    @Override
    public boolean hasCooldown(Material material) {

        return player.hasCooldown(material);
    }

    @Override
    public int getCooldown(Material material) {

        return player.getCooldown(material);
    }

    @Override
    public void setCooldown(Material material, int ticks) {
        player.setCooldown(material, ticks);
        
    }

    @Override
    public boolean isSleeping() {

        return player.isSleeping();
    }

    @Override
    public int getSleepTicks() {

        return player.getSleepTicks();
    }

    @Override
    public GameMode getGameMode() {

        return player.getGameMode();
    }

    @Override
    public void setGameMode(GameMode mode) {
        player.setGameMode(mode);
        
    }

    @Override
    public boolean isBlocking() {

        return player.isBlocking();
    }

    @Override
    public boolean isHandRaised() {

        return player.isHandRaised();
    }

    @Override
    public int getExpToLevel() {

        return player.getExpToLevel();
    }

    @Override
    public Entity getShoulderEntityLeft() {

        return player.getShoulderEntityLeft();
    }

    @Override
    public void setShoulderEntityLeft(Entity entity) {
        player.setShoulderEntityLeft(entity);
        
    }

    @Override
    public Entity getShoulderEntityRight() {

        return player.getShoulderEntityRight();
    }

    @Override
    public void setShoulderEntityRight(Entity entity) {
        player.setShoulderEntityRight(entity);
        
    }

    @Override
    public double getEyeHeight() {

        return player.getEyeHeight();
    }

    @Override
    public double getEyeHeight(boolean ignoreSneaking) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Location getEyeLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int maxDistance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRemainingAir() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRemainingAir(int ticks) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getMaximumAir() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setMaximumAir(int ticks) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getMaximumNoDamageTicks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setMaximumNoDamageTicks(int ticks) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public double getLastDamage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLastDamage(double damage) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getNoDamageTicks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setNoDamageTicks(int ticks) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Player getKiller() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addPotionEffect(PotionEffect effect) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addPotionEffect(PotionEffect effect, boolean force) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addPotionEffects(Collection<PotionEffect> effects) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPotionEffect(PotionEffectType type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PotionEffect getPotionEffect(PotionEffectType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removePotionEffect(PotionEffectType type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection<PotionEffect> getActivePotionEffects() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasLineOfSight(Entity other) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getRemoveWhenFarAway() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setRemoveWhenFarAway(boolean remove) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EntityEquipment getEquipment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCanPickupItems(boolean pickup) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean getCanPickupItems() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLeashed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Entity getLeashHolder() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setLeashHolder(Entity holder) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isGliding() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setGliding(boolean gliding) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAI(boolean ai) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasAI() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCollidable(boolean collidable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isCollidable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AttributeInstance getAttribute(Attribute attribute) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Location getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Location getLocation(Location loc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setVelocity(Vector velocity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Vector getVelocity() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isOnGround() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public World getWorld() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean teleport(Location location) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean teleport(Location location, TeleportCause cause) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean teleport(Entity destination) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean teleport(Entity destination, TeleportCause cause) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Entity> getNearbyEntities(double x, double y, double z) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getEntityId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFireTicks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxFireTicks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setFireTicks(int ticks) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isDead() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Server getServer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Entity getPassenger() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setPassenger(Entity passenger) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Entity> getPassengers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addPassenger(Entity passenger) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removePassenger(Entity passenger) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean eject() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public float getFallDistance() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setFallDistance(float distance) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLastDamageCause(EntityDamageEvent event) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UUID getUniqueId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTicksLived() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setTicksLived(int value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playEffect(EntityEffect type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EntityType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isInsideVehicle() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean leaveVehicle() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Entity getVehicle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCustomNameVisible(boolean flag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isCustomNameVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setGlowing(boolean flag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isGlowing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setInvulnerable(boolean flag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isInvulnerable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSilent() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSilent(boolean flag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasGravity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setGravity(boolean gravity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getPortalCooldown() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setPortalCooldown(int cooldown) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Set<String> getScoreboardTags() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addScoreboardTag(String tag) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeScoreboardTag(String tag) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isPermissionSet(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPermission(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void recalculatePermissions() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOp() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setOp(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getCustomName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCustomName(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void damage(double amount) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void damage(double amount, Entity source) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public double getHealth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setHealth(double health) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public double getMaxHealth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setMaxHealth(double health) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetMaxHealth() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isConversing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void acceptConversationInput(String input) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isOnline() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBanned() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWhitelisted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setWhitelisted(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long getFirstPlayed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLastPlayed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean hasPlayedBefore() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, Object> serialize() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPlayerListName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Location getCompassTarget() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InetSocketAddress getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void chat(String msg) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void awardAchievement(Achievement achievement) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasAchievement(Achievement achievement) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long getPlayerTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getPlayerTimeOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WeatherType getPlayerWeather() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void giveExp(int amount) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void giveExpLevels(int amount) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public float getExp() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTotalExperience() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getExhaustion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getSaturation() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFoodLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Location getBedSpawnLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getAllowFlight() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void hidePlayer(Player player) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean canSee(Player player) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public float getFlySpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getWalkSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Scoreboard getScoreboard() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getHealthScale() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Entity getSpectatorTarget() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AdvancementProgress getAdvancementProgress(Advancement advancement) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void hidePlayer(Plugin arg0, Player arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDisplayName(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setPlayerListName(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setCompassTarget(Location loc) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendRawMessage(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void kickPlayer(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean performCommand(String command) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSneaking() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSneaking(boolean sneak) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isSprinting() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void saveData() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void loadData() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSleepingIgnored(boolean isSleeping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isSleepingIgnored() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void playNote(Location loc, byte instrument, byte note) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setPlayerTime(long time, boolean relative) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setPlayerWeather(WeatherType type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setExp(float exp) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLevel(int level) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTotalExperience(int exp) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setExhaustion(float value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSaturation(float value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFoodLevel(int value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBedSpawnLocation(Location location) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBedSpawnLocation(Location location, boolean force) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void showPlayer(Player player) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFlying(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFlySpeed(float value) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setWalkSpeed(float value) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setTexturePack(String url) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setResourcePack(String url) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setResourcePack(String url, byte[] hash) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setHealthScaled(boolean scale) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setHealthScale(double scale) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSpectatorTarget(Entity entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void showPlayer(Plugin arg0, Player arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stopSound(Sound sound) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stopSound(String sound) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stopSound(Sound sound, SoundCategory category) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stopSound(String sound, SoundCategory category) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playEffect(Location loc, Effect effect, int data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendBlockChange(Location loc, Material material, byte data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sendBlockChange(Location loc, int material, byte data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendSignChange(Location loc, String[] lines) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendMap(MapView map) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateInventory() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeAchievement(Achievement achievement) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isPlayerTimeRelative() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void resetPlayerTime() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetPlayerWeather() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAllowFlight(boolean flight) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isFlying() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHealthScaled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetTitle() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
            double offsetZ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
            double offsetZ, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
            double offsetZ, double extra) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ, double extra) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
            double offsetZ, double extra, T data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
            double offsetY, double offsetZ, double extra, T data) {
        // TODO Auto-generated method stub
        
    }
    

    
}
