package com.islandwarp;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class WarpMenu implements Listener {
    private final IslandWarpPlugin plugin;
    private record Input(long deadline, Consumer<String> action) {}
    private record View(String mode, int page, String search, String category) {}
    private final Map<UUID, View> views = new ConcurrentHashMap<>();
    private final Map<UUID,SearchFilter> filters = new ConcurrentHashMap<>();
    private final Map<UUID,Long> shared = new ConcurrentHashMap<>();
    private final Map<UUID, Input> inputs = new ConcurrentHashMap<>();
    private final Map<UUID,Long> clicks = new ConcurrentHashMap<>();
    private final Set<UUID> helpParents = ConcurrentHashMap.newKeySet();
    private static final class Page implements InventoryHolder {
        Inventory inventory;
        final UUID viewer;
        boolean admin;
         final Map<Integer, Consumer<Player>> actions = new HashMap<>();
         final Map<Integer, Consumer<Player>> rightActions = new HashMap<>();
        Page(UUID viewer) { this.viewer=viewer; }
        @Override public Inventory getInventory() { return inventory; }
    }
    WarpMenu(IslandWarpPlugin plugin) {
        this.plugin=plugin;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,t -> {
            long hours=Math.max(0,Math.min(8760,plugin.getConfig().getLong("featured.reminder-hours",24)));
            plugin.store.remindPins(hours*3600000L).whenComplete((value,error) -> {
                if(error!=null) { plugin.getLogger().warning("置顶提醒检查失败："+error); return; }
                if(plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,task -> Bukkit.getOnlinePlayers().forEach(p -> deliverNotices(p.getUniqueId())));
            });
        },20,1200);
    }

    // Full dark border: rows 0 and 5 plus the two side columns; content sits in 10-43.
    private static final int[] HEADER_SLOTS = {0,1,2,3,4,5,6,7,8};
    private static final int[] FOOTER_SLOTS = {45,46,47,48,49,50,51,52,53};
    private static final int[] SIDE_SLOTS   = {9,18,27,36,17,26,35,44};
    private static final int[] CARD_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

    int featuredSlots() { return Math.max(1,Math.min(7,plugin.getConfig().getInt("featured.slots",7))); }

    private Page page(Player p,String title) {
        Page page = new Page(p.getUniqueId());
        page.inventory=Bukkit.createInventory(page,54,IslandWarpPlugin.color(title.replace('\ue000','&')));
        band(page,HEADER_SLOTS,material("frame",Material.BLACK_STAINED_GLASS_PANE));
        band(page,FOOTER_SLOTS,material("frame",Material.BLACK_STAINED_GLASS_PANE));
        band(page,SIDE_SLOTS,material("frame",Material.BLACK_STAINED_GLASS_PANE));
        plugin.sound(p,"open");
        return page;
    }
    private void band(Page page,int[] slots,Material material) {
        for(int slot:slots) button(page,slot,material," ",List.of(),null);
    }
    private int gridSlot(int index,int count) { return MenuLayout.slot(index,count); }
    private void button(Page page,int slot,Material icon,String name,List<String> lore,Consumer<Player> action) {
        Material value=icon==null||icon.isAir()||!icon.isItem()?Material.COMPASS:icon;
        buttonItem(page,slot,new ItemStack(value),name,lore,action,false);
    }
    private final Map<String,ItemStack> headCache=new ConcurrentHashMap<>();
    private static final Map<String,String> DEFAULT_HEADS=Map.of(
            "previous","f2599bd986659b8ce2c4988525c94e19ddd39fad08a38284a197f1b70675acc",
            "next","c2f910c47da042e4aa28af6cc81cf48ac6caf37dab35f88db993accb9dfe516",
            "my-warps","40cb1e67b512ab2d4bf3d7ace0eaaf61c32cd4681ddc3987ceb326706a33fa",
            "create","171d8979c1878a05987a7faf21b56d1b744f9d068c74cffcde1ea1edad5852",
            "delete","32d41042ce99147cc38cac9e46741576e7ee791283e6fac8d3292cae2935f1f");
    private void headButton(Page page,int slot,String key,Material fallback,String name,List<String> lore,Consumer<Player> action) {
        ItemStack item=new ItemStack(fallback==null||fallback.isAir()?Material.PLAYER_HEAD:fallback);
        String texture=plugin.getConfig().getString("gui.heads.textures."+key,DEFAULT_HEADS.getOrDefault(key,""));
        if(plugin.getConfig().getBoolean("gui.heads.enabled",true)&&texture!=null&&!texture.isBlank()) {
            try {
                String hash=texture.trim().replaceFirst("^https?://textures\\.minecraft\\.net/texture/","");
                if(!hash.matches("[a-fA-F0-9]{40,64}")) throw new IllegalArgumentException("纹理必须是哈希或 textures.minecraft.net 地址");
                item=headCache.computeIfAbsent(hash,value -> {
                    ItemStack head=new ItemStack(Material.PLAYER_HEAD);
                    var meta=(org.bukkit.inventory.meta.SkullMeta)head.getItemMeta();
                    var profile=Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    var textures=profile.getTextures();
                    try { textures.setSkin(java.net.URI.create("https://textures.minecraft.net/texture/"+value).toURL()); }
                    catch(java.net.MalformedURLException error) { throw new IllegalArgumentException(error); }
                    profile.setTextures(textures); meta.setOwnerProfile(profile); head.setItemMeta(meta);
                    return head;
                }).clone();
            } catch(IllegalArgumentException error) {
                // Invalid configuration falls back to the functional material icon.
            }
        }
        buttonItem(page,slot,item,name,lore,action,false);
    }
    private void buttonItem(Page page,int slot,ItemStack item,String name,List<String> lore,Consumer<Player> action,boolean glow) {
        var meta=item.getItemMeta();
        if(glow) meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING,1,true);
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(theme(name)).decoration(TextDecoration.ITALIC,false));
        meta.addItemFlags(ItemFlag.values());
        meta.lore(lore.stream().map(text -> LegacyComponentSerializer.legacyAmpersand().deserialize(theme(text)).decoration(TextDecoration.ITALIC,false)).toList());
        item.setItemMeta(meta);
        page.inventory.setItem(slot,item); page.actions.remove(slot); page.rightActions.remove(slot); if(action!=null) page.actions.put(slot,action);
    }
    private String theme(String text) {
        return text.replace("&b",plugin.getConfig().getString("gui.accent","&b"))
                .replace("&a",plugin.getConfig().getString("gui.success-color","&a"))
                .replace("&c",plugin.getConfig().getString("gui.danger-color","&c")).replace('\ue000','&');
    }
    private String playerText(String text) { return text.replace("\ue000","").replaceAll("(?i)[&§][k-o]","").replace('§','&').replace('&','\ue000'); }
    private Material material(String key,Material fallback) {
        Material value=Material.matchMaterial(plugin.getConfig().getString("gui.materials."+key,fallback.name()));
        return value==null||!value.isItem()||value.isAir()?fallback:value;
    }
    private Material icon(String name,Material fallback) {
        Material value=Material.matchMaterial(name==null?"":name);
        return value==null||!value.isItem()||value.isAir()?fallback:value;
    }
    // A short, tidy lore block shared by every warp card.
    private List<String> cardLore(Warp w,boolean admin) {
        List<String> lore=new ArrayList<>();
        lore.add("&8"+plugin.label(w.category()));
        lore.add("");
         lore.add("&7岛主 &f"+WarpText.plain(w.ownerName()));
        lore.add("&7访问 &f"+w.visits());
        var rating=plugin.store.rating(w.id());
        if(plugin.getConfig().getBoolean("ratings.enabled",true)) lore.add("&7评分 &f"+(rating.count()==0?"暂无":String.format(Locale.ROOT,"%.2f",rating.average())+"/5 &8("+rating.count()+")"));
        plugin.store.pins().entrySet().stream().filter(e -> e.getValue()==w.id()).findFirst().ifPresent(e -> {
            long expiry=plugin.store.pinExpiry(e.getKey());
            lore.add(expiry==0?"&6置顶：永久":"&6置顶到期："+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(expiry)));
            if(expiry>0) lore.add("&6剩余："+WarpText.remaining(expiry,System.currentTimeMillis()));
        });
        var ticket=plugin.store.ticket(w.id());
        lore.add(ticket.enabled()?"&7门票 &6"+WarpEconomy.money(ticket.price()):"&7免费访问");
         if(!w.description().isEmpty()) { lore.add(""); lore.add("&7"+playerText(w.description())); }
        if(admin&&!w.visible()) { lore.add(""); lore.add("&c隐藏地标"); }
        lore.add("");
         lore.add("&b左键 · 查看详情");
        if(plugin.getConfig().getBoolean("comments.enabled",true)) lore.add("&b右键 · 此坐标留言板");
        return lore;
    }
    private List<String> statusLore(Warp w) {
        var status=plugin.store.status(w.id());
        List<String> lore=new ArrayList<>();
        var event=plugin.store.event(w.id());
        if(event!=null) {
            var format=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            lore.add("&6活动："+format.format(new Date(event.start()))+" 至 "+format.format(new Date(event.end())));
            lore.add(System.currentTimeMillis()<event.start()?"&6尚未开始":event.active(System.currentTimeMillis())?"&a活动进行中":"&8活动已结束，已隐藏");
        }
        if(status.frozen()) lore.add("&c管理员冻结："+status.reason());
        if(!status.invalid().isEmpty()) lore.add("&c检测异常："+status.invalid());
        lore.add(status.checked()==0?"&8落点尚未检测":"&8检测时间："+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(status.checked())));
        return lore;
    }
    private List<String> coordinateLore(Warp w) {
        List<String> configured=plugin.getConfig().getStringList("gui.coordinate-lore");
        if(configured.isEmpty()) configured=List.of("&7世界 &f{world}","&7坐标 &f{x} / {y} / {z}");
         String world=Bukkit.getWorld(w.world())==null?"未加载":Bukkit.getWorld(w.world()).getName();
         return configured.stream().limit(3).map(line -> line.replace("{world}",world).replace("{x}",Integer.toString((int)Math.floor(w.x()))).replace("{y}",Integer.toString((int)Math.floor(w.y()))).replace("{z}",Integer.toString((int)Math.floor(w.z())))).toList();
    }
    private void card(Page page,int slot,Warp w,boolean featured,boolean admin) {
        List<String> lore=cardLore(w,admin);
        lore.addAll(statusLore(w));
        int unread=plugin.store.unread(page.viewer,w.id());
        if(unread>0&&plugin.getConfig().getBoolean("comments.unread-enabled",true)) lore.add("&e未读留言 / 回复："+unread);
        buttonItem(page,slot,new ItemStack(icon(w.icon(),Material.COMPASS)),
                (featured?"&6&l★ ":"&b&l")+playerText(w.name()),lore,v -> detail(v,w.id()),featured);
         if(plugin.getConfig().getBoolean("comments.enabled",true)) page.rightActions.put(slot,v -> commentsPage(v,w.id(),0));
    }
    // Shared footer: prev(45) · back(49) · next(53), mirrored around the row centre.
    private void nav(Page page,int current,int pages,Runnable prev,Runnable back,Runnable next) {
        boolean hasPrev=prev!=null&&current>0, hasNext=next!=null&&current<pages-1;
        if(hasPrev) headButton(page,45,"previous",Material.ARROW,"&f上一页",List.of("&7第 "+(current+1)+" / "+pages+" 页"),v -> { plugin.sound(v,"page"); prev.run(); });
        if(back!=null) button(page,49,material("return-menu",Material.OAK_DOOR),"&f返回",List.of("&7返回上一级"),v -> back.run());
        if(hasNext) headButton(page,53,"next",Material.ARROW,"&f下一页",List.of("&7第 "+(current+1)+" / "+pages+" 页"),v -> { plugin.sound(v,"page"); next.run(); });
    }
    void updated(Player p,long id,java.util.concurrent.CompletableFuture<?> future) {
        updated(p,id,future,"success");
    }
    void updated(Player p,long id,java.util.concurrent.CompletableFuture<?> future,String successSound) {
        future.whenComplete((v,error) -> {
            plugin.result(p,error==null?java.util.concurrent.CompletableFuture.completedFuture(null):java.util.concurrent.CompletableFuture.failedFuture(error),"&a设置已保存",successSound);
            if(error==null) plugin.onPlayer(p,() -> detail(p,id));
        });
    }
    void payment(Player p,String title,double amount,Runnable action) {
        if(amount==0) { action.run(); return; }
        Page page=page(p,"&8确认付款 · "+WarpEconomy.money(amount));
        button(page,4,Material.PAPER,"&6&l付款确认",List.of("&7请核对操作与金额"),null);
        button(page,12,Material.PAPER,"&f"+title,List.of("&7请核对操作后确认付款"),null);
        button(page,14,Material.GOLD_INGOT,"&6"+WarpEconomy.money(amount),List.of("&7仅确认后扣款","&7失败会自动退还","&7税费已包含在票价内"),null);
        band(page,new int[]{19,20,21,22,23,24,25},material("frame",Material.BLACK_STAINED_GLASS_PANE));
        button(page,30,Material.GRAY_CONCRETE,"&f取消付款",List.of("&7不会扣除任何金币"),this::back);
        button(page,32,material("on",Material.LIME_CONCRETE),"&a&l确认支付",List.of("&7按上方应付金额支付并执行"),v -> { plugin.sound(v,"confirm"); v.closeInventory(); action.run(); });
        p.openInventory(page.inventory);
    }
    void travel(Player p,Warp w) {
        plugin.guard.access(p,w);
        if(plugin.getConfig().getBoolean("teleport.safety-preview",true)) {
            plugin.tell(p,"&7正在检查目标落点，尚未扣款……");
            plugin.teleport.previewSafety(p,w,safe -> {
                if(!safe) { plugin.tell(p,"&c目标落点不安全或无法加载，已取消，未扣款"); return; }
                Warp latest=plugin.store.all().get(w.id());
                if(!w.sameSettings(latest)||!plugin.canSee(p,latest)||!p.hasPermission("islandwarp.use")) { plugin.tell(p,"&c坐标或权限已变更，请重新打开"); return; }
                plugin.tell(p,"&a落点检查通过；传送前仍会再次校验");
                try { travelChecked(p,w); } catch(IllegalArgumentException e) { plugin.tell(p,"&c"+e.getMessage()); }
            });
        } else travelChecked(p,w);
    }
    private void travelChecked(Player p,Warp w) {
        plugin.guard.access(p,w);
        double price=plugin.economy.admission(p,w);
        if(price>0) {
            payment(p,"前往 "+playerText(w.name()),price,() -> plugin.teleport.start(p,w,price));
            if(plugin.getConfig().getBoolean("gui.payment-preview",true)&&p.getOpenInventory().getTopInventory().getHolder() instanceof Page page) {
                List<String> lore=new ArrayList<>(coordinateLore(w));
                lore.add("&7门票：&6"+WarpEconomy.money(price));
                var rating=plugin.store.rating(w.id());
                lore.add("&7评分：&f"+(rating.count()==0?"暂无":String.format(Locale.ROOT,"%.2f / 5",rating.average())));
                button(page,12,icon(w.icon(),Material.COMPASS),"&f"+playerText(w.name()),lore,null);
            }
        }
        else plugin.teleport.start(p,w,0);
    }
    private void back(Player p) {
        if (helpParents.remove(p.getUniqueId())) { help(p); return; }
        View view=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null));
        open(p,view.mode(),view.page(),view.search(),view.category());
    }
    private Consumer<Player> fromHelp(Consumer<Player> action) {
        return p -> { helpParents.add(p.getUniqueId()); action.accept(p); };
    }
    void help(Player p) {
        if(!p.hasPermission("islandwarp.use")) { plugin.tell(p,"&c没有地标使用权限"); return; }
        Page page=page(p,"&8岛屿地标 · 使用指南");
        button(page,4,Material.KNOWLEDGE_BOOK,"&b&l地标使用指南",List.of("&7按分类查看指令与操作","&7按钮间隔 0.5 秒，付款前请核对金额"),null);
        button(page,20,Material.COMPASS,"&b发现与传送",List.of("&f/iw &7浏览公开地标","&f/iw search 关键词 &7搜索","&f/iw filters 最高票价或- 最低评分","&f/iw tp 名称或ID &7前往地标","&7移动或受伤取消准备；失败不收门票","&b点击浏览地标"),fromHelp(v -> open(v,"open",0,"",null)));
        button(page,22,Material.NETHER_STAR,"&b收藏与评分",List.of("&f/iw favorites &7收藏列表","&f/iw favorite ID &7切换收藏","&f/iw detail ID &7详情与评分","&7在详情页选择 1–5 星","&b点击打开收藏"),fromHelp(v -> open(v,"favorites",0,"",null)));
        button(page,24,Material.WRITABLE_BOOK,"&b留言与通知",List.of("&f/iw comments ID &7留言板","&7右键坐标卡片也可打开留言板","&f/iw notices &7查看历史通知","&f/iw notifications &7切换聊天提醒","&7输入 cancel 可取消聊天输入","&b点击查看通知"),fromHelp(v -> v.performCommand("iw notices")));
        button(page,29,Material.EMERALD,"&b创建与管理",List.of("&f/iw create 名称 [分类]","&7默认仅在自己的岛屿安全落点创建","&f/iw me &7自己的坐标","&f/iw edit ID name|description|category|icon|visible|location 内容","&f/iw check ID &7复查落点","&b点击查看自己的地标"),fromHelp(v -> open(v,"me",0,"",null)));
        button(page,31,Material.GOLD_INGOT,"&b门票与推广",List.of("&f/iw ticket ID price 金额","&f/iw ticket ID enabled true或false","&f/iw income &7门票收入","&f/iw share ID &7分享公开坐标","&7详情页可购买或续费置顶","&7岛主访问免费；收入扣除配置税费","&b点击查看收入"),fromHelp(v -> v.performCommand("iw income")));
        button(page,33,Material.CHEST,"&b回收与访问管理",List.of("&f/iw delete ID &7二次确认删除","&f/iw trash &7回收站","&f/iw restore ID &7恢复坐标","&7恢复不恢复原置顶和通知","&f/iw block ID 玩家名或UUID","&f/iw unblock ID 玩家名或UUID","&f/iw event ID 开始时间 结束时间 或 off","&b点击打开回收站"),fromHelp(v -> trashPage(v,0)));
        if(p.hasPermission("islandwarp.admin")) button(page,7,Material.COMMAND_BLOCK,"&c管理员指令",List.of("&f/iw admin &7管理中心（仅此指令进入）","&f/iw admin freeze ID 原因","&f/iw admin unfreeze ID","&f/iw admin payments &7核对未完成交易","&f/iw admin export &7导出含玩家隐私的数据","&f/iw reload &7重载配置","&f/iw status &7数据库状态"),null);
        button(page,47,Material.OAK_DOOR,"&f返回主菜单",List.of("&7返回发现岛屿"),v -> { helpParents.remove(v.getUniqueId()); open(v,"open",0,"",null); });
        button(page,49,Material.BARRIER,"&f关闭帮助",List.of("&7关闭使用指南"),Player::closeInventory);
        p.openInventory(page.inventory);
    }
    private List<Warp> featuredWarps(Player p) {
        List<Warp> list=new ArrayList<>();
        var pins=plugin.store.pins();
        for(int i=1;i<=featuredSlots();i++) {
            Long id=pins.get(i); if(id==null) continue;
            Warp w=plugin.store.all().get(id);
            if(w!=null&&plugin.store.publicAvailable(w)&&plugin.canSee(p,w)) list.add(w);
        }
        return list;
    }
    void open(Player p,String mode,int requested,String search,String category) {
        if(!p.hasPermission("islandwarp.use")) return;
          if(mode.equals("admin")&&(!p.hasPermission("islandwarp.admin")||!plugin.getConfig().getBoolean("admin.panel-enabled",true))) { plugin.tell(p,"&c没有管理员权限或管理面板已关闭"); return; }
        boolean admin=mode.equals("admin");
        var favorites=plugin.store.favorites(p.getUniqueId());
        SearchFilter filter=filters.getOrDefault(p.getUniqueId(),new SearchFilter(Double.MAX_VALUE,0));
        boolean home=mode.equals("open")&&category==null&&search.isEmpty()&&filter.maxPrice()==Double.MAX_VALUE&&filter.minRating()==0;
        List<Warp> featured=home?featuredWarps(p):List.of();
        Set<Long> pinned=new HashSet<>(); for(Warp w:featured) pinned.add(w.id());
        List<Warp> rows=plugin.store.all().values().stream()
                .filter(w -> plugin.canSee(p,w)||mode.equals("favorites")&&favorites.contains(w.id()))
                .filter(w -> !mode.equals("open") || plugin.store.publicAvailable(w))
                .filter(w -> !mode.equals("me") || w.owner().equals(p.getUniqueId()))
                .filter(w -> !mode.equals("favorites") || favorites.contains(w.id()))
                .filter(w -> category==null || w.category().equals(category))
                .filter(w -> !home || !pinned.contains(w.id()))
                 .filter(w -> filter.matches(w,search,plugin.economy.admission(p,w),plugin.store.rating(w.id()).average()))
                 .sorted(Comparator.comparingDouble((Warp w) -> plugin.getConfig().getBoolean("ratings.enabled",true)&&plugin.getConfig().getBoolean("ratings.sort-by-rating",true) && plugin.store.rating(w.id()).count() >= plugin.getConfig().getInt("ratings.min-count",0) ? plugin.store.rating(w.id()).average() : 0).reversed().thenComparing(Comparator.comparingLong(Warp::visits).reversed()).thenComparingLong(Warp::id)).toList();
        int listFirstRow=home?2:1, perPage=(home?3:4)*7;
        int pages=Math.max(1,(rows.size()+perPage-1)/perPage), index=Math.max(0,Math.min(requested,pages-1));
        views.put(p.getUniqueId(),new View(mode,index,search,category));
        String section=switch(mode) { case "me" -> "我的地标"; case "favorites" -> "我的收藏"; case "admin" -> "管理中心"; default -> "发现岛屿"; };
        String title=(mode.equals("open")?plugin.getConfig().getString("gui.title","&8发现岛屿"):"&8"+section)+" · "+(index+1)+" / "+pages;
        Page page=page(p,title);
        page.admin=admin;
        if(mode.equals("me")||admin) button(page,3,Material.CHEST,"&b坐标回收站",List.of("&7过期前恢复；不恢复原置顶和通知"),v -> trashPage(v,0));
        if(admin) {
            button(page,46,Material.WRITABLE_BOOK,"&c留言举报",List.of("&7管理员处理举报"),v -> reportsPage(v,0));
            button(page,52,Material.BOOK,"&b管理员操作记录",List.of("&7查看最近 200 条操作"),v -> auditsPage(v,0));
        }
        // Header: title centred, two tools mirrored on each side.
        button(page,1,material("filter",Material.HOPPER),"&b分类筛选",List.of("&7当前：&f"+(category==null?"全部":plugin.label(category)),"&7点击切换分类"),v -> categories(v,mode,search,0));
        button(page,2,material("search",Material.SPYGLASS),"&b搜索目的地",List.of("&7匹配坐标名、岛主名和描述","&7关键词：&f"+(search.isEmpty()?"未设置":search),"&7左键输入关键词，右键价格/评分筛选","&7最高票价："+(filter.maxPrice()==Double.MAX_VALUE?"不限":WarpEconomy.money(filter.maxPrice()))+" · 最低评分："+filter.minRating()),v -> input(v,"输入坐标名、岛主名或描述关键词",text -> open(v,mode,0,text,category)));
        page.rightActions.put(2,v -> searchFilters(v));
        buttonItem(page,4,new ItemStack(Material.COMPASS),"&b&l"+section,
                List.of("&7共 &f"+rows.size()+" &7个地标","&7第 &f"+(index+1)+" &7/ &f"+pages+" &7页"),null,true);
        button(page,6,material("reset",Material.MILK_BUCKET),"&f重置搜索",List.of("&7清除关键词、票价和评分条件，保留分类"),v -> { filters.remove(v.getUniqueId()); open(v,mode,0,"",category); });
        button(page,7,Material.BOOK,"&b我的收藏",List.of("&7共 &f"+favorites.size()+" &7个收藏"),v -> open(v,"favorites",0,"",null));
        // Home page: all seven pinned slots are shown directly, including empty slots.
         if(home) for(int i=0;i<featuredSlots();i++) {
            Long id=plugin.store.pins().get(i+1); Warp pinnedWarp=id==null?null:plugin.store.all().get(id);
            if(i<featuredSlots()&&pinnedWarp!=null&&plugin.store.publicAvailable(pinnedWarp)&&plugin.canSee(p,pinnedWarp)) card(page,10+i,pinnedWarp,true,admin);
             else button(page,10+i,material("empty",Material.OAK_SIGN),"&8置顶位 "+(i+1)+(id==null?" · 空闲":" · 暂不可见"),List.of(id==null?"&7暂无地标":"&7该位置已占用，地标当前不可见"),null);
        }
        // Content: list entries run left-to-right below the pinned row on the home page.
        for(int i=index*perPage;i<Math.min(rows.size(),(index+1)*perPage);i++) {
            Warp w=rows.get(i);
            int slot=MenuLayout.slot(i-index*perPage,Math.min(perPage,rows.size()-index*perPage),listFirstRow);
            card(page,slot,w,false,admin);
            if(mode.equals("favorites")&&!plugin.canSee(p,w)) {
                button(page,slot,Material.GRAY_DYE,"&7已隐藏的收藏 #"+w.id(),List.of("&7点击移除收藏"),v -> plugin.store.favorite(v.getUniqueId(),w.id()).whenComplete((value,error) -> {
                    if(error!=null) plugin.result(v,java.util.concurrent.CompletableFuture.failedFuture(error),"");
                    else plugin.onPlayer(v,() -> open(v,mode,index,search,category));
                }));
                page.rightActions.remove(slot);
            }
        }
        if(rows.isEmpty()) button(page,31,Material.MAP,"&f没有匹配的目的地",List.of("&7换个关键词，或稍后再来看看"),null);
        // Footer: prev(45) · my(47) · close(49) · create(51) · next(53).
        boolean hasPrev=index>0, hasNext=index<pages-1;
        headButton(page,45,"previous",Material.ARROW,hasPrev?"&f上一页":"&8已是首页",List.of("&7第 "+(index+1)+" / "+pages+" 页"),hasPrev?v -> { plugin.sound(v,"page"); open(v,mode,index-1,search,category); }:null);
        button(page,48,Material.PAPER,"&b通知中心",List.of("&7查看最近通知，点击跳转对应内容",plugin.getConfig().getBoolean("comments.unread-enabled",true)?"&7留言 / 回复未读：&f"+plugin.store.unread(p.getUniqueId()):"&7通知历史独立保存","&7提醒："+(plugin.store.muted(p.getUniqueId())?"&c已屏蔽":"&a开启")),v -> noticesPage(v,0));
        if(mode.equals("me")) button(page,47,material("return-menu",Material.OAK_DOOR),"&f返回主界面",List.of("&7返回坐标首页"),v -> open(v,"open",0,"",null));
        else headButton(page,47,"my-warps",Material.CHEST,"&b我的地标",List.of("&7已创建：&f"+plugin.store.owned(p.getUniqueId())),v -> open(v,"me",0,"",null));
        button(page,49,Material.BARRIER,"&7关闭",List.of(),Player::closeInventory);
        if(p.hasPermission("islandwarp.create")) headButton(page,51,"create",material("create",Material.EMERALD),"&a创建地标",List.of("&7创建费：&6"+WarpEconomy.money(plugin.economy.cost("creation-cost",100))),v -> input(v,"输入新地标名称；cancel 取消",text -> creationCategories(v,text,0)));
        else button(page,51,Material.BARRIER,"&8创建地标",List.of("&8需要创建权限"),null);
        headButton(page,53,"next",Material.ARROW,hasNext?"&f下一页":"&8已是末页",List.of("&7第 "+(index+1)+" / "+pages+" 页"),hasNext?v -> { plugin.sound(v,"page"); open(v,mode,index+1,search,category); }:null);
        p.openInventory(page.inventory);
    }
    private void categories(Player p,String mode,String search,int requestedPage) {
        List<String> options=new ArrayList<>(); options.add(""); options.addAll(plugin.categories());
        int perPage=6;
        int pages=Math.max(1,(options.size()+perPage-1)/perPage), current=Math.max(0,Math.min(requestedPage,pages-1));
        Page page=page(p,"&8分类筛选 · "+(current+1)+" / "+pages); page.admin=mode.equals("admin");
        buttonItem(page,4,new ItemStack(material("filter",Material.HOPPER)),"&b&l选择分类",List.of("&7选择一个分类浏览","&7共 &f"+plugin.categories().size()+" &7个分类"),null,true);
        for(int i=0;i<perPage && current*perPage+i<options.size();i++) {
            String id=options.get(current*perPage+i); boolean all=id.isEmpty();
            String selected=views.getOrDefault(p.getUniqueId(),new View(mode,0,search,null)).category();
            boolean active=Objects.equals(selected,all?null:id);
            buttonItem(page,MenuLayout.filterSlot(i),new ItemStack(all?Material.COMPASS:icon(plugin.getConfig().getString("categories."+id+".icon","COMPASS"),Material.COMPASS)),
                    (active?"&a":"&b")+(all?"全部分类":plugin.label(id)),List.of(active?"&a当前筛选":all?"&7显示所有地标":"&7只看这个分类"),v -> open(v,mode,0,search,all?null:id),active);
        }
        nav(page,current,pages, () -> categories(p,mode,search,current-1), () -> back(p), () -> categories(p,mode,search,current+1));
        p.openInventory(page.inventory);
    }
    private void searchFilters(Player p) {
        SearchFilter filter=filters.getOrDefault(p.getUniqueId(),new SearchFilter(Double.MAX_VALUE,0));
        Page page=page(p,"&8价格 / 评分筛选");
        button(page,20,Material.GOLD_NUGGET,"&6最高票价",List.of("&7按你实际需付的门票筛选","&7输入非负金额；- 表示不限"),v -> input(v,"输入最高票价，或 - 取消限制",text -> {
            double price;
            try { price=text.equals("-")?Double.MAX_VALUE:Double.parseDouble(text); } catch(NumberFormatException e) { throw new IllegalArgumentException("请输入有效金额"); }
            SearchFilter current=filters.getOrDefault(v.getUniqueId(),filter);
            filter(v,price,current.minRating());
        }));
        button(page,24,Material.NETHER_STAR,"&e最低评分",List.of("&7输入 0–5；0 表示不限","&7暂无评分按 0 分处理"),v -> input(v,"输入最低评分（0–5）",text -> {
            double rating;
            try { rating=Double.parseDouble(text); } catch(NumberFormatException e) { throw new IllegalArgumentException("请输入有效评分"); }
            SearchFilter current=filters.getOrDefault(v.getUniqueId(),filter);
            filter(v,current.maxPrice(),rating);
        }));
        button(page,49,Material.OAK_DOOR,"&f返回列表",List.of(),this::back);
        p.openInventory(page.inventory);
    }
    void filter(Player p,double price,double rating) {
        filters.put(p.getUniqueId(),new SearchFilter(price,rating));
        View view=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null));
        open(p,view.mode(),0,view.search(),view.category());
    }
    private void creationCategories(Player p,String name,int requestedPage) {
        WarpText.name(name,plugin.getConfig().getInt("limits.name-length",24));
        if(!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("没有创建权限");
        List<String> categories=plugin.categories(); int perPage=5;
        int pages=Math.max(1,(categories.size()+perPage-1)/perPage), current=Math.max(0,Math.min(requestedPage,pages-1));
        Page page=page(p,"&8创建坐标 · "+WarpEconomy.money(plugin.economy.cost("creation-cost",100)));
        button(page,4,material("create",Material.EMERALD),"&a&l创建坐标",List.of("&7请选择一个分类"),null);
        button(page,13,Material.PAPER,"&f"+name,List.of("&7创建费用：&6"+WarpEconomy.money(plugin.economy.cost("creation-cost",100)),"&7选择分类后再确认付款","&7新坐标使用所选分类的图标"),null);
        band(page,new int[]{19,20,21,22,23,24,25},material("frame",Material.BLACK_STAINED_GLASS_PANE));
        for(int i=0;i<perPage && current*perPage+i<categories.size();i++) {
            String id=categories.get(current*perPage+i);
            button(page,29+i,icon(plugin.getConfig().getString("categories."+id+".icon","COMPASS"),Material.COMPASS),"&b"+plugin.label(id),List.of("&7名称：&f"+name,"&a选择后进入付款确认"),v -> {
                v.closeInventory(); v.performCommand("iw create "+name+" "+id);
            });
        }
        nav(page,current,pages, () -> creationCategories(p,name,current-1), () -> back(p), () -> creationCategories(p,name,current+1));
        p.openInventory(page.inventory);
    }
    void detail(Player p,long id) {
        Warp w=plugin.store.all().get(id); if(w==null||!plugin.canSee(p,w)) { plugin.tell(p,"&c地标不可用"); return; }
         Page page=page(p,"&8地标详情 · "+playerText(w.name()));
        boolean admin=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null)).mode().equals("admin");
        page.admin=admin;
        List<String> detailLore=new ArrayList<>(List.of("&8"+plugin.label(w.category()),"","&7岛主 &f"+w.ownerName(),"&7访问 &f"+w.visits(),
                "&7评分 &f"+(plugin.store.rating(id).count()==0?"暂无":String.format(Locale.ROOT,"%.2f / 5",plugin.store.rating(id).average())),
                "&7评价人数 &f"+plugin.store.rating(id).count(),""));
         detailLore.addAll(coordinateLore(w));
         detailLore.addAll(statusLore(w));
        if(plugin.getConfig().getBoolean("comments.enabled",true)) {
            List<WarpStore.Comment> recent=plugin.store.comments(id);
            detailLore.add("");
            detailLore.add("&b玩家留言 &f"+recent.size());
            detailLore.add("&8点击留言板查看留言与主人回复");
        }
        detailLore.addAll(List.of("",(w.description().isEmpty()?"&8暂无介绍":"&7"+playerText(w.description())),"&8地标 #"+id));
         button(page,4,Material.COMPASS,"&b&l地标详情",List.of("&7浏览 · 评价 · 留言"),null);
         buttonItem(page,13,new ItemStack(icon(w.icon(),Material.COMPASS)),"&b&l"+playerText(w.name()),detailLore,null,true);
         button(page,20,material("teleport",Material.ENDER_PEARL),"&a&l出发 · 前往岛屿",List.of("&7门票：&6"+WarpEconomy.money(plugin.economy.admission(p,w)),"&7准备：&f"+plugin.getConfig().getInt("teleport.delay-seconds",3)+" 秒","&7移动或受伤可取消","","&a点击出发"),v -> { Warp latest=plugin.store.all().get(id); if(latest==null||!plugin.canSee(v,latest))throw new IllegalArgumentException("地标不可用"); travel(v,latest); });
         if(plugin.getConfig().getBoolean("ratings.enabled",true)) {
             int currentRating=plugin.store.rating(p.getUniqueId(),id);
             for(int score=1;score<=5;score++) {
                 int selected=score;
                 String stars="★".repeat(score)+"☆".repeat(5-score);
                   buttonItem(page,MenuLayout.ratingSlot(score),new ItemStack(Material.NETHER_STAR),"&e"+stars,List.of("&7评价：&f"+score+" / 5 星",currentRating==score?"&a当前评价":"&7点击提交此评分","&8重复评价会替换原评分"),v -> { if(!plugin.getConfig().getBoolean("ratings.enabled",true)) throw new IllegalArgumentException("评分已关闭");updated(v,id,plugin.store.rate(v.getUniqueId(),id,selected)); },currentRating==score);
             }
         }
         if(plugin.getConfig().getBoolean("comments.enabled",true)) {
              button(page,24,Material.WRITABLE_BOOK,"&b留言板",List.of("&7共 &f"+plugin.store.comments(id).size()+" &7条留言","&7点击查看或发表留言"),v -> commentsPage(v,id,0));
         }
        boolean favorite=plugin.store.favorites(p.getUniqueId()).contains(id);
         button(page,22,material(favorite?"favorite-on":"favorite-off",favorite?Material.NETHER_STAR:Material.GRAY_DYE),favorite?"&e已收藏":"&7收藏",List.of(favorite?"&7点击取消收藏":"&7点击收藏此地标"),v -> plugin.store.favorite(v.getUniqueId(),id).whenComplete((added,error) -> {
            plugin.onPlayer(v,() -> {
                plugin.sound(v,error!=null?"error":Boolean.TRUE.equals(added)?"favorite-on":"favorite-off");
                plugin.tell(v,error!=null?"&c收藏操作失败，请稍后重试":Boolean.TRUE.equals(added)?"&a已加入收藏":"&7已取消收藏");
                if(v.getOpenInventory().getTopInventory()==page.inventory) detail(v,id);
            });
            if(error!=null) plugin.getLogger().warning("收藏切换失败："+error);
        }));
         boolean canManage=(admin&&p.hasPermission("islandwarp.admin")&&plugin.getConfig().getBoolean("admin.manage-player-warps",true))||(w.owner().equals(p.getUniqueId())&&p.hasPermission("islandwarp.create"));
         if(canManage) button(page,51,Material.COMPARATOR,"&b管理地标",List.of("&7编辑信息、坐标、门票与置顶","&7删除操作需要再次确认"),v -> manage(v,id));
          button(page,49,material("return-menu",Material.OAK_DOOR),"&f返回列表",List.of("&7保留搜索、分类与页码"),this::back);
          if(plugin.getConfig().getBoolean("sharing.enabled",true)) button(page,47,Material.PAPER,"&b分享坐标",List.of("&7向在线玩家发送可点击详情卡片"),v -> share(v,id));
         p.openInventory(page.inventory);
     }
     private void manage(Player p,long id) {
         Warp w=plugin.store.all().get(id);
         boolean admin=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null)).mode().equals("admin");
         boolean canManage=(admin&&p.hasPermission("islandwarp.admin")&&plugin.getConfig().getBoolean("admin.manage-player-warps",true))||(w!=null&&w.owner().equals(p.getUniqueId())&&p.hasPermission("islandwarp.create"));
         if(w==null||!plugin.canSee(p,w)||!canManage) throw new IllegalArgumentException("没有管理权限或地标不可用");
         Page page=page(p,"&8管理地标 · "+playerText(w.name())); page.admin=admin;
         button(page,4,Material.COMPARATOR,"&b&l地标管理",List.of("&7地标：&f"+playerText(w.name()),"&7岛主：&f"+w.ownerName()),null);
          button(page,13,icon(w.icon(),Material.COMPASS),"&f"+playerText(w.name()),coordinateLore(w),null);
          if(plugin.getConfig().getBoolean("visits.enabled",true)) button(page,47,Material.CLOCK,"&b访问记录",List.of("&7最近访客及近 30 天每日访问"),v -> visitsPage(v,id,0));
           if(w.owner().equals(p.getUniqueId())) button(page,48,Material.IRON_DOOR,"&c坐标黑名单",List.of("&7禁止指定玩家传送及留言","&7当前："+plugin.store.blocks(id).size()+" 人"),v -> blocksPage(v,id,0));
           if(w.owner().equals(p.getUniqueId())) button(page,46,Material.GOLD_NUGGET,"&6门票收入",List.of("&7查看自己的全部坐标收入"),v -> incomePage(v,0));
           button(page,50,Material.RECOVERY_COMPASS,"&b复查落点状态",statusLore(w),v -> v.performCommand("iw check "+id));
           button(page,32,Material.CLOCK,"&6活动时间",List.of("&7设置开始和结束时间，结束后自动隐藏","&7左键设置，右键取消活动；不会自动重新公开"),v -> input(v,"输入 开始时间 结束时间，格式：2026-10-15T18:00 2026-10-15T22:00",text -> v.performCommand("iw event "+id+" "+text)));
           page.rightActions.put(32,v -> v.performCommand("iw event "+id+" off"));
           if(admin&&p.hasPermission("islandwarp.admin")) {
               boolean frozen=plugin.store.status(id).frozen();
               button(page,52,frozen?Material.LIME_CONCRETE:Material.ICE,frozen?"&a解除冻结":"&c管理员冻结",List.of("&7冻结后禁止传送、收费、新留言/回复和编辑留言","&7保留数据，原因记录到审计日志"),v -> {
                   if(frozen) v.performCommand("iw admin unfreeze "+id);
                   else input(v,"输入冻结原因（最多 100 字）",text -> v.performCommand("iw admin freeze "+id+" "+text));
               });
           }
         if(canManage) {
            var ticket=plugin.store.ticket(id);
            button(page,20,material("edit-name",Material.NAME_TAG),"&b修改名称",List.of("&7当前：&f"+playerText(w.name())),v -> input(v,"输入新名称",text -> v.performCommand("iw edit "+id+" name "+text)));
            button(page,21,material("edit-description",Material.WRITABLE_BOOK),"&b修改描述",List.of("&7输入 - 可清空描述"),v -> input(v,"输入新描述",text -> v.performCommand("iw edit "+id+" description "+text)));
            button(page,22,material("filter",Material.HOPPER),"&b修改分类",List.of("&7当前：&f"+plugin.label(w.category())),v -> editCategories(v,id,0));
            button(page,23,Material.ITEM_FRAME,"&b使用手持物品图标",List.of("&7不会消耗手中物品"),v -> v.performCommand("iw edit "+id+" icon "+v.getInventory().getItemInMainHand().getType().name()));
            button(page,24,material(w.visible()?"on":"off",w.visible()?Material.LIME_CONCRETE:Material.RED_CONCRETE),w.visible()?"&a公开：开":"&c公开：关",List.of("&7点击切换公开状态"),v -> { Warp latest=plugin.store.all().get(id); if(latest!=null)v.performCommand("iw edit "+id+" visible "+!latest.visible()); });
            button(page,29,Material.RECOVERY_COMPASS,"&b更新为当前位置",List.of("&7需要重新校验创建范围"),v -> v.performCommand("iw edit "+id+" location"));
            button(page,30,material(ticket.enabled()?"ticket-on":"ticket-off",ticket.enabled()?Material.EMERALD_BLOCK:Material.COAL_BLOCK),ticket.enabled()?"&a门票收费：开":"&7门票收费：关",List.of("&7票价：&6"+WarpEconomy.money(ticket.price()),"&7点击切换收费状态"),v -> { var latest=plugin.store.ticket(id); v.performCommand("iw ticket "+id+" enabled "+!latest.enabled()); });
            button(page,31,material("ticket-price",Material.GOLD_NUGGET),"&6设置门票价格",List.of("&7当前：&6"+WarpEconomy.money(ticket.price()),"&7门票税率：&f"+plugin.getConfig().getDouble("economy.tax-percent",5)+"%","&7岛主访问自己地标免费"),v -> input(v,"输入门票金币价格",text -> v.performCommand("iw ticket "+id+" price "+text)));
            if(admin&&p.hasPermission("islandwarp.admin")) button(page,33,material("pin",Material.GOLD_INGOT),"&6设置精选置顶",List.of("&7选择推荐位或取消置顶"),v -> pinMenu(v,id));
            else button(page,33,material("pin",Material.GOLD_BLOCK),"&6购买推荐位",List.of("&7费用：&6"+WarpEconomy.money(plugin.economy.cost("pin-cost",1000)),"&7只能购买空闲推荐位","&7取消或删除不退款"),v -> buyPins(v,id));
            headButton(page,51,"delete",material("delete",Material.TNT),"&c删除地标",List.of("&7危险操作 · 需要二次确认"),v -> confirm(v,plugin.store.all().get(id)));
        }
         button(page,49,material("return-menu",Material.OAK_DOOR),"&f返回详情",List.of("&7返回评分与留言页面"),v -> detail(v,id));
        p.openInventory(page.inventory);
    }
      void focusComment(Player p,long warp,long comment) {
          List<WarpStore.Comment> comments=plugin.store.comments(warp);
          int index=-1;
          for(int i=0;i<comments.size();i++) if(comments.get(i).id()==comment) { index=i; break; }
          if(index<0) plugin.tell(p,"&7原留言已删除，打开该坐标留言板");
          commentsPage(p,warp,index<0?0:index/28,comment);
      }
      void commentsPage(Player p,long id,int requested) { commentsPage(p,id,requested,0); }
      private void commentsPage(Player p,long id,int requested,long focused) {
         Warp w=plugin.store.all().get(id);
         if(w==null||!plugin.canSee(p,w)||!plugin.getConfig().getBoolean("comments.enabled",true)) throw new IllegalArgumentException("留言板不可用");
         List<WarpStore.Comment> comments=plugin.store.comments(id);
         int perPage=CARD_SLOTS.length, pages=Math.max(1,(comments.size()+perPage-1)/perPage), current=Math.max(0,Math.min(requested,pages-1));
        Page page=page(p,"&8留言板 · "+playerText(w.name()));
        int unread=plugin.store.unread(p.getUniqueId(),id);
         button(page,4,Material.BOOK,"&b&l"+playerText(w.name())+" · 留言板",List.of("&7坐标主人：&f"+w.ownerName(),"&7共 &f"+comments.size()+" &7条留言","&7未读：&f"+unread,"&7第 "+(current+1)+" / "+pages+" 页"),null);
         for(int i=0;i<perPage&&current*perPage+i<comments.size();i++) {
              var comment=comments.get(current*perPage+i);
               List<String> lore=new ArrayList<>(List.of("&7"+playerText(comment.text()),"","&8"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(comment.created()))));
               if(comment.edited()>0) lore.add("&8已编辑 · "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(comment.edited())));
              for(var reply:plugin.store.replies(comment.id())) lore.add("&b主人 "+playerText(reply.author())+"：&f"+playerText(reply.text()));
              boolean canReply=w.owner().equals(p.getUniqueId())&&plugin.getConfig().getBoolean("comments.replies-enabled",true);
              if(canReply) lore.add("&e点击回复这条留言");
                int slot=gridSlot(i,Math.min(perPage,comments.size()-current*perPage));
                lore.add("&e右键：编辑 / 举报 / 删除菜单");
               buttonItem(page,slot,new ItemStack(Material.PAPER),(focused==comment.id()?"&e▶ ":"&f")+comment.author(),lore,canReply?v -> input(v,"回复 ["+comment.author()+"] 的留言",text -> postReply(v,id,comment.id(),comment.player(),text)):null,focused==comment.id());
               page.rightActions.put(slot,v -> commentActions(v,id,comment));
         }
         if(comments.isEmpty()) button(page,22,Material.OAK_SIGN,"&7还没有留言",List.of("&7点击下方书本留下第一条留言"),null);
         button(page,51,Material.WRITABLE_BOOK,"&b给此坐标留言",List.of("&7坐标：&f"+playerText(w.name()),"&7最多 "+plugin.getConfig().getInt("comments.max-length",100)+" 字"),v -> input(v,"给 ["+WarpText.plain(w.name())+"] 坐标留言",text -> postComment(v,id,text)));
         nav(page,current,pages,() -> commentsPage(p,id,current-1),() -> detail(p,id),() -> commentsPage(p,id,current+1));
          p.openInventory(page.inventory);
          plugin.store.markRead(p.getUniqueId(),id).whenComplete((value,error) -> { if(error!=null) plugin.getLogger().warning("标记留言已读失败："+error); });
      }
      private void confirmComment(Player p,long warp,WarpStore.Comment comment) {
          Page page=page(p,"&8删除留言 · 二次确认");
          button(page,13,Material.PAPER,"&f"+comment.author(),List.of("&7"+playerText(comment.text()),"&c删除后回复也会被移除"),null);
          button(page,30,Material.LIME_CONCRETE,"&a保留留言",List.of(),v -> commentsPage(v,warp,0));
          button(page,32,Material.RED_CONCRETE,"&c确认删除",List.of(),v -> {
              if(!plugin.getConfig().getBoolean("comments.delete-enabled",true)) throw new IllegalArgumentException("留言删除已关闭");
              var future=plugin.store.deleteComment(v.getUniqueId(),warp,comment.id());
              plugin.result(v,future,"&a留言已删除");
              future.whenComplete((value,error) -> { if(error==null) plugin.onPlayer(v,() -> { if(plugin.store.all().containsKey(warp)) commentsPage(v,warp,0); }); });
          });
          p.openInventory(page.inventory);
      }
      private void commentActions(Player p,long warp,WarpStore.Comment comment) {
          Warp w=plugin.store.all().get(warp);
          if(w==null||!plugin.canSee(p,w)) throw new IllegalArgumentException("坐标不可用");
          Page page=page(p,"&8留言操作");
          button(page,13,Material.PAPER,"&f"+comment.author(),List.of("&7"+playerText(comment.text())),null);
          if(plugin.getConfig().getBoolean("comments.reports-enabled",true)) button(page,30,Material.WRITABLE_BOOK,"&c举报留言",List.of("&7填写原因，由管理员审核"),v -> input(v,"输入举报原因（最多 100 字）",text -> {
              if(!plugin.getConfig().getBoolean("comments.reports-enabled",true)) throw new IllegalArgumentException("举报功能已关闭");
              plugin.result(v,plugin.store.report(v.getUniqueId(),warp,comment.id(),text),"&a举报已提交，同一留言不重复提交");
          }));
          if(plugin.getConfig().getBoolean("comments.delete-enabled",true)&&(w.owner().equals(p.getUniqueId())||comment.player().equals(p.getUniqueId()))) button(page,32,Material.TNT,"&c删除留言",List.of("&7需要再次确认"),v -> confirmComment(v,warp,comment));
          if(comment.player().equals(p.getUniqueId())&&plugin.getConfig().getBoolean("comments.edit-enabled",true)) button(page,22,Material.WRITABLE_BOOK,"&b编辑自己的留言",List.of("&7保留回复、标记已编辑；与发帖共享冷却"),v -> input(v,"输入新的留言内容",text -> {
              if(!plugin.getConfig().getBoolean("comments.enabled",true)||!plugin.getConfig().getBoolean("comments.edit-enabled",true)) throw new IllegalArgumentException("留言编辑已关闭");
              var future=plugin.store.editComment(v.getUniqueId(),warp,comment.id(),WarpText.normalize(text),plugin.getConfig().getInt("comments.max-length",100),plugin.getConfig().getInt("comments.cooldown-seconds",10));
              plugin.result(v,future,"&a留言已修改");
              future.whenComplete((value,error) -> { if(error==null) plugin.onPlayer(v,() -> { Warp latest=plugin.store.all().get(warp); if(latest!=null&&plugin.canSee(v,latest)) focusComment(v,warp,comment.id()); }); });
          }));
          button(page,49,Material.OAK_DOOR,"&f返回留言板",List.of(),v -> commentsPage(v,warp,0));
          p.openInventory(page.inventory);
      }
      private void blocksPage(Player p,long warp,int requested) {
          Warp w=plugin.store.all().get(warp);
          if(w==null||!w.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("只能管理自己坐标的黑名单");
          var rows=plugin.store.blocks(warp);
          int pages=Math.max(1,(rows.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
          Page page=page(p,"&8黑名单 · "+playerText(w.name()));
          button(page,4,Material.IRON_DOOR,"&c坐标黑名单",List.of("&7禁止名单中的玩家传送及留言","&7共 "+rows.size()+" 人"),null);
          for(int i=0;i<28&&current*28+i<rows.size();i++) {
              var row=rows.get(current*28+i);
              button(page,gridSlot(i,Math.min(28,rows.size()-current*28)),Material.PAPER,"&f"+row.name(),List.of("&7"+row.player(),"&e点击移出黑名单"),v -> {
                  if(!v.hasPermission("islandwarp.create")) throw new IllegalArgumentException("管理权限已撤销");
                  var future=plugin.store.block(warp,v.getUniqueId(),row.player(),row.name(),false);
                  plugin.result(v,future,"&a已移出黑名单");
                  future.whenComplete((value,error) -> { if(error==null) plugin.onPlayer(v,() -> { Warp latest=plugin.store.all().get(warp); if(latest!=null&&latest.owner().equals(v.getUniqueId())) blocksPage(v,warp,0); }); });
              });
          }
          button(page,51,Material.NAME_TAG,"&c添加玩家",List.of("&7输入已知玩家名或 UUID"),v -> input(v,"输入要禁止的玩家名或 UUID",text -> v.performCommand("iw block "+warp+" "+text)));
          nav(page,current,pages,() -> blocksPage(p,warp,current-1),() -> manage(p,warp),() -> blocksPage(p,warp,current+1));
          p.openInventory(page.inventory);
      }
      void share(Player p,long id) {
          plugin.store.requireUnfrozen(id);
          Warp w=plugin.store.all().get(id);
          if(!plugin.getConfig().getBoolean("sharing.enabled",true)||w==null||!plugin.store.publicAvailable(w)) throw new IllegalArgumentException("只能分享公开且处于开放时间的坐标");
          if(plugin.store.blocked(id,p.getUniqueId())) throw new IllegalArgumentException("坐标主人已禁止你访问");
           long now=System.currentTimeMillis();
           shared.entrySet().removeIf(entry -> entry.getValue()<=now);
          if(shared.getOrDefault(p.getUniqueId(),0L)>now) throw new IllegalArgumentException("分享冷却中，请稍后再试");
          shared.put(p.getUniqueId(),now+Math.max(0,Math.min(86400,plugin.getConfig().getInt("sharing.cooldown-seconds",30)))*1000L);
          var card=net.kyori.adventure.text.Component.text(p.getName()+" 分享了坐标 ["+WarpText.plain(w.name())+"] · 点击查看详情",net.kyori.adventure.text.format.NamedTextColor.AQUA)
                  .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/iw detail "+id));
          Bukkit.getGlobalRegionScheduler().run(plugin,t -> Bukkit.getOnlinePlayers().forEach(recipient -> plugin.onPlayer(recipient,() -> {
              Warp latest=plugin.store.all().get(id);
              if(latest!=null&&plugin.store.publicAvailable(latest)&&!plugin.store.status(id).frozen()&&recipient.hasPermission("islandwarp.use")&&!plugin.store.blocked(id,recipient.getUniqueId())) recipient.sendMessage(card);
          })));
      }
      void trashPage(Player p,int requested) {
          plugin.store.trash(p.getUniqueId(),p.hasPermission("islandwarp.admin")).whenComplete((rows,error) -> {
              if(error!=null) { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return; }
              plugin.onPlayer(p,() -> {
                  var visible=rows.stream().filter(r -> r.owner().equals(p.getUniqueId())||p.hasPermission("islandwarp.admin")).toList();
                  int pages=Math.max(1,(visible.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
                  Page page=page(p,"&8坐标回收站"); page.admin=p.hasPermission("islandwarp.admin");
                  button(page,4,Material.CHEST,"&b坐标回收站",List.of("&7恢复会检查名称和数量上限","&7保留留言、评分、门票、访问记录及黑名单","&7不恢复原置顶和旧通知；过期自动清理"),null);
                  for(int i=0;i<28&&current*28+i<visible.size();i++) {
                      var row=visible.get(current*28+i);
                      button(page,gridSlot(i,Math.min(28,visible.size()-current*28)),Material.PAPER,"&f"+playerText(row.name()),List.of("&7ID："+row.id(),"&7主人："+row.owner(),"&7保留："+WarpText.remaining(row.expires(),System.currentTimeMillis()),"&e点击恢复（免费）"),v -> {
                          v.performCommand("iw restore "+row.id()); v.closeInventory();
                      });
                  }
                  nav(page,current,pages,() -> trashPage(p,current-1),() -> back(p),() -> trashPage(p,current+1));
                  p.openInventory(page.inventory);
              });
          });
      }
      void incomePage(Player p,int requested) {
          long today=java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
          plugin.store.incomePage(p.getUniqueId(),requested,today).whenComplete((data,error) -> {
              if(error!=null) { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return; }
              plugin.onPlayer(p,() -> {
                  var rows=data.rows();double total=data.total(),daily=data.daily();
                  int pages=data.pages(),current=data.page();
                  Page page=page(p,"&8我的门票收入");
                  button(page,4,Material.GOLD_INGOT,"&6门票收入",List.of("&7今日到账："+WarpEconomy.money(daily),"&7累计到账："+WarpEconomy.money(total),"&7仅统计更新后成功传送的门票","&7结算失败不计入到账；无历史补录"),null);
                  for(int i=0;i<rows.size();i++) {
                      var row=rows.get(i);
                       button(page,gridSlot(i,rows.size()),Material.GOLD_NUGGET,"&f"+playerText(row.name()),List.of("&7票款："+WarpEconomy.money(row.gross()),"&7税额："+WarpEconomy.money(row.tax()),"&7净额："+WarpEconomy.money(row.net()),row.paid()?"&a已到账":"&c结算失败，请联系管理员", "&8"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(row.created()))),null);
                  }
                  nav(page,current,pages,() -> incomePage(p,current-1),() -> back(p),() -> incomePage(p,current+1));
                  p.openInventory(page.inventory);
              });
          });
      }
      void noticesPage(Player p,int requested) {
          plugin.store.history(p.getUniqueId()).whenComplete((rows,error) -> {
              if(error!=null) { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return; }
              plugin.onPlayer(p,() -> {
                  if(!p.hasPermission("islandwarp.use")) return;
                  int pages=Math.max(1,(rows.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
                  Page page=page(p,"&8通知中心 · "+(current+1)+" / "+pages);
                  long unread=rows.stream().filter(r -> !r.read()).count();
                  button(page,4,Material.BOOK,"&b通知中心",List.of("&7共 "+rows.size()+" 条 · 未查看 "+unread+" 条","&7聊天推送后仍保留；屏蔽提醒不隐藏记录","&7打开记录后标记已查看，不清空留言板未读"),null);
                  for(int i=0;i<28&&current*28+i<rows.size();i++) {
                      var row=rows.get(current*28+i);
                      String text=switch(row.kind()) {
                          case 1 -> "主人 "+row.author()+" 回复了你的留言";
                          case 2 -> "置顶将于 "+row.author()+" 到期";
                          case 3 -> "检测异常："+row.author();
                          default -> "收到 "+row.author()+" 的留言";
                      };
                      buttonItem(page,gridSlot(i,Math.min(28,rows.size()-current*28)),new ItemStack(Material.PAPER),(row.read()?"&7":"&e● ")+playerText(row.name()),List.of("&7"+text,row.created()==0?"&8旧版本待推送通知（原时间未知）":"&8"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(row.created())),row.read()?"&8已查看":"&e尚未查看","&7点击打开；历史信息以当前坐标状态为准"),v -> {
                          var future=plugin.store.readHistory(v.getUniqueId(),row.id());
                          future.whenComplete((value,readError) -> {
                              if(readError!=null) { plugin.result(v,java.util.concurrent.CompletableFuture.failedFuture(readError),""); return; }
                              plugin.onPlayer(v,() -> {
                                  if(!v.hasPermission("islandwarp.use")) return;
                                  Warp latest=plugin.store.all().get(row.warp());
                                  if(latest==null||!plugin.canSee(v,latest)) { plugin.tell(v,"&7对应坐标已删除、隐藏或不可访问，历史记录仍保留"); noticesPage(v,current); return; }
                                  try {
                                      if(row.kind()==2) renew(v,row.warp());
                                      else if(row.kind()==3) detail(v,row.warp());
                                      else if(row.comment()>0) focusComment(v,row.warp(),row.comment());
                                      else commentsPage(v,row.warp(),0);
                                  } catch(IllegalArgumentException e) { plugin.tell(v,"&7"+e.getMessage()); }
                              });
                          });
                      },!row.read());
                  }
                  if(rows.isEmpty()) button(page,22,Material.OAK_SIGN,"&7暂无通知",List.of("&7更新后产生的通知会保留在这里"),null);
                  button(page,51,Material.LEVER,plugin.store.muted(p.getUniqueId())?"&c聊天提醒：已屏蔽":"&a聊天提醒：开启",List.of("&7点击切换；中心记录与留言板未读仍保留"),v -> {
                      var future=plugin.store.mute(v.getUniqueId(),!plugin.store.muted(v.getUniqueId()));
                      plugin.result(v,future,"&a通知设置已保存");
                      future.whenComplete((value,muteError) -> { if(muteError==null) plugin.onPlayer(v,() -> { noticesPage(v,current); deliverNotices(v.getUniqueId()); }); });
                  });
                  nav(page,current,pages,() -> noticesPage(p,current-1),() -> back(p),() -> noticesPage(p,current+1));
                  p.openInventory(page.inventory);
              });
          });
      }
      private void reportsPage(Player p,int requested) {
          if(!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("需要管理员权限");
          plugin.store.reports(true).whenComplete((rows,error) -> {
              if(error!=null) { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return; }
              plugin.onPlayer(p,() -> {
                  if(!p.hasPermission("islandwarp.admin")) return;
                  int pages=Math.max(1,(rows.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
                  Page page=page(p,"&8留言举报 · "+(current+1)+" / "+pages); page.admin=true;
                  button(page,4,Material.WRITABLE_BOOK,"&c待处理举报："+rows.size(),List.of(),null);
                  for(int i=0;i<28&&current*28+i<rows.size();i++) {
                      var report=rows.get(current*28+i);
                      button(page,gridSlot(i,Math.min(28,rows.size()-current*28)),Material.PAPER,"&f举报 #"+report.id(),List.of("&7坐标 ID："+report.warp(),"&7作者："+report.author(),"&7原文："+playerText(report.text()),"&7原因："+playerText(report.reason()),"&7举报人 UUID："+report.reporter(),"&e点击处理"),v -> reportReview(v,report));
                  }
                  nav(page,current,pages,() -> reportsPage(p,current-1),() -> back(p),() -> reportsPage(p,current+1));
                  p.openInventory(page.inventory);
              });
          });
      }
      private void reportReview(Player p,WarpStore.Report report) {
          if(!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("需要管理员权限");
          Page page=page(p,"&8处理举报 #"+report.id()); page.admin=true;
          button(page,13,Material.PAPER,"&f"+report.author(),List.of("&7"+playerText(report.text()),"&c举报原因："+playerText(report.reason())),null);
          for(boolean delete:new boolean[]{false,true}) button(page,delete?32:30,delete?Material.TNT:Material.LIME_CONCRETE,delete?"&c确认删除留言并结案":"&a保留留言并结案",List.of("&7操作将记录到管理员日志"),v -> {
              var future=plugin.store.resolveReport(report.id(),v.getUniqueId(),v.hasPermission("islandwarp.admin"),delete);
              plugin.result(v,future,"&a举报已处理");
              future.whenComplete((value,error) -> { if(error==null) plugin.onPlayer(v,() -> { if(v.hasPermission("islandwarp.admin")) reportsPage(v,0); }); });
          });
          button(page,49,Material.OAK_DOOR,"&f返回",List.of(),v -> reportsPage(v,0));
          p.openInventory(page.inventory);
      }
      private void auditsPage(Player p,int requested) {
          if(!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("需要管理员权限");
          plugin.store.audits(true).whenComplete((rows,error) -> {
              if(error!=null) { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return; }
              plugin.onPlayer(p,() -> {
                  if(!p.hasPermission("islandwarp.admin")) return;
                  int pages=Math.max(1,(rows.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
                  Page page=page(p,"&8管理员操作记录"); page.admin=true;
                  button(page,4,Material.BOOK,"&b最近 200 条操作",List.of("&7第 "+(current+1)+" / "+pages+" 页"),null);
                  for(int i=0;i<28&&current*28+i<rows.size();i++) {
                      var row=rows.get(current*28+i);
                      button(page,gridSlot(i,Math.min(28,rows.size()-current*28)),Material.PAPER,"&f"+row.action()+" · "+row.name(),List.of("&7操作者："+row.actor(),"&7坐标 ID："+row.warp(),"&7"+row.detail(),"&8"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(row.created()))),null);
                  }
                  nav(page,current,pages,() -> auditsPage(p,current-1),() -> back(p),() -> auditsPage(p,current+1));
                  p.openInventory(page.inventory);
              });
          });
      }
      private void visitsPage(Player p,long warp,int requested) {
          var visits=plugin.store.visits(warp,p.getUniqueId(),p.hasPermission("islandwarp.admin"));
          visits.thenCombine(plugin.store.dailyVisits(warp,p.getUniqueId(),p.hasPermission("islandwarp.admin")),(rows,days) -> {
              plugin.onPlayer(p,() -> {
                  Warp w=plugin.store.all().get(warp);
                  if(w==null||!w.owner().equals(p.getUniqueId())&&!p.hasPermission("islandwarp.admin")) return;
                  int pages=Math.max(1,(rows.size()+27)/28),current=Math.max(0,Math.min(requested,pages-1));
                  Page page=page(p,"&8访问记录 · "+playerText(w.name()));
                  List<String> lore=new ArrayList<>(); days.forEach((day,count) -> lore.add("&7"+day+"：&f"+count));
                  button(page,4,Material.CLOCK,"&b近 30 天每日访问",lore,null);
                  for(int i=0;i<28&&current*28+i<rows.size();i++) {
                      var row=rows.get(current*28+i);
                      button(page,gridSlot(i,Math.min(28,rows.size()-current*28)),Material.PAPER,"&f"+row.visitor(),List.of("&7"+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(row.created()))),null);
                  }
                  nav(page,current,pages,() -> visitsPage(p,warp,current-1),() -> manage(p,warp),() -> visitsPage(p,warp,current+1));
                  p.openInventory(page.inventory);
              });
              return null;
          }).exceptionally(error -> { plugin.result(p,java.util.concurrent.CompletableFuture.failedFuture(error),""); return null; });
      }
      void renew(Player p,long warp) {
          Warp w=plugin.store.all().get(warp);
          if(w==null||!w.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("只能续费自己的置顶");
          long days=plugin.getConfig().getLong("featured.duration-days",30);
          if(!plugin.getConfig().getBoolean("featured.renew-enabled",true)||days<=0||days>36500) throw new IllegalArgumentException("续费关闭或期限配置无效");
          Integer slot=plugin.store.pins().entrySet().stream().filter(e -> e.getValue()==warp).map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalArgumentException("置顶已到期，请重新购买空位"));
          long expiry=plugin.store.pinExpiry(slot);
          if(expiry<=System.currentTimeMillis()) throw new IllegalArgumentException("永久置顶无需续费；过期请重新购买");
          double fee=plugin.economy.cost("pin-cost",1000);
          payment(p,"续费置顶 "+playerText(w.name())+" · "+days+" 天",fee,() -> {
              if(!p.hasPermission("islandwarp.create")||!plugin.getConfig().getBoolean("featured.renew-enabled",true)) throw new IllegalArgumentException("续费权限或功能已关闭");
              plugin.economy.purchase(p,fee,() -> plugin.store.renewPin(warp,p.getUniqueId(),expiry,days*86400000L),"&a置顶续费成功",() -> detail(p,warp));
          });
      }
     private void postComment(Player p,long id,String text) {
         if(!plugin.getConfig().getBoolean("comments.enabled",true)) throw new IllegalArgumentException("留言功能已关闭");
         Warp w=plugin.store.all().get(id);
         if(w==null||!plugin.canSee(p,w)) throw new IllegalArgumentException("坐标不可用");
         String author=p.getName();
           var future=plugin.store.comment(p.getUniqueId(),author,id,WarpText.normalize(text),plugin.getConfig().getInt("comments.max-length",100),plugin.getConfig().getInt("comments.max-per-warp",50),plugin.getConfig().getBoolean("comments.notify-owner",true),plugin.getConfig().getInt("comments.cooldown-seconds",10));
         plugin.result(p,future,"&a留言已发布");
         future.whenComplete((value,error) -> {
             if(error!=null) return;
             plugin.onPlayer(p,() -> {
                 Warp latest=plugin.store.all().get(id);
                 if(latest!=null&&plugin.canSee(p,latest)&&plugin.getConfig().getBoolean("comments.enabled",true)) commentsPage(p,id,0);
             });
              deliverNotices(w.owner());
          });
      }
      private void postReply(Player p,long warp,long comment,UUID recipient,String text) {
          if(!plugin.getConfig().getBoolean("comments.enabled",true)||!plugin.getConfig().getBoolean("comments.replies-enabled",true)) throw new IllegalArgumentException("回复功能已关闭");
           var future=plugin.store.reply(p.getUniqueId(),p.getName(),warp,comment,WarpText.normalize(text),plugin.getConfig().getInt("comments.max-length",100),plugin.getConfig().getBoolean("comments.notify-reply",true),plugin.getConfig().getInt("comments.cooldown-seconds",10));
          plugin.result(p,future,"&a回复已发布");
          future.whenComplete((value,error) -> {
              if(error!=null) return;
              plugin.onPlayer(p,() -> {
                  Warp latest=plugin.store.all().get(warp);
                  if(latest!=null&&plugin.canSee(p,latest)&&plugin.getConfig().getBoolean("comments.enabled",true)) commentsPage(p,warp,0);
              });
              deliverNotices(recipient);
          });
      }
      private final Set<UUID> delivering=ConcurrentHashMap.newKeySet();
      private void deliverNotices(UUID recipient) {
          if(!plugin.isEnabled()) return;
          Bukkit.getGlobalRegionScheduler().run(plugin,task -> {
              Player player=Bukkit.getPlayer(recipient);
              if(player==null||!delivering.add(recipient)) return;
              plugin.store.notices(recipient).whenComplete((notices,error) -> {
                  if(error!=null) { delivering.remove(recipient); plugin.getLogger().warning("读取留言通知失败："+error); return; }
                  if(notices.isEmpty()||!plugin.isEnabled()) { delivering.remove(recipient); return; }
                   var delivery=player.getScheduler().run(plugin,t -> {
                      if(!player.isOnline()||plugin.store.muted(recipient)) { delivering.remove(recipient); return; }
                      for(var notice:notices) player.sendMessage(
                              net.kyori.adventure.text.Component.text("["+WarpText.plain(notice.name())+"] 坐标"+(notice.kind()==3?"检测异常："+notice.author()+" ":notice.kind()==2?"置顶将于 "+notice.author()+" 到期 ":notice.reply()?"主人 ["+notice.author()+"] 回复了你的留言 ":"收到 ["+notice.author()+"] 的留言 "),net.kyori.adventure.text.format.NamedTextColor.AQUA)
                                      .append(net.kyori.adventure.text.Component.text("（点击查看）",net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                                              .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand((notice.kind()==3?"/iw detail ":notice.kind()==2?"/iw renew ":"/iw comments ")+notice.warp()+(notice.kind()<2&&notice.comment()>0?" "+notice.comment():"")))));
                      plugin.store.acknowledge(recipient,notices.stream().map(WarpStore.Notice::id).toList()).whenComplete((ignored,ackError) -> {
                          delivering.remove(recipient);
                          if(ackError!=null) plugin.getLogger().warning("确认留言通知失败："+ackError);
                          else deliverNotices(recipient);
                      });
                   },() -> delivering.remove(recipient));
                   if(delivery==null) delivering.remove(recipient);
              });
          });
      }
      @EventHandler public void join(PlayerJoinEvent e) { deliverNotices(e.getPlayer().getUniqueId()); }
     private void buyPins(Player p,long id) {
        Warp owned=plugin.store.all().get(id);
        if(owned==null||!owned.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("只能推广自己的地标");
        buyPinPage(p,id,0);
    }
    private void buyPinPage(Player p,long id,int pageNumber) {
        Warp owned=plugin.store.all().get(id);
        if(owned==null||!owned.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("只能推广自己的地标");
        int perPage=CARD_SLOTS.length, pages=Math.max(1,(featuredSlots()+perPage-1)/perPage);
        int current=Math.max(0,Math.min(pageNumber,pages-1));
        Page page=page(p,"&8购买推荐位 · "+(current+1)+" / "+pages);
        buttonItem(page,4,new ItemStack(Material.GOLD_INGOT),"&6&l购买推荐位",List.of("&7费用：&6"+WarpEconomy.money(plugin.economy.cost("pin-cost",1000)),"&7第 "+(current+1)+" / "+pages+" 页"),null,true);
        for(int i=0;i<perPage;i++) {
            int slot=current*perPage+i+1; if(slot>featuredSlots()) break;
             long expiry=plugin.store.pinExpiry(slot);
             boolean taken=plugin.store.pins().containsKey(slot)&&(expiry==0||expiry>System.currentTimeMillis());
            button(page,gridSlot(i,Math.min(perPage,featuredSlots()-current*perPage)),material(taken?"off":"on",taken?Material.RED_CONCRETE:Material.LIME_CONCRETE),taken?"&c推荐位 "+slot+" · 已占用":"&a推荐位 "+slot+" · 空闲",List.of(taken?"&7暂不可购买":"&7价格：&6"+WarpEconomy.money(plugin.economy.cost("pin-cost",1000))),taken?null:v -> {
                double fee=plugin.economy.cost("pin-cost",1000);
                payment(v,"购买推荐位 "+slot,fee,() -> {
                    if(!v.hasPermission("islandwarp.create")) throw new IllegalArgumentException("推广权限已撤销");
                    long days=Math.max(0,plugin.getConfig().getLong("featured.duration-days",30));
                    long expires=days==0?0:System.currentTimeMillis()+days*86400000L;
                    plugin.economy.purchase(v,fee,() -> plugin.store.buyPin(id,v.getUniqueId(),slot,featuredSlots(),expires),"&a推荐位购买成功",() -> detail(v,id));
                });
            });
        }
         if(plugin.store.pins().containsValue(id)) button(page,47,Material.BARRIER,adminLabel(p,id),List.of("&7释放推荐位，不退还购买费"),v -> {
            if(!v.hasPermission("islandwarp.create")) throw new IllegalArgumentException("没有管理权限");
             if(v.hasPermission("islandwarp.admin")) plugin.result(v,plugin.store.pin(id,0,true,featuredSlots(),v.getUniqueId().toString()),"&a已取消置顶","pin");
            else updated(v,id,plugin.store.releasePin(id,v.getUniqueId()),"pin");
        });
         if(plugin.store.pins().containsValue(id)&&plugin.store.pins().entrySet().stream().anyMatch(e -> e.getValue()==id&&plugin.store.pinExpiry(e.getKey())>0)&&plugin.getConfig().getBoolean("featured.renew-enabled",true)) button(page,51,Material.GOLD_INGOT,"&6续费置顶",List.of("&7在原期限上延长，需确认付款"),v -> renew(v,id));
        nav(page,current,pages, () -> buyPinPage(p,id,current-1), () -> detail(p,id), () -> buyPinPage(p,id,current+1));
        p.openInventory(page.inventory);
    }
    private String adminLabel(Player p,long id) {
        return p.hasPermission("islandwarp.admin")?"&c取消此地标置顶":"&c取消我的置顶";
    }
    private void pinMenu(Player p,long id) {
        if(!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("没有管理员权限");
        pinAdminPage(p,id,1);
    }
    private void pinAdminPage(Player p,long id,int pageNumber) {
        if(!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("没有管理员权限");
        int perPage=CARD_SLOTS.length, pages=Math.max(1,(featuredSlots()+perPage-1)/perPage);
        int current=Math.max(0,Math.min(pageNumber-1,pages-1));
        Page page=page(p,"&8管理推荐位 · "+(current+1)+" / "+pages); page.admin=true;
        buttonItem(page,4,new ItemStack(Material.GOLD_INGOT),"&6&l设置管理员推荐",List.of("&7此操作不收费","&7替换玩家购买位不退款"),null,true);
         if(plugin.store.pins().containsValue(id)) button(page,47,Material.BARRIER,"&c取消此地标置顶",List.of("&7释放推荐位，不退还购买费"),v -> updated(v,id,plugin.store.pin(id,0,v.hasPermission("islandwarp.admin"),featuredSlots(),v.getUniqueId().toString()),"pin"));
        for(int i=0;i<perPage;i++) {
            int slot=current*perPage+i+1; if(slot>featuredSlots()) break;
            int selected=slot; Long currentWarp=plugin.store.pins().get(slot); Warp w=currentWarp==null?null:plugin.store.all().get(currentWarp);
            button(page,gridSlot(i,Math.min(perPage,featuredSlots()-current*perPage)),Material.GOLD_INGOT,"&6推荐位 "+slot,List.of("&7当前：&f"+(w==null?"空":playerText(w.name())),"&7点击设为此地标"),v -> {
                 plugin.result(v,plugin.store.pin(id,selected,v.hasPermission("islandwarp.admin"),featuredSlots(),v.getUniqueId().toString()),"&a置顶设置已保存"); v.closeInventory();
            });
        }
        nav(page,current,pages, () -> pinAdminPage(p,id,current), () -> detail(p,id), () -> pinAdminPage(p,id,current+2));
        p.openInventory(page.inventory);
    }
    private void editCategories(Player p,long id,int requestedPage) {
        Warp w=plugin.store.all().get(id);
        if(w==null||!plugin.canSee(p,w)) throw new IllegalArgumentException("地标不可用");
        List<String> categories=plugin.categories(); int perPage=5;
        int pages=Math.max(1,(categories.size()+perPage-1)/perPage), current=Math.max(0,Math.min(requestedPage,pages-1));
        Page page=page(p,"&8修改分类 · "+(current+1)+" / "+pages);
        page.admin=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null)).mode().equals("admin");
        buttonItem(page,4,new ItemStack(material("filter",Material.HOPPER)),"&b&l修改地标分类",List.of("&7选择后立即保存","&7共 &f"+categories.size()+" &7个分类"),null,true);
        button(page,13,icon(w.icon(),Material.COMPASS),"&f"+playerText(w.name()),List.of("&7当前分类：&f"+plugin.label(w.category()),"&7选择新分类后立即保存"),null);
        band(page,new int[]{19,20,21,22,23,24,25},material("frame",Material.BLACK_STAINED_GLASS_PANE));
        for(int i=0;i<perPage && current*perPage+i<categories.size();i++) {
            String category=categories.get(current*perPage+i);
            boolean active=category.equals(w.category());
            buttonItem(page,29+i,new ItemStack(icon(plugin.getConfig().getString("categories."+category+".icon","COMPASS"),Material.COMPASS)),(active?"&a":"&b")+plugin.label(category),List.of(active?"&a当前分类":"&7点击设为此地标分类"),v -> v.performCommand("iw edit "+id+" category "+category),active);
        }
        nav(page,current,pages, () -> editCategories(p,id,current-1), () -> manage(p,id), () -> editCategories(p,id,current+1));
        p.openInventory(page.inventory);
    }
    void confirm(Player p,Warp w) {
        if(w==null)throw new IllegalArgumentException("地标已删除");
        plugin.requireManage(p,w);
        Page page=page(p,"&8确认删除");
        page.admin=views.getOrDefault(p.getUniqueId(),new View("open",0,"",null)).mode().equals("admin");
        button(page,4,Material.PAPER,"&c&l删除确认",List.of("&7请核对下方坐标与删除范围"),null);
        button(page,12,icon(w.icon(),Material.COMPASS),"&f"+playerText(w.name()),List.of("&7坐标主人：&f"+w.ownerName()),null);
         int retention=plugin.getConfig().getInt("recycle.retention-days",7);
         button(page,14,Material.TNT,retention>0?"&c删除后进入回收站":"&c删除后无法恢复",List.of(retention>0?"&7保留 "+Math.min(36500,retention)+" 天，可用 /iw trash 恢复":"&7回收站已关闭，关联数据永久删除","&7原置顶位释放，旧通知不恢复"),null);
        band(page,new int[]{19,20,21,22,23,24,25},material("frame",Material.BLACK_STAINED_GLASS_PANE));
         button(page,32,material("off",Material.RED_CONCRETE),"&c&l确认删除",List.of("&7以当前回收站保留配置为准"),v -> {
             plugin.requireManage(v,w);
            v.closeInventory(); plugin.result(v,plugin.store.delete(w.id(),v.getUniqueId(),v.hasPermission("islandwarp.admin"),plugin.getConfig().getInt("recycle.retention-days",7)),"&a地标已删除，可用 /iw trash 查看未过期的回收记录");
        });
        button(page,30,material("on",Material.LIME_CONCRETE),"&a&l保留坐标",List.of("&7取消删除，返回地标详情"),v -> detail(v,w.id()));
        p.openInventory(page.inventory);
    }
    private void input(Player p,String prompt,Consumer<String> action) {
        p.closeInventory(); Input request=new Input(System.currentTimeMillis()+60000,action); inputs.put(p.getUniqueId(),request);
        plugin.tell(p,"&e"+prompt+"；60 秒内有效。输入 cancel 取消。");
        p.getScheduler().runDelayed(plugin,t -> { if(inputs.remove(p.getUniqueId(),request))plugin.tell(p,"&7输入已超时"); },() -> inputs.remove(p.getUniqueId(),request),1200);
    }
    @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncChatEvent e) {
        Input request=inputs.remove(e.getPlayer().getUniqueId()); if(request==null)return;
        e.setCancelled(true); String text=PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        plugin.onPlayer(e.getPlayer(),() -> {
            if(!e.getPlayer().hasPermission("islandwarp.use")) { plugin.tell(e.getPlayer(),"&c地标权限已撤销"); return; }
            if(System.currentTimeMillis()>request.deadline()||text.equalsIgnoreCase("cancel")) { plugin.tell(e.getPlayer(),"&7输入已取消"); back(e.getPlayer()); return; }
            if(text.isEmpty()||text.length()>200 || text.contains("\n")||text.contains("\r")) { plugin.tell(e.getPlayer(),"&c输入为空或过长"); return; }
            try { request.action().accept(text); } catch(IllegalArgumentException error) { plugin.tell(e.getPlayer(),"&c"+error.getMessage()); }
        });
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Page page))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||!page.viewer.equals(p.getUniqueId()))return;
        Consumer<Player> action=e.isRightClick()?page.rightActions.getOrDefault(e.getRawSlot(),page.actions.get(e.getRawSlot())):page.actions.get(e.getRawSlot()); if(action==null)return;
        if(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT)return;
        long now=System.nanoTime();
        Long last=clicks.get(p.getUniqueId());
        if(last!=null&&now-last<500_000_000L) {
            p.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&6请稍候 &8· &f"+String.format(Locale.ROOT,"%.1f",Math.ceil((500_000_000L-(now-last))/100_000_000.0)/10)+" &7秒后可再次点击"));
            return;
        }
        clicks.put(p.getUniqueId(),now);
        plugin.onPlayer(p,() -> {
            if(p.getOpenInventory().getTopInventory()!=page.inventory||!p.hasPermission("islandwarp.use"))return;
            if(page.admin&&!p.hasPermission("islandwarp.admin")) { p.closeInventory(); return; }
            try { plugin.sound(p,"click"); action.accept(p); } catch(IllegalArgumentException error) { plugin.sound(p,"error"); plugin.tell(p,"&c"+error.getMessage()); }
        });
    }
    @EventHandler public void drag(InventoryDragEvent e) { if(e.getView().getTopInventory().getHolder() instanceof Page)e.setCancelled(true); }
      @EventHandler public void quit(PlayerQuitEvent e) { UUID id=e.getPlayer().getUniqueId(); clicks.remove(id); inputs.remove(id); views.remove(id); filters.remove(id); helpParents.remove(id); shared.entrySet().removeIf(entry -> entry.getValue()<=System.currentTimeMillis()); delivering.remove(id); }
}
