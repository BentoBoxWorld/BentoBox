package verification;

import com.islandwarp.*;
import fr.euphyllia.skyllia.api.SkylliaAPI;
import fr.euphyllia.skyllia.api.skyblock.*;
import fr.euphyllia.skyllia.api.skyblock.model.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Test-only: actual fakeplayer CraftPlayers, real Skyllia and local CMI balances. */
public final class RealPlayerCheck extends JavaPlugin implements Listener {
    volatile boolean cancelGuestTeleport;
    volatile int cancelledEvents;
    volatile boolean redirectGuestTeleport, revokeGuestPermission;
    volatile int guestEvents;
    @EventHandler(priority=EventPriority.LOWEST) public void changeTestTeleport(PlayerTeleportEvent e) {
        if(guest==null||!e.getPlayer().getUniqueId().equals(guest.getUniqueId())) return;
        guestEvents++;
        if(redirectGuestTeleport) e.setTo(e.getTo().clone().add(0,50,0));
        if(revokeGuestPermission) { var a=guest.addAttachment(this);a.setPermission("islandwarp.use",false);attachments.add(a); }
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void cancelTestTeleport(PlayerTeleportEvent e) { if(cancelGuestTeleport&&guest!=null&&e.getPlayer().getUniqueId().equals(guest.getUniqueId())) { cancelledEvents++;getLogger().info("TEST_TELEPORT_VETO cause="+e.getCause());e.setCancelled(true); } }
    JavaPlugin target; WarpStore store; Object menu,guard; Player owner,guest; long warp;
    final List<org.bukkit.permissions.PermissionAttachment> attachments=new ArrayList<>();
    static Object field(Object obj,String name)throws Exception {var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    static Object call(Object obj,String name,Object...args)throws Exception {
        for(var m:obj.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);try{return m.invoke(obj,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception ex)throw ex;throw e;}}
        throw new NoSuchMethodException(name);
    }
    <T>T global(Callable<T> action)throws Exception {var f=new CompletableFuture<T>();Bukkit.getGlobalRegionScheduler().run(this,t->{try{f.complete(action.call());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(20,TimeUnit.SECONDS);}
    <T>T player(Player p,Callable<T> action)throws Exception {var f=new CompletableFuture<T>();p.getScheduler().run(this,t->{try{f.complete(action.call());}catch(Throwable e){f.completeExceptionally(e);}},()->f.completeExceptionally(new IllegalStateException("retired")));return f.get(20,TimeUnit.SECONDS);}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    void waitFor(BooleanSupplier test,String name)throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);while(System.nanoTime()<end){if(test.getAsBoolean())return;Thread.sleep(100);}throw new AssertionError("timeout: "+name);}
    void pass(String name){getLogger().info("SERVER_CHECK_PASS: REAL "+name);}
    void command(Player p,String text)throws Exception {player(p,()->{p.performCommand("iw "+text);return null;});}
    void click(Player p,int slot)throws Exception {Thread.sleep(550);player(p,()->{var e=new InventoryClickEvent(p.getOpenInventory(),InventoryType.SlotType.CONTAINER,slot,ClickType.LEFT,InventoryAction.PICKUP_ALL);Bukkit.getPluginManager().callEvent(e);check(e.isCancelled(),"menu failed to protect items");return null;});}
    String label(Player p,int slot)throws Exception {return player(p,()->{var item=p.getOpenInventory().getTopInventory().getItem(slot);return item==null?"":net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());});}
    void expectAccessDenied(Player p,Warp w)throws Exception {player(p,()->{try{call(guard,"access",p,w);throw new AssertionError("access allowed");}catch(IllegalArgumentException expected){getLogger().info("Expected denial: "+expected.getMessage());}return null;});}
    @Override public void onEnable(){Bukkit.getGlobalRegionScheduler().runDelayed(this,t->new Thread(()->{
        boolean passed=false;
        try{runTests();passed=true;}catch(Throwable e){getLogger().log(java.util.logging.Level.SEVERE,"SERVER_CHECK_FAILED",e);}
        finally{
            try{
                if(warp!=0&&store.all().containsKey(warp))store.delete(warp,new UUID(0,0),true).get();
                for(Player p:Arrays.asList(owner,guest))if(p!=null&&p.isOnline())player(p,()->{p.kick(net.kyori.adventure.text.Component.text("Test finished"));return null;});
                global(()->{target.reloadConfig();return null;});
                if(passed)getLogger().info("SERVER_CHECK_ALL_PASS");
            }catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"fixture cleanup failed",e);}
            getLogger().info("SERVER_CHECK_FINISHED");
        }
    },"IW-RealPlayers").start(),100);}
    @SuppressWarnings("unchecked")void runTests()throws Exception {
        Bukkit.getPluginManager().registerEvents(this,this);
        for(String name:List.of("Skyllia","fakeplayer","CMI","Vault","CommandAPI"))check(Bukkit.getPluginManager().isPluginEnabled(name),"disabled dependency "+name);
        target=(JavaPlugin)Bukkit.getPluginManager().getPlugin("IslandWarp");store=(WarpStore)field(target,"store");menu=field(target,"menu");guard=field(target,"guard");
        World world=global(()->Bukkit.getWorld("world"));Location start=new Location(world,320.5,100,320.5);
        platform(start);
        var fp=Bukkit.getPluginManager().getPlugin("fakeplayer");var cl=fp.getClass().getClassLoader();
        var main=Class.forName("io.github.hello09x.fakeplayer.core.Main",true,cl);Object injector=main.getMethod("getInjector").invoke(null);
        var managerType=Class.forName("io.github.hello09x.fakeplayer.core.manager.FakeplayerManager",true,cl);
        Object manager=Class.forName("com.google.inject.Injector",true,cl).getMethod("getInstance",Class.class).invoke(injector,managerType);
        var spawn=managerType.getMethod("spawnAsync",org.bukkit.command.CommandSender.class,String.class,Location.class,long.class);
        owner=((CompletableFuture<Player>)global(()->spawn.invoke(manager,Bukkit.getConsoleSender(),"IWRealOwner",start,-1L))).get(40,TimeUnit.SECONDS);
        guest=((CompletableFuture<Player>)global(()->spawn.invoke(manager,Bukkit.getConsoleSender(),"IWRealGuest",start,-1L))).get(40,TimeUnit.SECONDS);
        check(owner!=null&&guest!=null&&owner.isOnline()&&guest.isOnline(),"real players not online");
        check(owner.getClass().getName().contains("CraftPlayer"),"not actual CraftPlayer");
        for(Player p:List.of(owner,guest))player(p,()->{p.setInvulnerable(true);p.setGravity(false);var a=p.addAttachment(this);a.setPermission("islandwarp.use",true);a.setPermission("islandwarp.create",true);a.setPermission("islandwarp.admin",false);attachments.add(a);return null;});
        pass("fakeplayer online and actual entity schedulers");
        command(guest,"help");check(label(guest,4).contains("使用指南")&&label(guest,7).isBlank(),"help layout or admin disclosure");
        var executions=new java.util.concurrent.atomic.AtomicInteger();
        player(guest,()->{
            Object page=guest.getOpenInventory().getTopInventory().getHolder();
            ((Map<Integer,java.util.function.Consumer<Player>>)field(page,"actions")).put(20,v -> executions.incrementAndGet());
            for(int i=0;i<10;i++) {var e=new InventoryClickEvent(guest.getOpenInventory(),InventoryType.SlotType.CONTAINER,20,ClickType.LEFT,InventoryAction.PICKUP_ALL);Bukkit.getPluginManager().callEvent(e);check(e.isCancelled(),"burst inventory unprotected");}
            return null;
        });waitFor(()->executions.get()==1,"burst first click");Thread.sleep(150);check(executions.get()==1,"rapid clicks executed repeatedly");
        click(guest,20);waitFor(()->executions.get()==2,"cooldown allows later click");
        command(guest,"help");click(guest,20);waitFor(()->{try{return label(guest,4).contains("发现岛屿");}catch(Exception e){throw new RuntimeException(e);}},"help browse navigation");
        pass("help categories/navigation/admin hidden; ten rapid clicks execute once; click works after cooldown");
        Thread.sleep(2000);
        var islandId=UUID.randomUUID();if(SkylliaAPI.getIslandByOwner(owner.getUniqueId())==null)check(Boolean.TRUE.equals(SkylliaAPI.createIsland(islandId,new IslandSettings("IWRealIsland",5,64),new Players(owner.getUniqueId(),owner.getName(),null,RoleType.OWNER))),"Skyllia createIsland failed");
        waitFor(()->SkylliaAPI.getIslandByOwner(owner.getUniqueId())!=null,"async island creation");
        Island island=SkylliaAPI.getIslandByOwner(owner.getUniqueId());check(island!=null,"island lookup failed");
        World sky=global(()->Bukkit.getWorld("sky-overworld"));check(sky!=null,"sky world missing");
        Location landing=island.getCenterLocation(sky);check(landing!=null,"island center missing");landing.setY(100);landing.setX(landing.getBlockX()+.5);landing.setZ(landing.getBlockZ()+.5);
        platform(landing);check(owner.teleportAsync(landing).get(25,TimeUnit.SECONDS),"owner island teleport failed");
        player(owner,()->{check(island.getId().equals(call(guard,"creation",owner,owner.getLocation())),"normal owner creation guard");return null;});
        var econ=global(()->Bukkit.getServicesManager().getRegistration(Economy.class).getProvider());check(econ.getName().toLowerCase().contains("cmi"),"not real CMI provider");
        global(()->{econ.createPlayerAccount(owner);econ.createPlayerAccount(guest);check(econ.depositPlayer(owner,1000).transactionSuccess(),"seed owner");check(econ.depositPlayer(guest,1000).transactionSuccess(),"seed guest");target.getConfig().set("teleport.delay-seconds",0);return null;});
        double ownerBefore=global(()->econ.getBalance(owner)),guestBefore=global(()->econ.getBalance(guest));
        command(owner,"create IW_REAL_WARP shop");check(label(owner,32).contains("支付"),"create confirmation missing");click(owner,32);
        waitFor(()->store.all().values().stream().anyMatch(w->w.name().equals("IW_REAL_WARP")),"real creation");warp=store.all().values().stream().filter(w->w.name().equals("IW_REAL_WARP")).findFirst().orElseThrow().id();
        check(store.all().get(warp).island().equals(island.getId()),"island binding missing");check(global(()->econ.getBalance(owner))==ownerBefore-100,"real create charge");
        command(owner,"detail "+warp);click(owner,33);waitFor(()->store.rating(owner.getUniqueId(),warp)==5,"real rating");click(owner,22);waitFor(()->store.favorites(owner.getUniqueId()).contains(warp),"real favorite");
        command(owner,"me");check(label(owner,3).contains("回收站"),"recycle missing");pass("ordinary island owner create/payment, real inventory clicks/rating/favorites");
        check(guest.teleportAsync(landing).get(25,TimeUnit.SECONDS),"guest staging teleport failed");
        player(guest,()->{try{call(guard,"creation",guest,guest.getLocation());throw new AssertionError("non-owner creation allowed");}catch(IllegalArgumentException expected){}return null;});
        island.setPrivateIsland(true);expectAccessDenied(guest,store.all().get(warp));island.setPrivateIsland(false);
        check(island.updateMember(new Players(guest.getUniqueId(),guest.getName(),island.getId(),RoleType.BAN)),"ban write");expectAccessDenied(guest,store.all().get(warp));
        check(island.removeMember(new Players(guest.getUniqueId(),guest.getName(),island.getId(),RoleType.BAN)),"ban remove");
        check(guest.teleportAsync(start).get(25,TimeUnit.SECONDS),"guest staging return failed");pass("real Skyllia ownership, private visitor and banned visitor gates");
        global(()->{target.getConfig().set("teleport.delay-seconds",3);target.getConfig().set("teleport.cooldown-seconds",60);return null;});
        store.ticket(warp,owner.getUniqueId(),false,true,20d).get();command(guest,"tp "+warp);
        waitFor(()->{try{return label(guest,14).contains("20.00");}catch(Exception e){throw new RuntimeException(e);}},"async real ticket preview");click(guest,32);
        Object teleport=field(target,"teleport");
        @SuppressWarnings("unchecked") Map<UUID,?> pending=(Map<UUID,?>)field(teleport,"pending");
        waitFor(()->pending.containsKey(guest.getUniqueId()),"guest countdown");
        long visitsBefore=store.all().get(warp).visits();
        global(()->{target.getConfig().set("teleport.delay-seconds",0);return null;});
        command(owner,"tp "+warp);
        waitFor(()->store.all().get(warp).visits()>visitsBefore,"second player visits during guest countdown");
        check(pending.containsKey(guest.getUniqueId()),"guest countdown cancelled by second player visit");
        waitFor(()->{try{return player(guest,()->guest.getWorld().equals(sky)&&guest.getLocation().distanceSquared(landing)<4)&&global(()->econ.getBalance(guest))==guestBefore-20;}catch(Exception e){throw new RuntimeException(e);}},"real successful paid teleport");
        waitFor(()->{try{return global(()->econ.getBalance(owner))==ownerBefore-100+19;}catch(Exception e){throw new RuntimeException(e);}},"owner CMI settlement");
        pass("actual successful cross-world teleport, CMI debit/19 credit/1 tax");
        pass("two real players: owner visit changes counter during guest paid countdown without cancellation");
        UUID guestId=guest.getUniqueId();Player previousGuest=guest;
        player(previousGuest,()->{previousGuest.kick(net.kyori.adventure.text.Component.text("Reconnect cooldown check"));return null;});
        waitFor(()->!previousGuest.isOnline(),"guest disconnected");
        Thread.sleep(1000);
        guest=((CompletableFuture<Player>)global(()->spawn.invoke(manager,Bukkit.getConsoleSender(),"IWRealGuest",start,-1L))).get(40,TimeUnit.SECONDS);
        check(guest.isOnline()&&guest.getUniqueId().equals(guestId),"reconnect changed identity");
        player(guest,()->{guest.setInvulnerable(true);guest.setGravity(false);var a=guest.addAttachment(this);a.setPermission("islandwarp.use",true);a.setPermission("islandwarp.admin",false);
            try{call(teleport,"start",guest,store.all().get(warp),20d);throw new AssertionError("reconnect bypassed cooldown");}
            catch(IllegalArgumentException expected){check(expected.getMessage().contains("冷却"),"not a cooldown rejection");}return null;});
        check(global(()->econ.getBalance(guest))==guestBefore-20,"rejected reconnect charged money");
        pass("real disconnect/reconnect retains cooldown and does not charge");
        Thread.sleep(2000);
        Object economy=field(target,"economy");double beforeRefund=global(()->econ.getBalance(guest));
        var rejectedSave=new CompletableFuture<Void>();
        player(guest,()->{call(economy,"purchase",guest,7d,(java.util.function.Supplier<CompletableFuture<?>>)()->rejectedSave,"test",(Runnable)()->{});return null;});
        waitFor(()->{try{return global(()->econ.getBalance(guest))==beforeRefund-7;}catch(Exception e){throw new RuntimeException(e);}},"CMI debit before failed save");
        rejectedSave.completeExceptionally(new IllegalArgumentException("injected pre-commit save failure"));
        waitFor(()->{try{return global(()->econ.getBalance(guest))==beforeRefund;}catch(Exception e){throw new RuntimeException(e);}},"CMI refund after rejected save");
        pass("real CMI purchase debit then refund on pre-commit save failure");
        store.comment(guest.getUniqueId(),guest.getName(),warp,"real guest comment",100,50,true).get();long comment=store.comments(warp).get(0).id();
        store.reply(owner.getUniqueId(),owner.getName(),warp,comment,"real owner reply",100,true).get();command(guest,"comments "+warp+" "+comment);command(guest,"notices");
        store.freeze(warp,"test",true,true,"real freeze").get();expectAccessDenied(guest,store.all().get(warp));store.freeze(warp,"test",true,false,"").get();
        store.block(warp,owner.getUniqueId(),guest.getUniqueId(),guest.getName(),true).get();expectAccessDenied(guest,store.all().get(warp));store.block(warp,owner.getUniqueId(),guest.getUniqueId(),guest.getName(),false).get();
        store.delete(warp,owner.getUniqueId(),false,7).get();store.restore(warp,owner.getUniqueId(),false,5,10).get();check(store.comments(warp).size()==1,"restore lost comments");pass("real player comment/notices menus, freeze/blacklist, recovery");
        detailed(econ,start,landing);
    }
    @SuppressWarnings("unchecked")void resetTeleport()throws Exception {
        Object teleport=field(target,"teleport");
        ((Map<UUID,Long>)field(teleport,"cooldown")).clear();
        global(()->{target.getConfig().set("teleport.delay-seconds",0);return null;});
    }
    void waitPayments()throws Exception { waitFor(()->{try{return store.unresolvedPayments().get(5,TimeUnit.SECONDS).isEmpty();}catch(Exception e){throw new RuntimeException(e);}},"payment ledger terminal states"); }
    void beginGuest(double price)throws Exception {
        player(guest,()->{call(field(target,"teleport"),"start",guest,store.all().get(warp),price);return null;});
    }
    void waitIdle()throws Exception {waitFor(()->{try{return !((Map<?,?>)field(field(target,"teleport"),"pending")).containsKey(guest.getUniqueId());}catch(Exception e){throw new RuntimeException(e);}},"guest pending cleanup");}
    void detailed(Economy econ,Location start,Location landing)throws Exception {
        List<String> findings=new ArrayList<>();
        waitPayments();pass("successful purchases/transfers/refunds leave no unresolved payment ledger");
        Object economy=field(target,"economy");
        double balance=global(()->econ.getBalance(guest));
        global(()->{check(econ.withdrawPlayer(guest,balance).transactionSuccess(),"empty guest balance");return null;});
        var declined=new CompletableFuture<Void>();
        player(guest,()->{call(economy,"purchase",guest,7d,(java.util.function.Supplier<CompletableFuture<?>>)()->{declined.complete(null);return CompletableFuture.completedFuture(null);},"test",(Runnable)()->{});return null;});
        waitFor(()->{try{return !((Set<?>)field(economy,"busy")).contains(guest.getUniqueId());}catch(Exception e){throw new RuntimeException(e);}},"insufficient funds releases lock");
        check(!declined.isDone()&&global(()->econ.getBalance(guest))==0,"insufficient balance delivered goods");
        waitPayments();global(()->{check(econ.depositPlayer(guest,balance).transactionSuccess(),"restore guest funds");return null;});pass("CMI insufficient funds: no delivery/no charge, busy cleared, ledger FAILED");

        var save=new CompletableFuture<Void>();
        player(guest,()->{call(economy,"purchase",guest,7d,(java.util.function.Supplier<CompletableFuture<?>>)()->save,"test",(Runnable)()->{});return null;});
        waitFor(()->{try{return global(()->econ.getBalance(guest))==balance-7;}catch(Exception e){throw new RuntimeException(e);}},"held purchase debit");
        player(guest,()->{try{call(economy,"purchase",guest,7d,(java.util.function.Supplier<CompletableFuture<?>>)()->CompletableFuture.completedFuture(null),"duplicate",(Runnable)()->{});throw new AssertionError("duplicate purchase accepted");}catch(IllegalArgumentException expected){}return null;});
        check(global(()->econ.getBalance(guest))==balance-7,"double debit");save.completeExceptionally(new IllegalArgumentException("fixture rejected save"));
        waitFor(()->{try{return global(()->econ.getBalance(guest))==balance;}catch(Exception e){throw new RuntimeException(e);}},"held purchase refund");waitPayments();pass("duplicate purchase blocked; single CMI debit/refund; ledger terminal");

        resetTeleport();check(guest.teleportAsync(start).get(25,TimeUnit.SECONDS),"stage cancellation");
        cancelGuestTeleport=true;
        player(guest,()->{var probe=new PlayerTeleportEvent(guest,guest.getLocation(),landing,PlayerTeleportEvent.TeleportCause.PLUGIN);Bukkit.getPluginManager().callEvent(probe);check(probe.isCancelled()&&cancelledEvents>0,"test veto listener not active");return null;});
        cancelledEvents=0;beginGuest(20);waitIdle();cancelGuestTeleport=false;
        waitPayments();
        check(cancelledEvents==1,"missing or duplicated standard teleport veto event: "+cancelledEvents);
            check(global(()->econ.getBalance(guest))==balance,"cancelled teleport failed to refund: actual="+global(()->econ.getBalance(guest))+" expected="+balance);
            check(player(guest,()->guest.getWorld().equals(start.getWorld())),"cancelled teleport moved player");pass("real PlayerTeleportEvent cancellation refunds CMI and closes payment ledger");

        resetTeleport();redirectGuestTeleport=true;beginGuest(20);waitIdle();redirectGuestTeleport=false;waitPayments();
        check(global(()->econ.getBalance(guest))==balance,"redirect failed to refund");
        check(player(guest,()->guest.getWorld().equals(start.getWorld())),"unchecked redirect moved guest");pass("third-party unsafe redirect refused and refunded");

        resetTeleport();revokeGuestPermission=true;beginGuest(20);waitIdle();revokeGuestPermission=false;waitPayments();
        check(global(()->econ.getBalance(guest))==balance,"permission revoked by listener failed to refund");
        check(player(guest,()->guest.getWorld().equals(start.getWorld())),"listener revoked permission but teleport succeeded");
        player(guest,()->{guest.removeAttachment(attachments.remove(attachments.size()-1));return null;});pass("permission revoked inside teleport listener rechecked and refunded");

        resetTeleport();
        var altered=new CompletableFuture<Void>();
        Bukkit.getRegionScheduler().run(this,landing,t->{landing.clone().add(0,-1,0).getBlock().setType(Material.LAVA);altered.complete(null);});altered.get(20,TimeUnit.SECONDS);
        beginGuest(20);waitIdle();waitPayments();
        check(global(()->econ.getBalance(guest))==balance,"unsafe landing charged money");
        check(player(guest,()->guest.getWorld().equals(start.getWorld())),"unsafe landing accepted");platform(landing);pass("real unsafe lava landing refused without charge");

        resetTeleport();global(()->{target.getConfig().set("teleport.delay-seconds",2);return null;});beginGuest(20);
        player(guest,()->{
            var damage=new org.bukkit.event.entity.EntityDamageEvent(guest,org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM,1);
            Bukkit.getPluginManager().callEvent(damage);
            if(damage.isCancelled()) {
                check(((Map<?,?>)field(field(target,"teleport"),"pending")).containsKey(guest.getUniqueId()),"cancelled damage wrongly cancelled teleport");
                getLogger().info("TEST_FIXTURE: another plugin vetoed damage; verify uncancelled handler directly");
                damage.setCancelled(false);call(field(target,"teleport"),"damage",damage);
            }
            return null;
        });waitIdle();waitPayments();
        check(global(()->econ.getBalance(guest))==balance,"damage cancellation charged money");pass("uncancelled damage handler cancels countdown; cancelled damage ignored; no charge");

        resetTeleport();global(()->{target.getConfig().set("teleport.delay-seconds",2);return null;});beginGuest(20);
        global(()->{call(field(target,"teleport"),"shutdown");return null;});waitIdle();Thread.sleep(2200);waitPayments();
        check(global(()->econ.getBalance(guest))==balance,"shutdown countdown charged money");
        check(player(guest,()->guest.getWorld().equals(start.getWorld())),"delayed task teleported after shutdown cancellation");pass("shutdown cancels unpaid countdown; delayed task cannot teleport or charge");

        resetTeleport();global(()->{target.getConfig().set("teleport.delay-seconds",2);return null;});beginGuest(20);
        player(guest,()->{Location from=guest.getLocation(),to=from.clone().add(1,0,0);Bukkit.getPluginManager().callEvent(new PlayerMoveEvent(guest,from,to));return null;});waitIdle();
        check(global(()->econ.getBalance(guest))==balance,"movement cancellation charged money");pass("movement event cancels countdown before debit");

        resetTeleport();global(()->{target.getConfig().set("teleport.delay-seconds",2);return null;});beginGuest(20);
        store.ticket(warp,owner.getUniqueId(),false,true,21d).get();waitIdle();
        check(global(()->econ.getBalance(guest))==balance,"changed ticket charged old price");store.ticket(warp,owner.getUniqueId(),false,true,20d).get();pass("ticket changes during countdown rejected without debit");

        global(()->{target.getConfig().set("economy.enabled",false);return null;});command(owner,"ticket "+warp+" price 50");
        waitFor(()->store.ticket(warp).price()==50,"set price while economy disabled");
        resetTeleport();int beforeEvents=guestEvents;beginGuest(0);waitIdle();check(player(guest,()->guest.getWorld().equals(landing.getWorld())),"free teleport failed");
        check(guestEvents==beforeEvents+1,"successful teleport emitted duplicate/missing event");
        check(global(()->econ.getBalance(guest))==balance,"disabled economy charged money");waitPayments();pass("economy disabled: preset ticket 50, actual free cross-world transfer, no payment residue");
        global(()->{target.getConfig().set("economy.enabled",true);return null;});store.ticket(warp,owner.getUniqueId(),false,true,20d).get();

        global(()->{target.getConfig().set("featured.slots",3);target.getConfig().set("ratings.enabled",false);return null;});
        command(guest,"");check(label(guest,13).isBlank(),"disabled featured slots still shown");
        String lore=player(guest,()->String.valueOf(call(menu,"cardLore",store.all().get(warp),false)));
        check(!lore.contains("评分"),"disabled ratings still shown in list");pass("real home menu: featured slots=3 and disabled ratings");
        global(()->{target.getConfig().set("featured.slots",7);target.getConfig().set("ratings.enabled",true);return null;});

        store.favorite(guest.getUniqueId(),warp).get();store.edit(warp,owner.getUniqueId(),false,w->w.edit(w.name(),w.description(),w.category(),w.icon(),false)).get();
        command(guest,"favorite "+warp);Thread.sleep(500);
        if(store.favorites(guest.getUniqueId()).contains(warp)) { findings.add("隐藏坐标虽然 store.favorite 允许取消，但普通玩家 /iw favorite ID 被 find 可见性检查拒绝，仍不能取消收藏");getLogger().warning("TEST_FINDING: "+findings.get(findings.size()-1)); }
        else pass("hidden warp favorite removable through real command");
        store.edit(warp,owner.getUniqueId(),false,w->w.edit(w.name(),w.description(),w.category(),w.icon(),true)).get();

        var admin=player(guest,()->{var a=guest.addAttachment(this);a.setPermission("islandwarp.admin",true);return a;});
        global(()->{target.getConfig().set("admin.manage-player-warps",false);return null;});
        command(guest,"edit "+warp+" name SHOULD_NOT_CHANGE");command(guest,"ticket "+warp+" price 99");command(guest,"delete "+warp);
        Thread.sleep(400);check(store.all().get(warp).name().equals("IW_REAL_WARP")&&store.ticket(warp).price()==20,"admin config bypassed");
        global(()->{target.getConfig().set("admin.panel-enabled",false);return null;});command(guest,"admin freeze "+warp+" config_test");
        waitFor(()->store.status(warp).frozen(),"admin subcommand while panel disabled");command(guest,"admin unfreeze "+warp);waitFor(()->!store.status(warp).frozen(),"unfreeze");
        player(guest,()->{guest.removeAttachment(admin);return null;});global(()->{target.getConfig().set("admin.manage-player-warps",true);target.getConfig().set("admin.panel-enabled",true);return null;});pass("admin manage-other switch enforced; panel-disabled freeze/unfreeze commands remain usable");

        global(()->{target.getConfig().set("economy.enabled",false);return null;});double createBalance=global(()->econ.getBalance(owner));
        command(owner,"create IW_FREE_CREATE other");waitFor(()->store.all().values().stream().anyMatch(w->w.name().equals("IW_FREE_CREATE")),"free create without confirmation");
        long free=store.all().values().stream().filter(w->w.name().equals("IW_FREE_CREATE")).findFirst().orElseThrow().id();
        check(global(()->econ.getBalance(owner))==createBalance,"free creation charged money");store.delete(free,owner.getUniqueId(),false).get();waitPayments();
        global(()->{target.getConfig().set("economy.enabled",true);return null;});pass("economy disabled creation delivered immediately, zero CMI debit, no ledger residue");

        check(guest.teleportAsync(start).get(25,TimeUnit.SECONDS),"stage offline owner transfer");resetTeleport();
        UUID ownerId=owner.getUniqueId();double ownerBalance=global(()->econ.getBalance(Bukkit.getOfflinePlayer(ownerId)));
        player(owner,()->{owner.kick(net.kyori.adventure.text.Component.text("Offline income test"));return null;});waitFor(()->!owner.isOnline(),"owner offline");
        beginGuest(20);waitIdle();waitPayments();
        check(global(()->econ.getBalance(guest))==balance-20,"offline-owner debit mismatch");
        check(global(()->econ.getBalance(Bukkit.getOfflinePlayer(ownerId)))==ownerBalance+19,"offline owner not paid");
        check(store.incomePage(ownerId,0,0).get().rows().stream().anyMatch(row->row.paid()&&row.net()==19),"income record missing");pass("actual owner offline: CMI guest debit 20 / offline credit 19 / income row paid");
        if(!findings.isEmpty()) throw new AssertionError(String.join("; ",findings));
    }
    void platform(Location at)throws Exception {
        at.getWorld().getChunkAtAsync(at).get(25,TimeUnit.SECONDS);var f=new CompletableFuture<Void>();
        Bukkit.getRegionScheduler().run(this,at,t->{try{for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){at.clone().add(x,-1,z).getBlock().setType(Material.STONE);at.clone().add(x,0,z).getBlock().setType(Material.AIR);at.clone().add(x,1,z).getBlock().setType(Material.AIR);}f.complete(null);}catch(Throwable e){f.completeExceptionally(e);}});f.get(20,TimeUnit.SECONDS);
    }
}
