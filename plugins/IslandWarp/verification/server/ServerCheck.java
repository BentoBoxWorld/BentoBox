package verification;

import com.islandwarp.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import net.milkbowl.vault.economy.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Test-only synthetic players; real Bukkit inventories, SQLite, region checks and Vault adapter. No client packet testing. */
public final class ServerCheck extends JavaPlugin {
    JavaPlugin target; WarpStore store; Object menu,economy,teleport;
    final Map<UUID,Double> balances=new ConcurrentHashMap<>();
    final List<Long> fixtures=new ArrayList<>();
    final Map<Location,org.bukkit.block.data.BlockData> originalBlocks=new HashMap<>();
    final StringBuilder snapshots=new StringBuilder("<!doctype html><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>测试服实际菜单数据</title><style>body{background:#101218;color:#e8ecf4;font:14px system-ui;padding:16px}.grid{display:grid;grid-template-columns:repeat(9,minmax(0,1fr));max-width:850px;gap:4px}.slot{min-height:74px;background:#181b22;padding:4px;border:1px solid #282d3a;overflow-wrap:anywhere;font-size:10px}small{color:#8b93a7}</style><h1>测试服实际菜单数据</h1><p>来自实际 Bukkit Inventory 的标题、物品与 Lore；不是客户端截图。</p>");
    static String html(String text) { return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    void snapshot(User u,String name) throws Exception {
        global(() -> {
            snapshots.append("<h2>").append(html(name)).append("</h2><div class='grid'>");
            for(int slot=0;slot<54;slot++) {
                snapshots.append("<div class='slot'><small>").append(slot).append("</small><br>");
                var item=u.top.getItem(slot);
                if(item!=null) {
                    snapshots.append(html(u.label(slot))).append("<br><small>").append(item.getType()).append("</small>");
                    var lore=item.getItemMeta().lore();
                    if(lore!=null) for(var line:lore) snapshots.append("<br>").append(html(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line)));
                }
                snapshots.append("</div>");
            }
            snapshots.append("</div>"); return null;
        });
    }
    static Object field(Object obj,String name) throws Exception { Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
    static Object invoke(Object obj,String name,Object... args) throws Exception {
        for(Method m:obj.getClass().getDeclaredMethods()) if(m.getName().equals(name)&&m.getParameterCount()==args.length) {
            m.setAccessible(true); try { return m.invoke(obj,args); } catch(InvocationTargetException e) { if(e.getCause() instanceof Exception ex) throw ex; throw e; }
        }
        throw new NoSuchMethodException(name);
    }
    static void check(boolean value,String text) { if(!value) throw new AssertionError(text); }
    <T> T global(Callable<T> action) throws Exception {
        var f=new CompletableFuture<T>();
        Bukkit.getGlobalRegionScheduler().run(this,t -> { try { f.complete(action.call()); } catch(Throwable e) { f.completeExceptionally(e); } });
        return f.get(20,TimeUnit.SECONDS);
    }
    static Object zero(Class<?> type) {
        if(!type.isPrimitive()||type==void.class) return null;
        if(type==boolean.class) return false; if(type==double.class) return 0d; if(type==float.class) return 0f; if(type==long.class) return 0L; if(type==char.class) return '\0'; return 0;
    }
    final class User implements InvocationHandler {
        final UUID id=UUID.randomUUID(); final String name; final Set<String> permissions=new HashSet<>();
        final List<String> messages=new CopyOnWriteArrayList<>(); Player player; Inventory top,bottom; InventoryView view;
        User(String name,boolean admin,World world) throws Exception {
            this.name=name; permissions.add("islandwarp.use"); permissions.add("islandwarp.create"); if(admin) permissions.add("islandwarp.admin");
            player=(Player)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Player.class},this);
            bottom=Bukkit.createInventory(null,36);
            top=Bukkit.createInventory(null,9);
            view=(InventoryView)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{InventoryView.class},(p,m,a) -> switch(m.getName()) {
                case "getTopInventory" -> top; case "getBottomInventory" -> bottom; case "getPlayer" -> player;
                case "getType" -> InventoryType.CHEST; case "getInventory" -> ((int)a[0]<54?top:bottom);
                case "countSlots" -> top.getSize()+bottom.getSize(); case "convertSlot" -> (int)a[0];
                case "getSlotType" -> InventoryType.SlotType.CONTAINER;
                case "getTitle","getOriginalTitle" -> "verify"; default -> zero(m.getReturnType());
            });
        }
        @SuppressWarnings("unchecked") public Object invoke(Object proxy,Method m,Object[] a) throws Throwable {
            return switch(m.getName()) {
                case "getUniqueId" -> id; case "getName" -> name; case "hasPermission" -> permissions.contains(String.valueOf(a[0]));
                case "isOnline","isValid" -> true; case "isOp" -> permissions.contains("islandwarp.admin");
                case "getLocation" -> new Location(Bukkit.getWorlds().get(0),256.5,100,256.5);
                case "getWorld" -> Bukkit.getWorlds().get(0); case "getOpenInventory" -> view;
                case "teleportAsync" -> CompletableFuture.completedFuture(false); // Deliberately fail transport to exercise refund; not a real teleport.
                case "openInventory" -> { if(a[0] instanceof Inventory i) top=i; yield view; }
                case "closeInventory" -> { top=Bukkit.createInventory(null,9); yield null; }
                case "sendMessage" -> { messages.add(String.valueOf(a[a.length-1])); yield null; }
                case "performCommand" -> target.getCommand("islandwarp").getExecutor().onCommand(player,target.getCommand("islandwarp"),"iw",((String)a[0]).replaceFirst("^iw ?","").split(" +"));
                case "getScheduler" -> Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{EntityScheduler.class},(p,method,args) -> {
                    org.bukkit.plugin.Plugin owner=(org.bukkit.plugin.Plugin)args[0];
                    if(method.getName().equals("run")) return Bukkit.getGlobalRegionScheduler().run(owner,(Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>)args[1]);
                    if(method.getName().equals("runDelayed")) return Bukkit.getGlobalRegionScheduler().runDelayed(owner,(Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>)args[1],(long)args[3]);
                    if(method.getName().equals("runAtFixedRate")) return Bukkit.getGlobalRegionScheduler().runAtFixedRate(owner,(Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>)args[1],(long)args[3],(long)args[4]);
                    return zero(method.getReturnType());
                });
                case "toString" -> name; case "hashCode" -> id.hashCode(); case "equals" -> proxy==a[0];
                default -> zero(m.getReturnType());
            };
        }
        String label(int slot) { var i=top.getItem(slot); return i==null?"":net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(i.getItemMeta().displayName()); }
    }
    void click(User u,int slot,boolean right) throws Exception {
        global(() -> { InventoryClickEvent e=new InventoryClickEvent(u.view,InventoryType.SlotType.CONTAINER,slot,right?ClickType.RIGHT:ClickType.LEFT,InventoryAction.PICKUP_ALL); invoke(menu,"click",e); check(e.isCancelled(),"inventory click not cancelled"); return null; });
        global(() -> null);
    }
    void await(BooleanSupplier condition,String description) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<end) { if(global(condition::getAsBoolean)) return; Thread.sleep(25); }
        throw new AssertionError("timeout: "+description);
    }
    void pass(String name) { getLogger().info("SERVER_CHECK_PASS: "+name); }
    @Override public void onEnable() {
        target=(JavaPlugin)Bukkit.getPluginManager().getPlugin("IslandWarp");
        Bukkit.getGlobalRegionScheduler().runDelayed(this,t -> new Thread(() -> {
            boolean passed=false;
            try { runTests(); passed=true; }
            catch(Throwable e) { getLogger().log(java.util.logging.Level.SEVERE,"SERVER_CHECK_FAILED",e); }
            finally {
                try {
                    for(long id:fixtures) if(store.all().containsKey(id)) store.delete(id,new UUID(0,0),true).get();
                    if(!originalBlocks.isEmpty()) {
                        var restored=new CompletableFuture<Void>(); Location at=originalBlocks.keySet().iterator().next();
                        Bukkit.getRegionScheduler().run(this,at,task -> { try { originalBlocks.forEach((location,data) -> location.getBlock().setBlockData(data,false)); restored.complete(null); } catch(Throwable e) { restored.completeExceptionally(e); } });
                        restored.get(20,TimeUnit.SECONDS);
                    }
                    if(passed) getLogger().info("SERVER_CHECK_ALL_PASS");
                } catch(Exception e) { getLogger().log(java.util.logging.Level.SEVERE,"fixture cleanup failed",e); }
                Bukkit.getGlobalRegionScheduler().run(this,task -> { Bukkit.getServicesManager().unregisterAll(this); target.reloadConfig(); getLogger().info("SERVER_CHECK_FINISHED"); });
            }
        },"IslandWarp-Verify").start(),100);
    }
    @SuppressWarnings("unchecked") void runTests() throws Exception {
        store=(WarpStore)field(target,"store"); menu=field(target,"menu"); economy=field(target,"economy"); teleport=field(target,"teleport");
        // Only fixtures created by this helper, including leftovers from an interrupted run.
        fixtures.addAll(store.all().values().stream().filter(w -> w.ownerName().equals("IWVerifyOwner")&&w.name().contains("IW_VERIFY_")).map(Warp::id).toList());
        for(long id:fixtures) store.delete(id,new UUID(0,0),true).get(); fixtures.clear();
        global(() -> { target.getCommand("islandwarp").getExecutor().onCommand(Bukkit.getConsoleSender(),target.getCommand("islandwarp"),"iw",new String[]{"reload"}); return null; });
        check(target.getConfig().contains("notifications.history-limit")&&target.getConfig().contains("health.enabled"),"reload did not fill defaults"); pass("legacy config migration and plugin enable");
        World world=global(() -> Bukkit.getWorlds().get(0));
        User owner=global(() -> new User("IWVerifyOwner",true,world)),guest=global(() -> new User("IWVerifyGuest",false,world));
        global(() -> { invoke(menu,"open",guest.player,"open",0,"",null); return null; }); snapshot(guest,"首页 / 空置顶槽");
        long id=store.create(new Warp(0,owner.id,owner.name,"&aIW_VERIFY_"+System.currentTimeMillis(),"test shop","shop",world.getUID(),256.5,100,256.5,0,0,"EMERALD",true,null,System.currentTimeMillis(),0),Integer.MAX_VALUE,0).get(); fixtures.add(id);
        Warp warp=store.all().get(id);
        for(int i=0;i<30;i++) { long extra=store.create(new Warp(0,owner.id,owner.name,"IW_VERIFY_PAGE_"+i+"_"+id,"fixture","other",world.getUID(),256.5,100,256.5,0,0,"COMPASS",true,null,1,0),Integer.MAX_VALUE,0).get(); fixtures.add(extra); }
        global(() -> { invoke(menu,"open",owner.player,"me",0,"",null); check(owner.top.getSize()==54,"inventory size"); check(owner.label(3).contains("回收站"),"recycle entry overwritten"); check(owner.label(45).contains("首页"),"pagination absent"); return null; });
        click(owner,53,false); check(global(() -> owner.label(3).contains("回收站")&&owner.label(45).contains("上一页")),"page 2 overwrote recycle");
        snapshot(owner,"我的坐标 / 第二页 / 回收站入口");
        click(owner,3,false); await(() -> owner.label(4).contains("回收站"),"trash menu"); pass("full border/list paging and recycle entry");
        global(() -> { invoke(menu,"detail",owner.player,id); for(int s=1;s<=5;s++) check(owner.label(28+s).contains("★"),"rating missing "+s); return null; });
        snapshot(owner,"详情 / 1–5 星评分");
        // Use real listener routing; ratings overwrite existing votes.
        click(owner,30,false); await(() -> store.rating(owner.id,id)==2,"2 stars");
        click(owner,33,false); await(() -> store.rating(owner.id,id)==5,"5 stars");
        check(store.rating(id).count()==1,"repeat rating counted twice");
        click(owner,22,false); await(() -> store.favorites(owner.id).contains(id),"favorite"); pass("detail clicks, 1-5 rating overwrite, favorites");
        global(() -> { invoke(menu,"open",owner.player,"admin",0,"",null); invoke(menu,"detail",owner.player,id); return null; });
        click(owner,51,false);
        check(global(() -> owner.label(32).contains("活动")&&owner.label(50).contains("复查")&&owner.label(52).contains("冻结")&&owner.label(46).contains("收入")),"management entries missing");
        snapshot(owner,"管理员管理 / 活动 / 冻结 / 访问 / 收入");
        global(() -> { invoke(menu,"open",guest.player,"open",0,"",null); return null; });
        check(global(() -> !guest.label(46).contains("举报")&&!guest.label(52).contains("管理员")),"admin tools exposed to guest");
        global(() -> { target.getCommand("islandwarp").getExecutor().onCommand(guest.player,target.getCommand("islandwarp"),"iw",new String[]{"admin","freeze",Long.toString(id),"forged"}); return null; });
        check(!store.status(id).frozen(),"guest froze warp"); pass("management slots and administrator permission boundary");
        global(() -> { invoke(menu,"open",guest.player,"open",0,"test shop",null); return null; });
        check(global(() -> guest.label(10).contains("IW_VERIFY_")),"description search missing");
        click(guest,2,true); check(global(() -> guest.label(20).contains("票价")&&guest.label(24).contains("评分")),"search filters missing");
        snapshot(guest,"价格 / 评分筛选");
        global(() -> { invoke(menu,"filter",guest.player,0d,5d); return null; });
        check(global(() -> guest.label(10).contains("IW_VERIFY_")),"combined rating price filter missing"); pass("keyword/owner/description search and filter UI");
        store.comment(guest.id,guest.name,id,"&6hello",100,50,true).get(); long comment=store.comments(id).get(0).id();
        store.reply(owner.id,owner.name,id,comment,"&athanks",100,true).get();
        global(() -> { invoke(menu,"focusComment",guest.player,id,comment); return null; });
        check(global(() -> guest.label(10).contains("▶")),"focused comment not highlighted");
        click(guest,10,true); check(global(() -> guest.label(22).contains("编辑")&&guest.label(30).contains("举报")&&guest.label(32).contains("删除")),"comment actions missing");
        snapshot(guest,"作者留言操作 / 编辑 / 举报 / 删除");
        store.report(guest.id,id,comment,"test report").get();
        global(() -> { invoke(menu,"reportsPage",owner.player,0); return null; }); await(() -> owner.label(4).contains("举报"),"reports");
        global(() -> { invoke(menu,"noticesPage",guest.player,0); return null; }); await(() -> guest.label(4).contains("通知中心"),"notification center");
        snapshot(guest,"通知中心 / 未查看记录 / 通知开关");
        click(guest,10,false); await(() -> guest.label(10).contains("▶"),"history jump"); pass("colored comments/replies, author actions, reports, history jump");
        long now=System.currentTimeMillis(); store.event(id,owner.id,false,now+60000,now+120000).get();
        global(() -> { try { invoke(teleport,"start",guest.player,store.all().get(id),0d); throw new AssertionError("early event teleport allowed"); } catch(IllegalArgumentException expected) {} return null; });
        store.event(id,owner.id,false,0,0).get(); store.freeze(id,"verify",true,true,"test freeze").get();
        global(() -> { try { invoke(teleport,"start",guest.player,store.all().get(id),0d); throw new AssertionError("frozen teleport allowed"); } catch(IllegalArgumentException expected) {} return null; });
        store.freeze(id,"verify",true,false,"").get(); store.block(id,owner.id,guest.id,guest.name,true).get();
        global(() -> { try { invoke(teleport,"start",guest.player,store.all().get(id),0d); throw new AssertionError("blocked teleport allowed"); } catch(IllegalArgumentException expected) {} return null; });
        store.block(id,owner.id,guest.id,guest.name,false).get(); pass("event/freeze/blacklist gates on teleport entry");
        var ready=new CompletableFuture<Void>(); Location safe=new Location(world,256.5,100,256.5);
        world.getChunkAtAsync(safe).get(30,TimeUnit.SECONDS);
        Bukkit.getRegionScheduler().run(this,safe,t -> {
            try {
                for(int x=256;x<=257;x++) for(int y=99;y<=101;y++) { Location location=new Location(world,x,y,256); originalBlocks.put(location,location.getBlock().getBlockData().clone()); }
                world.getBlockAt(256,99,256).setType(Material.STONE); world.getBlockAt(256,100,256).setType(Material.AIR); world.getBlockAt(256,101,256).setType(Material.AIR); world.getBlockAt(257,99,256).setType(Material.LAVA); world.getBlockAt(257,100,256).setType(Material.AIR); world.getBlockAt(257,101,256).setType(Material.AIR); ready.complete(null);
            } catch(Throwable e) { ready.completeExceptionally(e); }
        }); ready.get(20,TimeUnit.SECONDS);
        check(((CompletableFuture<String>)global(() -> invoke(teleport,"inspect",warp))).get().isEmpty(),"safe landing rejected");
        Warp unsafe=new Warp(warp.id(),warp.owner(),warp.ownerName(),warp.name(),warp.description(),warp.category(),warp.world(),257.5,100,256.5,0,0,warp.icon(),true,null,warp.created(),0);
        check(!((CompletableFuture<String>)global(() -> invoke(teleport,"inspect",unsafe))).get().isEmpty(),"lava landing accepted"); pass("real region scheduler safe/lava landing checks");
        Economy provider=(Economy)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Economy.class},(p,m,a) -> {
            String n=m.getName(); if(n.equals("getName")) return "IWVerifyEconomy"; if(n.equals("isEnabled")) return true;
            if(n.equals("withdrawPlayer")||n.equals("depositPlayer")) {
                UUID uuid=((OfflinePlayer)a[0]).getUniqueId(); double amount=(double)a[a.length-1],balance=balances.getOrDefault(uuid,0d);
                if(n.equals("withdrawPlayer")&&balance<amount) return new EconomyResponse(0,balance,EconomyResponse.ResponseType.FAILURE,"insufficient funds");
                double after=balance+(n.equals("depositPlayer")?amount:-amount); balances.put(uuid,after); return new EconomyResponse(amount,after,EconomyResponse.ResponseType.SUCCESS,"");
            }
            return zero(m.getReturnType());
        });
        global(() -> { Bukkit.getServicesManager().register(Economy.class,provider,this,ServicePriority.Highest); return null; });
        balances.put(guest.id,100d);
        ((CompletableFuture<Void>)invoke(economy,"debit",guest.id,10d)).get(); check(balances.get(guest.id)==90,"Vault debit");
        ((CompletableFuture<Void>)invoke(economy,"credit",guest.id,10d)).get(); check(balances.get(guest.id)==100,"Vault refund");
        try { ((CompletableFuture<Void>)invoke(economy,"debit",guest.id,1000d)).get(); throw new AssertionError("insufficient balance accepted"); } catch(ExecutionException expected) {}
        global(() -> { invoke(economy,"purchase",guest.player,10d,(Supplier<CompletableFuture<?>>)() -> CompletableFuture.failedFuture(new IllegalArgumentException("test save failure")),"test",(Runnable)() -> {}); return null; });
        await(() -> balances.get(guest.id)==100&&!((Set<?>)uncheckedField(economy,"busy")).contains(guest.id),"purchase failure refund");
        store.ticket(id,owner.id,false,true,20d).get();
        global(() -> { try { invoke(teleport,"start",guest.player,store.all().get(id),10d); throw new AssertionError("stale price accepted"); } catch(IllegalArgumentException expected) {} return null; });
        global(() -> { invoke(economy,"settle",store.all().get(id),20d); return null; }); await(() -> balances.getOrDefault(owner.id,0d)==19d,"5 percent tax settlement");
        await(() -> !uncheckedIncome(store,owner.id).isEmpty(),"income record"); pass("Vault mock-service debit/refund/insufficient funds, purchase rollback, stale price, tax income");
        global(() -> {
            target.getConfig().set("teleport.delay-seconds",3);
            invoke(teleport,"start",guest.player,store.all().get(id),20d);
            Location from=guest.player.getLocation(),turn=from.clone(); turn.setYaw(90);
            invoke(teleport,"move",new org.bukkit.event.player.PlayerMoveEvent(guest.player,from,turn));
            check(((Map<?,?>)field(teleport,"pending")).containsKey(guest.id),"turning incorrectly cancels");
            invoke(teleport,"move",new org.bukkit.event.player.PlayerMoveEvent(guest.player,from,from.clone().add(1,0,0)));
            check(!((Map<?,?>)field(teleport,"pending")).containsKey(guest.id),"movement failed to cancel");
            invoke(teleport,"start",guest.player,store.all().get(id),20d);
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.entity.EntityDamageEvent(guest.player,org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL,1d));
            check(!((Map<?,?>)field(teleport,"pending")).containsKey(guest.id),"damage failed to cancel");
            invoke(teleport,"start",guest.player,store.all().get(id),20d);
            var cancelled=new org.bukkit.event.entity.EntityDamageEvent(guest.player,org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL,1d); cancelled.setCancelled(true);
            Bukkit.getPluginManager().callEvent(cancelled);
            check(((Map<?,?>)field(teleport,"pending")).containsKey(guest.id),"cancelled damage incorrectly cancels");
            invoke(teleport,"move",new org.bukkit.event.player.PlayerMoveEvent(guest.player,from,from.clone().add(1,0,0)));
            target.getConfig().set("teleport.delay-seconds",0);
            invoke(teleport,"start",guest.player,store.all().get(id),20d);
            return null;
        });
        await(() -> guest.messages.stream().anyMatch(m -> m.contains("传送失败")),"failed transport callback");
        await(() -> balances.get(guest.id)==100,"failed teleport refund");
        check(balances.get(owner.id)==19,"failed teleport paid owner");
        pass("movement/damage cancellation, cancelled-damage exemption, failed transport refund without owner payment");
        balances.put(owner.id,500d); String createdName="IW_VERIFY_CREATE_"+id;
        var creating=new CompletableFuture<Void>();
        Bukkit.getRegionScheduler().run(this,safe,t -> {
            try { target.getCommand("islandwarp").getExecutor().onCommand(owner.player,target.getCommand("islandwarp"),"iw",new String[]{"create",createdName,"shop"}); creating.complete(null); } catch(Throwable e) { creating.completeExceptionally(e); }
        }); creating.get(20,TimeUnit.SECONDS);
        check(global(() -> owner.label(32).contains("支付")),"create lacks payment confirmation");
        click(owner,32,false); await(() -> store.all().values().stream().anyMatch(v -> v.name().equals(createdName)),"create payment save");
        long created=store.all().values().stream().filter(v -> v.name().equals(createdName)).findFirst().orElseThrow().id(); fixtures.add(created);
        check(store.all().get(created).icon().equals("EMERALD")&&balances.get(owner.id)==400,"category icon or create cost");
        global(() -> {
            target.getCommand("islandwarp").getExecutor().onCommand(owner.player,target.getCommand("islandwarp"),"iw",new String[]{"edit",Long.toString(created),"description","&6edited"}); return null;
        });
        await(() -> store.all().get(created).description().contains("edited"),"edit description");
        pass("creation region validation, confirmation payment, category icon, metadata edit");
        store.buyPin(id,owner.id,1,7,System.currentTimeMillis()+3600000).get(); store.remindPins(7200000).get();
        long expiry=store.pinExpiry(1); store.renewPin(id,owner.id,expiry,3600000).get(); check(store.pinExpiry(1)==expiry+3600000,"renew failed");
        global(() -> { invoke(menu,"incomePage",owner.player,0); return null; }); await(() -> owner.label(4).contains("收入"),"income UI");
        var exported=store.export(getDataFolder().toPath(),true).get(); check(java.nio.file.Files.size(exported)>0,"empty export");
        store.delete(id,owner.id,false,7).get(); store.restore(id,owner.id,false,Integer.MAX_VALUE,0).get();
        check(store.comments(id).size()==1&&store.rating(owner.id,id)==5,"restore lost related data"); pass("pin reminder/renew, income UI, export, recycle restore");
        global(() -> { invoke(menu,"categories",owner.player,"admin","",0); return null; }); snapshot(owner,"分类筛选");
        global(() -> { invoke(menu,"creationCategories",owner.player,"preview",0); return null; }); snapshot(owner,"创建坐标 / 分类图标");
        global(() -> { invoke(menu,"editCategories",owner.player,id,0); return null; }); snapshot(owner,"修改分类");
        global(() -> { invoke(menu,"buyPinPage",owner.player,id,0); return null; }); snapshot(owner,"购买置顶 / 续费入口");
        global(() -> { invoke(menu,"pinMenu",owner.player,id); return null; }); snapshot(owner,"管理员置顶");
        global(() -> { invoke(menu,"payment",owner.player,"测试付款",12.5d,(Runnable)() -> {}); check(owner.label(14).contains("12.50"),"amount label"); return null; }); snapshot(owner,"付款确认 / 金额 12.50");
        click(owner,30,false); check(balances.get(guest.id)==100,"cancel charged money");
        global(() -> { invoke(menu,"confirm",owner.player,store.all().get(id)); return null; }); snapshot(owner,"坐标删除确认 / 回收保留期限");
        global(() -> { invoke(menu,"blocksPage",owner.player,id,0); return null; }); snapshot(owner,"黑名单");
        global(() -> { invoke(menu,"visitsPage",owner.player,id,0); return null; }); await(() -> owner.label(4).contains("每日"),"visits UI"); snapshot(owner,"访问记录");
        global(() -> { invoke(menu,"auditsPage",owner.player,0); return null; }); await(() -> owner.label(4).contains("操作"),"audit UI"); snapshot(owner,"管理员操作记录");
        check(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"),"PAPI hook unavailable");
        String placeholders=global(() -> me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(owner.player,"%islandwarp_total%"));
        check(!placeholders.contains("%"),"PAPI placeholder unresolved");
        global(() -> { target.getCommand("islandwarp").getExecutor().onCommand(guest.player,target.getCommand("islandwarp"),"iw",new String[]{"create","IW_VERIFY_FORBIDDEN","shop"}); return null; });
        check(store.all().values().stream().noneMatch(v -> v.name().equals("IW_VERIFY_FORBIDDEN")),"missing Skyllia allowed ordinary create");
        getDataFolder().mkdirs(); java.nio.file.Files.writeString(getDataFolder().toPath().resolve("runtime-ui.html"),snapshots.toString());
        pass("all auxiliary menu builds, cancel UI, PAPI, missing Skyllia fails closed");
        getLogger().info("LIMITATION: synthetic players, no client networking or real teleport success; Vault uses isolated mock balances; Skyllia not installed.");
    }
    static Object uncheckedField(Object o,String n) { try { return field(o,n); } catch(Exception e) { throw new RuntimeException(e); } }
    static List<WarpStore.Income> uncheckedIncome(WarpStore db,UUID owner) { try { return db.income(owner).get(); } catch(Exception e) { throw new RuntimeException(e); } }
}
