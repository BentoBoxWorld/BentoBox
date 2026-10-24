package com.islandwarp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

public final class IslandWarpPlugin extends JavaPlugin {
    WarpStore store;
    IslandGuard guard;
    WarpTeleport teleport;
    WarpMenu menu;
    WarpExpansion expansion;
    WarpEconomy economy;
    WarpHealth health;
    @Override public void onEnable() {
        saveDefaultConfig();
        try (var defaults=getResource("config.yml")) {
            java.nio.file.Files.copy(defaults,new File(getDataFolder(),"config-reference.yml").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) { getLogger().log(Level.WARNING,"配置参考模板保存失败",e); }
        try { store = new WarpStore(new File(getDataFolder(),"islandwarp.db").toPath()); }
        catch (Exception e) { getLogger().log(Level.SEVERE,"数据库初始化失败",e); getServer().getPluginManager().disablePlugin(this); return; }
        store.historyLimit(getConfig().getInt("notifications.history-limit",100));
        economy = new WarpEconomy(this);
        store.unresolvedPayments().thenAccept(rows -> rows.forEach(payment -> getLogger().severe("未完成交易需人工核对（禁止直接重试）："+payment)));
        guard = new IslandGuard(this); teleport = new WarpTeleport(this); menu = new WarpMenu(this);
        health = new WarpHealth(this);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t -> {
            long now=System.currentTimeMillis();
            if(store.all().values().stream().anyMatch(w -> w.visible()&&store.event(w.id())!=null&&store.event(w.id()).end()<=now)) {
                store.expireEvents().exceptionally(e -> { getLogger().log(Level.WARNING,"活动到期隐藏失败",e); return null; });
            }
        },20,20);
        getServer().getPluginManager().registerEvents(teleport,this);
        getServer().getPluginManager().registerEvents(menu,this);
        WarpCommand command = new WarpCommand(this);
        getCommand("islandwarp").setExecutor(command); getCommand("islandwarp").setTabCompleter(command);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) { expansion = new WarpExpansion(this); expansion.register(); }
        getLogger().info("IslandWarp 已启用，SQLite 地标数："+store.all().size());
    }
    @Override public void onDisable() {
        if (expansion != null) expansion.unregister();
        if (teleport != null) teleport.shutdown();
        if (store != null) try { store.close(); } catch(Exception e) { getLogger().log(Level.SEVERE,"数据库停服收尾失败，请保留数据库及 WAL 文件",e); }
    }
    static String color(String s) { return ChatColor.translateAlternateColorCodes('&',s); }
    void tell(Player p, String s) {
        String status=s.startsWith("&c")?"&c失败":s.startsWith("&a")?"&a成功":"&b提示";
        p.sendMessage(color("&8[&b岛屿地标&8] "+status+" &8› &r"+s));
    }
    void sound(Player p,String type) {
        if(!getConfig().getBoolean("sounds.enabled",true)) return;
        String fallback=switch(type) {
            case "open" -> "BLOCK_CHEST_OPEN";
            case "page" -> "ITEM_BOOK_PAGE_TURN";
            case "favorite-on" -> "ENTITY_PLAYER_LEVELUP";
            case "favorite-off" -> "BLOCK_NOTE_BLOCK_HAT";
            case "confirm" -> "BLOCK_NOTE_BLOCK_PLING";
            case "create" -> "BLOCK_AMETHYST_BLOCK_CHIME";
            case "pin" -> "BLOCK_BEACON_ACTIVATE";
            case "ticket" -> "ENTITY_VILLAGER_YES";
            case "countdown" -> "BLOCK_NOTE_BLOCK_HAT";
            case "cancel" -> "BLOCK_NOTE_BLOCK_BASS";
            case "success" -> "ENTITY_EXPERIENCE_ORB_PICKUP";
            case "error" -> "BLOCK_NOTE_BLOCK_BASS";
            case "teleport" -> "ENTITY_PLAYER_LEVELUP";
            default -> "UI_BUTTON_CLICK";
        };
        try { p.playSound(p.getLocation(),Sound.valueOf(getConfig().getString("sounds."+type,fallback)),(float)Math.max(0,Math.min(1,getConfig().getDouble("sounds.volume",.5))),(float)Math.max(.5,Math.min(2,getConfig().getDouble("sounds.pitch",1)))); }
        catch(IllegalArgumentException ignored) { }
    }
    void onPlayer(Player p, Runnable task) { if (isEnabled()) p.getScheduler().run(this,t -> task.run(),null); }
    void reloadConfigWithDefaults() {
        try (var resource=getResource("config.yml")) {
            if(resource==null) throw new IllegalStateException("config.yml resource missing");
            var defaults=new YamlConfiguration();
            defaults.load(new java.io.InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8));
            var file=new File(getDataFolder(),"config.yml");
            var config=new YamlConfiguration();
            config.load(file);
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            var temporary=java.nio.file.Files.createTempFile(getDataFolder().toPath(),"config-",".yml");
            try {
                config.save(temporary.toFile());
                try { java.nio.file.Files.move(temporary,file.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                catch(java.nio.file.AtomicMoveNotSupportedException ignored) { java.nio.file.Files.move(temporary,file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            } finally { java.nio.file.Files.deleteIfExists(temporary); }
            reloadConfig();
            if(store!=null) store.historyLimit(getConfig().getInt("notifications.history-limit",100));
        } catch(java.io.IOException | org.bukkit.configuration.InvalidConfigurationException error) {
            getLogger().log(Level.WARNING,"配置重载失败，未覆盖原配置",error);
            throw new IllegalArgumentException("配置读取或保存失败，请检查 YAML 格式和文件权限",error);
        }
    }
    void result(Player p, CompletableFuture<?> future, String message) {
        result(p,future,message,"success");
    }
    void result(Player p, CompletableFuture<?> future, String message,String successSound) {
        future.whenComplete((value,error) -> {
            Throwable cause=error==null?null:rootCause(error);
            if (cause != null && !(cause instanceof IllegalArgumentException)) getLogger().log(Level.WARNING,"地标操作失败",cause);
            onPlayer(p,() -> { tell(p,error == null ? message : "&c操作失败："+root(cause)); sound(p,error==null?successSound:"error"); });
        });
    }
    private static Throwable rootCause(Throwable e) {
        if(e==null) return null;
        while (e.getCause()!=null) e=e.getCause();
        return e;
    }
    private static String root(Throwable e) {
        if (e instanceof java.sql.SQLException) return "名称已存在或数据库写入异常，请检查日志";
        return e==null||e.getMessage()==null?"未知错误":e.getMessage();
    }
    String label(String id) { return getConfig().getString("categories."+id+".label",id); }
    boolean manageOthers(Player p) { return p.hasPermission("islandwarp.admin")&&getConfig().getBoolean("admin.manage-player-warps",true); }
    void requireManage(Player p,Warp w) {
        if(!manageOthers(p)&&(!w.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create"))) throw new IllegalArgumentException("不能管理其他玩家的坐标");
    }
    java.util.List<String> categories() {
        var section = getConfig().getConfigurationSection("categories");
        return section == null ? java.util.List.of() : java.util.List.copyOf(section.getKeys(false));
    }
    boolean canSee(Player p, Warp w) { return store.publicAvailable(w) || w.owner().equals(p.getUniqueId()) || p.hasPermission("islandwarp.admin"); }
}
