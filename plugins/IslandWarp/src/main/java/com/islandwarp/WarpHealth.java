package com.islandwarp;

import org.bukkit.Bukkit;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

final class WarpHealth {
    private final IslandWarpPlugin plugin;
    private final AtomicBoolean scanning=new AtomicBoolean();
    private long cursor,next;
    WarpHealth(IslandWarpPlugin plugin) {
        this.plugin=plugin;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,t -> {
            if(!plugin.getConfig().getBoolean("health.enabled",true)||System.currentTimeMillis()<next||scanning.get()) return;
            next=System.currentTimeMillis()+Math.max(1,Math.min(86400,plugin.getConfig().getInt("health.interval-seconds",5)))*1000L;
            var all=plugin.store.all().values();
            Warp w=all.stream().filter(v -> v.id()>cursor).min(Comparator.comparingLong(Warp::id)).orElseGet(() -> all.stream().min(Comparator.comparingLong(Warp::id)).orElse(null));
            if(w==null) return;
            cursor=w.id();
            check(w).whenComplete((reason,error) -> {
                if(error!=null&&plugin.isEnabled()) plugin.getLogger().warning("坐标检测失败 ID="+w.id()+"："+error);
            });
        },100,20);
    }
    CompletableFuture<String> check(Warp w) {
        if(!scanning.compareAndSet(false,true)) return CompletableFuture.failedFuture(new IllegalArgumentException("正在检测其他坐标，请稍后再试"));
        boolean notify=plugin.getConfig().getBoolean("health.notify-owner",true);
        return plugin.teleport.inspect(w).orTimeout(30,java.util.concurrent.TimeUnit.SECONDS).thenCompose(reason -> {
            if(!plugin.isEnabled()) return CompletableFuture.failedFuture(new IllegalStateException("插件已停用"));
            return plugin.store.checked(w,reason,notify).thenApply(changed -> reason.isEmpty()?"检查通过（仅代表检查时的落点状态）":"异常："+reason);
        }).whenComplete((reason,error) -> scanning.set(false));
    }
}
