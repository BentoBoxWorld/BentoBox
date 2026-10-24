package com.islandwarp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class WarpTeleport implements Listener {
    // shortcut: current Folia/Lophine async teleport omits the standard event; recheck on server upgrades.
    private static final boolean FOLIA = folia();
    private static boolean folia() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
    private record Pending(long warp, Location start, double price, java.util.concurrent.atomic.AtomicBoolean committing,
                           java.util.concurrent.atomic.AtomicBoolean paid,java.util.concurrent.atomic.AtomicBoolean refunded,java.util.concurrent.atomic.AtomicReference<String> payment) {}
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private final IslandWarpPlugin plugin;
    WarpTeleport(IslandWarpPlugin plugin) { this.plugin = plugin; }
    void start(Player player, Warp warp, double acceptedPrice) {
        cooldown.entrySet().removeIf(entry -> entry.getValue()<=System.currentTimeMillis());
        if (!player.hasPermission("islandwarp.use")) throw new IllegalArgumentException("没有传送权限");
        if (cooldown.getOrDefault(player.getUniqueId(),0L) > System.currentTimeMillis()) throw new IllegalArgumentException("传送冷却中，请稍后再试");
        plugin.guard.access(player, warp);
        if(Double.compare(plugin.economy.admission(player,warp),acceptedPrice)!=0) throw new IllegalArgumentException("门票价格已变更，请重新确认");
        Pending request = new Pending(warp.id(),player.getLocation().clone(),acceptedPrice,new java.util.concurrent.atomic.AtomicBoolean(),new java.util.concurrent.atomic.AtomicBoolean(),new java.util.concurrent.atomic.AtomicBoolean(),new java.util.concurrent.atomic.AtomicReference<>());
        if (pending.putIfAbsent(player.getUniqueId(),request) != null) throw new IllegalArgumentException("已经有待执行的传送");
        player.closeInventory();
        int seconds = Math.max(0,plugin.getConfig().getInt("teleport.delay-seconds",3));
        plugin.tell(player,"&b传送准备中，" + seconds + " 秒后出发。移动或受伤会取消。");
        plugin.sound(player,"confirm");
        if(player.getScheduler().runDelayed(plugin,t -> load(player,request), () -> pending.remove(player.getUniqueId(),request), Math.max(1,seconds * 20L))==null) pending.remove(player.getUniqueId(),request);
        if(seconds>0) {
            long deadline=System.currentTimeMillis()+seconds*1000L;
            player.getScheduler().runAtFixedRate(plugin,t -> {
                if(pending.get(player.getUniqueId())!=request||System.currentTimeMillis()>=deadline) { t.cancel(); return; }
                if(plugin.getConfig().getBoolean("teleport.countdown",true)) player.sendActionBar(net.kyori.adventure.text.Component.text("传送准备 · "+Math.max(1,(deadline-System.currentTimeMillis()+999)/1000)+" 秒"));
                if(plugin.getConfig().getBoolean("sounds.teleport-preparing",true)) plugin.sound(player,"countdown");
            },null,1,20);
        }
    }
    private void load(Player player, Pending request) {
        if (pending.get(player.getUniqueId()) != request) return;
        Warp warp = plugin.store.all().get(request.warp());
        if (warp == null || (!warp.visible() && !warp.owner().equals(player.getUniqueId()) && !player.hasPermission("islandwarp.admin"))) {
            fail(player,request,"地标已删除或隐藏"); return;
        }
        try { plugin.guard.access(player,warp); }
        catch (IllegalArgumentException e) { fail(player,request,e.getMessage()); return; }
        World world = Bukkit.getWorld(warp.world());
        if (world == null) { fail(player,request,"目标世界未加载"); return; }
        Location target = new Location(world,Math.floor(warp.x())+.5,Math.floor(warp.y()),Math.floor(warp.z())+.5,warp.yaw(),warp.pitch());
        world.getChunkAtAsync(target).whenComplete((chunk,error) -> {
            if (!plugin.isEnabled()) return;
            if (error != null) { plugin.onPlayer(player,() -> fail(player,request,"目标区块加载失败")); return; }
            Bukkit.getRegionScheduler().run(plugin,target,t -> {
                boolean safe = safe(target);
                plugin.onPlayer(player,() -> {
                    if (pending.get(player.getUniqueId()) != request) return;
                    if (!player.hasPermission("islandwarp.use")) { fail(player,request,"传送权限已被撤销"); return; }
                    Warp latest = plugin.store.all().get(warp.id());
                    if (!warp.sameSettings(latest)) { fail(player,request,"地标发生变更，请重新传送"); return; }
                    if (!safe) { fail(player,request,"落点不安全，请联系岛主更新位置"); return; }
                    try { plugin.guard.access(player,warp); }
                    catch (IllegalArgumentException e) { fail(player,request,e.getMessage()); return; }
                    if(Double.compare(plugin.economy.admission(player,warp),request.price())!=0) { fail(player,request,"门票价格已变更，请重新确认"); return; }
                     plugin.economy.debit(player.getUniqueId(),request.price(),"传送门票，地标="+warp.id()).whenComplete((paid,paymentError) -> {
                         if(paymentError!=null) { plugin.onPlayer(player,() -> fail(player,request,"付款未完成；若已扣款请让管理员用 /iw admin payments 核对，不要重复付款")); return; }
                         request.payment().set(paid);request.paid().set(true);
                        Runnable refund=() -> {
                            refund(player.getUniqueId(),request);
                        };
                        if(!plugin.isEnabled()) { refund.run(); return; }
                        try { var scheduled=player.getScheduler().run(plugin,task -> {
                            if(pending.get(player.getUniqueId())!=request) { refund.run(); return; }
                            Warp now=plugin.store.all().get(warp.id());
                            try {
                                if(!warp.sameSettings(now)||Double.compare(plugin.economy.admission(player,warp),request.price())!=0) throw new IllegalArgumentException("地标或票价已变更");
                                plugin.guard.access(player,warp);
                                if(!player.hasPermission("islandwarp.use")) throw new IllegalArgumentException("传送权限已撤销");
                            } catch(IllegalArgumentException e) { refund.run(); fail(player,request,e.getMessage()); return; }
                             request.committing().set(true);
                             try {
                                 // Folia's async path omits the standard player event; never accept an unchecked redirect.
                                 if(FOLIA) {
                                     var event=new PlayerTeleportEvent(player,player.getLocation(),target.clone(),PlayerTeleportEvent.TeleportCause.PLUGIN);
                                     Bukkit.getPluginManager().callEvent(event);
                                     if(event.isCancelled()||!target.equals(event.getTo())) {
                                         refund.run(); fail(player,request,"传送被其他插件取消或修改，门票已申请退还"); return;
                                     }
                                     if(pending.get(player.getUniqueId())!=request) { refund.run(); return; }
                                     if(!warp.sameSettings(plugin.store.all().get(warp.id()))||Double.compare(plugin.economy.admission(player,warp),request.price())!=0) throw new IllegalArgumentException("地标或票价已变更");
                                     plugin.guard.access(player,warp);
                                     if(!player.hasPermission("islandwarp.use")) throw new IllegalArgumentException("传送权限已撤销");
                                 }
                                 player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((success,failure) -> {
                                    pending.remove(player.getUniqueId(),request);
                                    if(failure!=null||!Boolean.TRUE.equals(success)) {
                                        refund.run(); plugin.onPlayer(player,() -> plugin.tell(player,"&c传送失败，门票已申请退还")); return;
                                    }
                                     plugin.economy.settle(warp,request.price(),request.payment().get()).whenComplete((settled,settleError) -> plugin.economy.finish(request.payment().get()));
                                    cooldown.put(player.getUniqueId(),System.currentTimeMillis()+Math.max(0,plugin.getConfig().getInt("teleport.cooldown-seconds",10))*1000L);
                                    plugin.result(player,plugin.store.visit(warp.id(),plugin.getConfig().getBoolean("visits.enabled",true)?player.getName():null,plugin.getConfig().getInt("visits.recent-limit",100)),"&a已到达 "+warp.name());
                                    plugin.onPlayer(player,() -> plugin.sound(player,"teleport"));
                                });
                            } catch(Exception e) { refund.run(); fail(player,request,"传送失败，门票已申请退还"); }
                        },() -> { pending.remove(player.getUniqueId(),request); refund.run(); });
                        if(scheduled==null) { pending.remove(player.getUniqueId(),request); refund.run(); }
                        } catch(Exception e) { pending.remove(player.getUniqueId(),request); refund.run(); }
                    });
                });
            });
        });
    }
    static boolean safe(Location l) {
        World w = l.getWorld(); int y = l.getBlockY();
        if (y <= w.getMinHeight() || y + 1 >= w.getMaxHeight() || !w.getWorldBorder().isInside(l)) return false;
        var feet = l.getBlock(); var head = feet.getRelative(0,1,0); var floor = feet.getRelative(0,-1,0);
        Set<Material> danger = Set.of(Material.LAVA,Material.FIRE,Material.SOUL_FIRE,Material.MAGMA_BLOCK,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.CACTUS,Material.SWEET_BERRY_BUSH,Material.POWDER_SNOW,Material.WATER);
        return feet.isPassable() && head.isPassable() && !floor.isPassable()
                && !danger.contains(feet.getType()) && !danger.contains(head.getType()) && !danger.contains(floor.getType());
    }
    void previewSafety(Player player,Warp warp,java.util.function.Consumer<Boolean> result) {
        inspect(warp).whenComplete((reason,error) -> plugin.onPlayer(player,() -> result.accept(error==null&&reason.isEmpty())));
    }
    java.util.concurrent.CompletableFuture<String> inspect(Warp warp) {
        var result=new java.util.concurrent.CompletableFuture<String>();
        World world=Bukkit.getWorld(warp.world());
        if(world==null) { result.complete("目标世界不存在或未加载"); return result; }
        Location target=new Location(world,Math.floor(warp.x())+.5,Math.floor(warp.y()),Math.floor(warp.z())+.5);
        try { world.getChunkAtAsync(target).whenComplete((chunk,error) -> {
            if(!plugin.isEnabled()) { result.completeExceptionally(new IllegalStateException("插件已停用")); return; }
            if(error!=null) { result.complete("目标区块加载失败，请稍后复查"); return; }
            try { Bukkit.getRegionScheduler().run(plugin,target,t -> {
                try { result.complete(safe(target)?"":"目标落点不安全（地面、空间、危险方块或世界边界）"); }
                catch(Exception e) { result.completeExceptionally(e); }
            }); } catch(Exception e) { result.completeExceptionally(e); }
        }); } catch(Exception e) { result.completeExceptionally(e); }
        return result;
    }
    private void fail(Player player, Pending request, String message) { pending.remove(player.getUniqueId(),request); plugin.tell(player,"&c"+message); }
    private void refund(UUID player,Pending request) {
        if(request.paid().get()&&request.refunded().compareAndSet(false,true)) plugin.economy.refund(player,request.price(),request.payment().get()).exceptionally(e -> { plugin.getLogger().severe("需人工退款："+player+" 坐标="+request.warp()+" 金额="+request.price()+" "+e); return null; });
    }
    private void cancel(Player p) {
        Pending r=pending.get(p.getUniqueId());
        if(r!=null&&!r.committing().get()&&pending.remove(p.getUniqueId(),r)) { plugin.tell(p,"&c传送已取消"); plugin.sound(p,"cancel"); }
    }
    @EventHandler(ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        Pending r = pending.get(e.getPlayer().getUniqueId());
        if (r != null && plugin.getConfig().getBoolean("teleport.cancel-on-move",true) && (!r.start().getWorld().equals(e.getTo().getWorld()) || r.start().distanceSquared(e.getTo()) > .04)) cancel(e.getPlayer());
    }
    @EventHandler(ignoreCancelled = true) public void damage(EntityDamageEvent e) { if (plugin.getConfig().getBoolean("teleport.cancel-on-damage",true) && e.getEntity() instanceof Player p) cancel(p); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
        cooldown.entrySet().removeIf(entry -> entry.getValue()<=System.currentTimeMillis());
    }
    void shutdown() {
        pending.forEach((player,request) -> {
            if(request.paid().get()&&request.price()>0) {
                if(request.committing().get()) plugin.getLogger().severe("停服待核对传送/退款："+player+" 坐标="+request.warp()+" 金额="+request.price()+"，传送结果尚未完成，不可直接重复退款");
                else refund(player,request);
            }
        });
        pending.clear(); cooldown.clear();
    }
}
