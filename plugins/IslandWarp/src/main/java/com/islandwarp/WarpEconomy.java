package com.islandwarp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Vault calls are serialized on the global scheduler, never on the SQLite worker. */
final class WarpEconomy {
    private final IslandWarpPlugin plugin;
    private final Set<UUID> busy=ConcurrentHashMap.newKeySet();
    WarpEconomy(IslandWarpPlugin plugin) { this.plugin=plugin; }
    double cost(String key,double fallback) {
        if(!plugin.getConfig().getBoolean("economy.enabled",true)) return 0;
        return limit(key,fallback);
    }
    double limit(String key,double fallback) {
        double n=plugin.getConfig().getDouble("economy."+key,fallback);
        if(!Double.isFinite(n)||n<0) throw new IllegalArgumentException("经济配置金额无效");
        return Math.round(n*100)/100.0;
    }
    static String money(double amount) { return String.format(Locale.ROOT,"%.2f 金币",amount); }
    double admission(Player p,Warp w) {
        var t=plugin.store.ticket(w.id());
        return !plugin.getConfig().getBoolean("economy.enabled",true)||w.owner().equals(p.getUniqueId())||!t.enabled()?0:t.price();
    }
    private Economy provider() {
        if(!Bukkit.getPluginManager().isPluginEnabled("Vault")) throw new IllegalArgumentException("Vault 未连接，收费操作暂不可用");
        var registration=Bukkit.getServicesManager().getRegistration(Economy.class);
        if(registration==null) throw new IllegalArgumentException("金币账户未连接，收费操作暂不可用");
        return registration.getProvider();
    }
    CompletableFuture<String> debit(UUID id,double amount,String detail) {
        return tracked(id,amount,"DEBIT",detail,() -> {
            if(amount<=0) return;
            var result=provider().withdrawPlayer(Bukkit.getOfflinePlayer(id),amount);
            if(!result.transactionSuccess()) {
                String reason=result.errorMessage==null?"":result.errorMessage.toLowerCase(Locale.ROOT);
                throw new Declined(reason.contains("insufficient")||reason.contains("fund")?"金币余额不足，需要 "+money(amount):"Vault 扣款失败："+result.errorMessage);
            }
        });
    }
    CompletableFuture<Void> credit(UUID id,double amount) {
        return credit(id,amount,"入账");
    }
    private CompletableFuture<Void> credit(UUID id,double amount,String detail) {
        return tracked(id,amount,"CREDIT",detail,() -> {
            if(amount<=0) return;
            var result=provider().depositPlayer(Bukkit.getOfflinePlayer(id),amount);
            if(!result.transactionSuccess()) throw new Declined("入账失败："+result.errorMessage);
        }).thenApply(payment -> null);
    }
    private CompletableFuture<String> tracked(UUID id,double amount,String direction,String detail,Runnable action) {
        if(amount<=0) return CompletableFuture.completedFuture(null);
        return plugin.store.payment(id,amount,direction,detail).thenCompose(payment -> {
            return plugin.store.paymentState(payment,"EXECUTING").thenCompose(ignored -> global(action))
                    .thenCompose(ignored -> plugin.store.paymentState(payment,direction.equals("DEBIT")?"PAID":"DONE").thenApply(done -> payment))
                    .whenComplete((value,error) -> {
                        if(error==null) return;
                        Throwable cause=error;while(cause.getCause()!=null) cause=cause.getCause();
                        if(cause instanceof Declined&&direction.equals("DEBIT")) plugin.store.paymentState(payment,"FAILED");
                        else plugin.getLogger().severe("交易结果需核对，禁止直接重试："+payment+" 玩家="+id+" 金额="+amount+" "+error);
                    });
        });
    }
    void finish(String payment) {
        plugin.store.paymentState(payment,"DONE").exceptionally(error -> { plugin.getLogger().severe("交易完成状态保存失败："+payment+" "+error);return null; });
    }
    CompletableFuture<Void> refund(UUID player,double amount,String payment) {
        return credit(player,amount,"退款，原交易="+payment).thenCompose(ignored -> plugin.store.paymentState(payment,"DONE"));
    }
    private static final class Declined extends IllegalArgumentException { Declined(String message) { super(message); } }
    private CompletableFuture<Void> global(Runnable action) {
        CompletableFuture<Void> future=new CompletableFuture<>();
        try { var task=Bukkit.getGlobalRegionScheduler().run(plugin,t -> { if(future.isDone()) return; try { action.run(); future.complete(null); } catch(Exception e) { future.completeExceptionally(e); } });
            if(task==null) future.completeExceptionally(new IllegalStateException("调度器拒绝经济任务，操作未执行")); }
        catch(Exception e) { future.completeExceptionally(e); }
        // Vault has no cancellation protocol; a timeout could report failure after money was already moved.
        return future;
    }
    void purchase(Player p,double cost,Supplier<CompletableFuture<?>> save,String message,Runnable refresh) {
        plugin.store.requireCacheReady();
        UUID id=p.getUniqueId();
        if(!busy.add(id)) throw new IllegalArgumentException("上一笔操作正在处理，请稍候");
        debit(id,cost,"购买："+message).whenComplete((payment,error) -> {
            if(error!=null) { busy.remove(id); plugin.result(p,CompletableFuture.failedFuture(error),""); return; }
            CompletableFuture<?> saving;
            try { saving=save.get(); } catch(Exception e) { saving=CompletableFuture.failedFuture(e); }
            saving.whenComplete((value,failure) -> {
                if(failure!=null) {
                    refund(id,cost,payment).whenComplete((r,refundError) -> {
                        busy.remove(id);
                        if(refundError!=null) plugin.getLogger().severe("需人工退款："+id+" 金额="+cost+" 原因="+refundError);
                        if(refundError!=null) plugin.onPlayer(p,() -> plugin.tell(p,"&c操作失败且退款未完成，请联系管理员，金额："+money(cost)));
                        else plugin.result(p,CompletableFuture.failedFuture(failure),"");
                    });
                } else {
                    finish(payment);
                    busy.remove(id); plugin.onPlayer(p,() -> {
                        plugin.tell(p,message);
                        plugin.sound(p,message.contains("创建成功")?"create":message.contains("推荐位")?"pin":message.contains("门票")?"ticket":"success");
                        refresh.run();
                    });
                }
            });
        });
    }
    CompletableFuture<Void> settle(Warp warp,double gross,String payment) {
        if(gross<=0) return CompletableFuture.completedFuture(null);
        double tax=plugin.getConfig().getDouble("economy.tax-percent",5);
        if(!Double.isFinite(tax)) tax=5;
        double net=Math.round(gross*(1-Math.max(0,Math.min(100,tax))/100)*100)/100.0;
        return credit(warp.owner(),net,"门票收入，地标="+warp.id()+"，原交易="+payment).whenComplete((value,error) -> {
            if(error!=null) plugin.getLogger().severe("需人工补发门票收入："+warp.owner()+" 金额="+net+" 原因="+error);
            plugin.store.income(warp.owner(),warp.id(),warp.name(),gross,net,error==null).exceptionally(e -> { plugin.getLogger().severe("门票统计保存失败：坐标="+warp.id()+" 收款人="+warp.owner()+" 票款="+gross+" 净额="+net+" 已到账="+(error==null)+" "+e); return null; });
        });
    }
}
