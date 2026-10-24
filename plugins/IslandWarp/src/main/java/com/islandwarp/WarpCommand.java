package com.islandwarp;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

final class WarpCommand implements CommandExecutor, TabCompleter {
    private final IslandWarpPlugin plugin;
    WarpCommand(IslandWarpPlugin plugin) { this.plugin=plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("islandwarp.use")&&!sender.hasPermission("islandwarp.admin")) { sender.sendMessage("你没有地标权限"); return true; }
        String sub = args.length==0 ? "open" : args[0].toLowerCase(Locale.ROOT);
        try {
            if (sub.equals("help")) {
                if(sender instanceof Player p) { plugin.menu.help(p); return true; }
                sender.sendMessage(IslandWarpPlugin.color("&b/iw &7浏览 | /iw me 个人 | /iw favorites 收藏 | /iw search 关键词"));
                sender.sendMessage(IslandWarpPlugin.color("&b/iw create 名称 [分类] &7创建 | /iw tp 名称或ID &7传送"));
                sender.sendMessage(IslandWarpPlugin.color("&b/iw trash | /iw restore ID &7回收站和恢复 | /iw income &7门票收入 | /iw share ID &7分享"));
                sender.sendMessage(IslandWarpPlugin.color("&b/iw edit ID name|description|category|icon|visible|location 内容"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw delete ID &7删除需再次确认 | /iw favorite ID &7收藏开关"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw comments ID &7查看该坐标留言板，也可右键点击坐标图标"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw notices &7通知中心 | /iw notifications &7切换聊天提醒"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw filters 最高票价或- 最低评分 &7搜索筛选 | /iw event ID 开始时间 结束时间 或 off &7活动时间"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw block ID 玩家名或UUID | /iw unblock ID 玩家名或UUID &7坐标黑名单"));
                 if(sender.hasPermission("islandwarp.admin")) sender.sendMessage(IslandWarpPlugin.color("&b/iw admin export &7导出坐标、留言、回复、状态和审计 CSV"));
                 sender.sendMessage(IslandWarpPlugin.color("&b/iw check ID &7主人或管理员复查落点"));
                 if(sender.hasPermission("islandwarp.admin")) sender.sendMessage(IslandWarpPlugin.color("&b/iw admin freeze ID 原因 | /iw admin unfreeze ID &7冻结/解冻 | /iw admin payments &7核对未完成交易"));
                sender.sendMessage(IslandWarpPlugin.color("&b/iw ticket ID price 金额 &7设置票价 | /iw ticket ID enabled true或false &7收费开关"));
                return true;
            }
            if (Set.of("admin","reload","status").contains(sub) && !sender.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("没有管理员权限");
            if (sub.equals("reload")) { plugin.reloadConfigWithDefaults(); sender.sendMessage("IslandWarp 配置已重载，缺失配置已自动补全"); return true; }
            if (sub.equals("status")) { sender.sendMessage("地标数："+plugin.store.all().size()+"，SQLite："+(plugin.store.healthy()?"正常":"异常")); return true; }
            if (sub.equals("admin") && args.length==1&&!plugin.getConfig().getBoolean("admin.panel-enabled",true)) throw new IllegalArgumentException("管理员面板已关闭");
            if (sub.equals("admin") && args.length > 1) {
                if(args[1].equals("payments")) {
                    if(args.length==2) plugin.store.unresolvedPayments().whenComplete((rows,error) -> {
                        Runnable reply=() -> {
                            if(error!=null) { sender.sendMessage("读取交易失败，请检查日志");return; }
                            sender.sendMessage("待核对交易："+rows.size()+"；先核对经济日志和实际交付，不要直接重复退款");
                            rows.forEach(row -> sender.sendMessage(row.toString()));
                        };
                        if(sender instanceof Player player) plugin.onPlayer(player,reply);
                        else if(plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,t -> reply.run());
                    });
                    else {
                        require(args,5,"/iw admin payments ID DONE|FAILED confirm（人工核对后标记，不执行转账）");
                        if(!Set.of("DONE","FAILED").contains(args[3])||!args[4].equals("confirm")) throw new IllegalArgumentException("先人工核对，再填写 DONE 或 FAILED confirm；此命令不会转账");
                        plugin.store.reconcilePayment(args[2],args[3],sender.getName()).whenComplete((value,error) -> {
                            Runnable reply=() -> sender.sendMessage(error==null?"核对状态已保存，未执行任何转账":"标记失败，请检查交易 ID 和日志");
                            if(sender instanceof Player player) plugin.onPlayer(player,reply);
                            else if(plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,t -> reply.run());
                        });
                    }
                    return true;
                }
                if(Set.of("freeze","unfreeze").contains(args[1])) {
                    boolean frozen=args[1].equals("freeze");
                    require(args,frozen?4:3,frozen?"/iw admin freeze ID 原因":"/iw admin unfreeze ID");
                    long id; try { id=Long.parseLong(args[2]); } catch(NumberFormatException e) { throw new IllegalArgumentException("ID 必须是数字"); }
                    String reason=String.join(" ",Arrays.copyOfRange(args,3,args.length));
                    plugin.store.freeze(id,sender.getName(),sender.hasPermission("islandwarp.admin"),frozen,reason).whenComplete((value,error) -> {
                        Runnable reply=() -> sender.sendMessage(error==null?(frozen?"坐标已冻结，禁止传送、收费和留言":"坐标已解冻"):"操作失败："+(error.getCause()==null?error.getMessage():error.getCause().getMessage()));
                        if(sender instanceof Player player) plugin.onPlayer(player,reply);
                        else if(plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,t -> reply.run());
                    });
                    return true;
                }
                if(args[1].equals("export")) {
                    if(!plugin.getConfig().getBoolean("admin.export-enabled",true)) throw new IllegalArgumentException("数据导出已关闭");
                    plugin.store.export(plugin.getDataFolder().toPath().resolve("exports"),sender.hasPermission("islandwarp.admin")).whenComplete((file,error) -> {
                        Runnable reply=() -> sender.sendMessage(error==null?"导出完成（仅服务器本地）："+file:"导出失败，请检查日志");
                        if(error!=null) plugin.getLogger().log(java.util.logging.Level.WARNING,"导出失败",error);
                        if(sender instanceof Player player) plugin.onPlayer(player,reply);
                        else if(plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,t -> reply.run());
                    });
                    return true;
                }
                if (args[1].equals("pin")) {
                    require(args,4,"/iw admin pin ID 位置（0取消）");
                    long id; int slot;
                    try { id=Long.parseLong(args[2]); slot=Integer.parseInt(args[3]); }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("ID 与置顶位必须是数字"); }
                    plugin.store.pin(id,slot,true,plugin.menu.featuredSlots(),sender instanceof Player player?player.getUniqueId().toString():sender.getName()).whenComplete((v,error) -> {
                        Runnable reply = () -> sender.sendMessage(error == null ? "置顶设置已保存" : "置顶失败："+error.getMessage());
                        if (sender instanceof Player player) plugin.onPlayer(player,reply);
                        else if (plugin.isEnabled()) Bukkit.getGlobalRegionScheduler().run(plugin,t -> reply.run());
                    });
                    return true;
                }
                sender.sendMessage("/iw admin 打开管理中心 | /iw admin pin ID 位置（0取消） | /iw admin payments 核对未完成交易");
                return true;
            }
            if (!(sender instanceof Player p)) throw new IllegalArgumentException("这个指令需要玩家执行");
            switch (sub) {
                case "open", "me", "favorites", "admin" -> plugin.menu.open(p,sub,0,"",null);
                case "search" -> { require(args,2,"/iw search 关键词"); plugin.menu.open(p,"open",0,String.join(" ",Arrays.copyOfRange(args,1,args.length)),null); }
                case "filters" -> {
                    require(args,3,"/iw filters 最高票价或- 最低评分（0–5）");
                    try { plugin.menu.filter(p,args[1].equals("-")?Double.MAX_VALUE:Double.parseDouble(args[1]),Double.parseDouble(args[2])); }
                    catch(NumberFormatException e) { throw new IllegalArgumentException("请输入有效票价和评分"); }
                }
                case "event" -> {
                    require(args,3,"/iw event ID 开始时间 结束时间 或 /iw event ID off");
                    Warp w=find(p,args[1]);
                    plugin.requireManage(p,w);
                    long start=0,end=0;
                    if(!args[2].equalsIgnoreCase("off")) {
                        require(args,4,"/iw event ID yyyy-MM-ddTHH:mm yyyy-MM-ddTHH:mm（服务器时区）");
                        start=WarpText.eventTime(args[2]); end=WarpText.eventTime(args[3]);
                    }
                    plugin.menu.updated(p,w.id(),plugin.store.event(w.id(),p.getUniqueId(),p.hasPermission("islandwarp.admin"),start,end));
                }
                case "create" -> {
                    if (!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("没有创建权限");
                    require(args,2,"/iw create 名称 [分类]");
                    String name = name(args[1]); String category = args.length>2?args[2]:"other";
                    category(category);
                    Location l = p.getLocation(); String island = plugin.guard.creation(p,l);
                    Location centered = new Location(l.getWorld(),l.getBlockX()+.5,l.getBlockY(),l.getBlockZ()+.5);
                    if (!WarpTeleport.safe(centered)) throw new IllegalArgumentException("请站在安全的实心地面上创建");
                    Material icon=Material.matchMaterial(plugin.getConfig().getString("categories."+category+".icon","COMPASS"));
                    if(icon==null||!icon.isItem()||icon.isAir()) icon=Material.COMPASS;
                    Warp w = new Warp(0,p.getUniqueId(),p.getName(),name,"",category,l.getWorld().getUID(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),icon.name(),true,island,System.currentTimeMillis(),0);
                    double fee=plugin.economy.cost("creation-cost",100);
                    plugin.menu.payment(p,"发布地标 · "+name,fee,() -> {
                        if(!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("创建权限已撤销");
                        plugin.guard.creation(p,l);
                        int limit=p.hasPermission("islandwarp.admin")?Integer.MAX_VALUE:Math.max(0,plugin.getConfig().getInt("limits.per-player",5));
                         int islandLimit=Math.max(0,plugin.getConfig().getInt("limits.per-island",10));
                         plugin.economy.purchase(p,fee,() -> plugin.store.create(w,limit,islandLimit),"&a地标创建成功："+name,() -> plugin.menu.open(p,"me",0,"",null));
                    });
                }
                case "tp" -> { require(args,2,"/iw tp 名称或ID"); plugin.menu.travel(p,find(p,args[1])); }
                case "detail" -> { require(args,2,"/iw detail ID"); plugin.menu.detail(p,find(p,args[1]).id()); }
                case "check" -> {
                    require(args,2,"/iw check ID"); Warp w=find(p,args[1]);
                    if(!w.owner().equals(p.getUniqueId())&&!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("只能复查自己的坐标");
                    plugin.health.check(w).whenComplete((message,error) -> plugin.result(p,error==null?java.util.concurrent.CompletableFuture.completedFuture(null):java.util.concurrent.CompletableFuture.failedFuture(error),message==null?"":message));
                }
                case "share" -> { require(args,2,"/iw share ID"); plugin.menu.share(p,find(p,args[1]).id()); }
                case "income" -> plugin.menu.incomePage(p,0);
                case "notices" -> plugin.menu.noticesPage(p,0);
                case "trash" -> plugin.menu.trashPage(p,0);
                case "restore" -> {
                    require(args,2,"/iw restore ID");
                    if(!p.hasPermission("islandwarp.create")&&!p.hasPermission("islandwarp.admin")) throw new IllegalArgumentException("没有恢复权限");
                    long id; try { id=Long.parseLong(args[1]); } catch(NumberFormatException e) { throw new IllegalArgumentException("ID 必须是数字"); }
                    plugin.result(p,plugin.store.restore(id,p.getUniqueId(),p.hasPermission("islandwarp.admin"),Math.max(0,plugin.getConfig().getInt("limits.per-player",5)),Math.max(0,plugin.getConfig().getInt("limits.per-island",10))),"&a坐标及关联记录已恢复，原置顶和旧通知不恢复");
                }
                  case "favorite" -> {
                      require(args,2,"/iw favorite ID");
                      Warp w=plugin.store.all().values().stream().filter(row -> plugin.store.favorites(p.getUniqueId()).contains(row.id()))
                              .filter(row -> Long.toString(row.id()).equals(args[1])||WarpText.plain(row.name()).equalsIgnoreCase(WarpText.plain(args[1]))).findFirst().orElse(null);
                      plugin.result(p,plugin.store.favorite(p.getUniqueId(),w==null?find(p,args[1]).id():w.id()),"&a收藏状态已切换");
                  }
                case "comments" -> {
                    require(args,2,"/iw comments ID [留言ID]");
                    long warp=find(p,args[1]).id();
                    if(args.length>2) { try { plugin.menu.focusComment(p,warp,Long.parseLong(args[2])); } catch(NumberFormatException e) { throw new IllegalArgumentException("留言 ID 必须是数字"); } }
                    else plugin.menu.commentsPage(p,warp,0);
                }
                case "block","unblock" -> {
                    require(args,3,"/iw block 或 unblock 坐标ID 玩家名或UUID");
                    Warp w=find(p,args[1]);
                    if(!w.owner().equals(p.getUniqueId())||!p.hasPermission("islandwarp.create")) throw new IllegalArgumentException("只能管理自己坐标的黑名单");
                    UUID target; String name;
                    try { target=UUID.fromString(args[2]); name=args[2]; }
                    catch(IllegalArgumentException e) {
                        var online=Bukkit.getPlayerExact(args[2]);
                        var cached=online!=null?online:Bukkit.getOfflinePlayerIfCached(args[2]);
                        if(cached==null) throw new IllegalArgumentException("找不到此玩家，请使用其 UUID");
                        target=cached.getUniqueId(); name=cached.getName()==null?args[2]:cached.getName();
                    }
                    plugin.result(p,plugin.store.block(w.id(),p.getUniqueId(),target,name,sub.equals("block")),"&a坐标黑名单已更新");
                }
                case "renew" -> { require(args,2,"/iw renew ID"); plugin.menu.renew(p,find(p,args[1]).id()); }
                case "notifications" -> { boolean muted=!plugin.store.muted(p.getUniqueId()); plugin.result(p,plugin.store.mute(p.getUniqueId(),muted),muted?"&7通知已屏蔽，未读保留":"&a通知已开启，下次登录推送积压通知"); }
                case "delete" -> { require(args,2,"/iw delete ID"); plugin.menu.confirm(p,find(p,args[1])); }
                case "edit" -> edit(p,args);
                case "ticket" -> {
                    require(args,4,"/iw ticket ID enabled true|false 或 /iw ticket ID price 金额");
                    Warp w=find(p,args[1]);
                    plugin.requireManage(p,w);
                    Boolean enabled=null; Double price=null;
                    if(args[2].equals("enabled")) {
                        if(!Set.of("true","false").contains(args[3])) throw new IllegalArgumentException("开关填写 true 或 false");
                        enabled=Boolean.parseBoolean(args[3]);
                    } else if(args[2].equals("price")) {
                        try { price=Double.parseDouble(args[3]); } catch(NumberFormatException e) { throw new IllegalArgumentException("请输入有效票价"); }
                        double max=plugin.economy.limit("max-ticket",10000);
                        if(!Double.isFinite(price)||price<0||price>max) throw new IllegalArgumentException("票价超出允许范围");
                        price=Math.round(price*100)/100.0;
                    } else throw new IllegalArgumentException("使用 enabled 或 price");
                    plugin.menu.updated(p,w.id(),plugin.store.ticket(w.id(),p.getUniqueId(),p.hasPermission("islandwarp.admin"),enabled,price));
                }
                default -> throw new IllegalArgumentException("未知指令，请查看 /iw help");
            }
        } catch (IllegalArgumentException e) { sender.sendMessage(IslandWarpPlugin.color("&c"+e.getMessage())); }
        return true;
    }
    private Warp find(Player p,String text) {
        String plain=WarpText.plain(text);
        return plugin.store.all().values().stream().filter(w -> plugin.canSee(p,w)).filter(w -> Long.toString(w.id()).equals(plain)).findFirst()
                .orElseGet(() -> plugin.store.all().values().stream().filter(w -> plugin.canSee(p,w)&&WarpText.plain(w.name()).equalsIgnoreCase(plain)).findFirst().orElseThrow(() -> new IllegalArgumentException("找不到该地标")));
    }
    private static void require(String[] a,int length,String usage) { if (a.length<length) throw new IllegalArgumentException(usage); }
    private String name(String text) {
        return WarpText.name(text,plugin.getConfig().getInt("limits.name-length",24));
    }
    private void category(String text) { if (!plugin.categories().contains(text)) throw new IllegalArgumentException("分类可选："+String.join(", ",plugin.categories())); }
    private void edit(Player p,String[] args) {
        require(args,3,"/iw edit ID name|description|category|icon|visible|location 内容");
        Warp old = find(p,args[1]);
        plugin.requireManage(p,old);
        String field=args[2], value=String.join(" ",Arrays.copyOfRange(args,3,args.length));
        if (!field.equals("location") && value.isEmpty()) throw new IllegalArgumentException("请填写修改内容");
        java.util.function.Function<Warp,Warp> update;
        switch (field) {
            case "name" -> { String n=name(value); update=w -> w.edit(n,w.description(),w.category(),w.icon(),w.visible()); }
            case "description" -> {
                if (WarpText.plain(value).length()>Math.max(0,plugin.getConfig().getInt("limits.description-length",100))||value.length()>200) throw new IllegalArgumentException("描述过长");
                update=w -> w.edit(w.name(),value.equals("-")?"":WarpText.normalize(value),w.category(),w.icon(),w.visible());
            }
            case "category" -> { category(value); update=w -> w.edit(w.name(),w.description(),value,w.icon(),w.visible()); }
            case "icon" -> {
                Material m=Material.matchMaterial(value); if(m==null || !m.isItem() || m.isAir()) throw new IllegalArgumentException("无效物品 ID");
                update=w -> w.edit(w.name(),w.description(),w.category(),m.name(),w.visible());
            }
            case "visible" -> {
                if(!value.equals("true")&&!value.equals("false")) throw new IllegalArgumentException("填写 true 或 false");
                update=w -> w.edit(w.name(),w.description(),w.category(),w.icon(),Boolean.parseBoolean(value));
            }
            case "location" -> {
                Location l=p.getLocation().clone(); String island=plugin.guard.creation(p,l);
                if(!WarpTeleport.safe(l)) throw new IllegalArgumentException("当前位置不安全");
                update=w -> new Warp(w.id(),w.owner(),w.ownerName(),w.name(),w.description(),w.category(),l.getWorld().getUID(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),w.icon(),w.visible(),island,w.created(),w.visits());
            }
            default -> throw new IllegalArgumentException("不支持的编辑字段");
        }
        plugin.menu.updated(p,old.id(),plugin.store.edit(old.id(),p.getUniqueId(),p.hasPermission("islandwarp.admin"),update,Math.max(0,plugin.getConfig().getInt("limits.per-island",10))));
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args) {
        if(!s.hasPermission("islandwarp.use")) return List.of();
        List<String> values = new ArrayList<>();
        if(args.length==1) {
            values.addAll(List.of("open","me","favorites","search","filters","tp","detail","check","share","income","trash","restore","favorite","comments","renew","notices","notifications","help"));
            if(s.hasPermission("islandwarp.create")||s.hasPermission("islandwarp.admin")) values.addAll(List.of("create","edit","delete","ticket","event","block","unblock"));
            if(s.hasPermission("islandwarp.admin")) values.addAll(List.of("admin","reload","status"));
        } else if(args[0].equals("admin") && s.hasPermission("islandwarp.admin")) {
            if(args.length==2) values.addAll(List.of("pin","payments","export","freeze","unfreeze","help"));
            if(args.length==3 && Set.of("pin","freeze","unfreeze").contains(args[1])) plugin.store.all().keySet().forEach(id -> values.add(id.toString()));
            if(args.length==4 && args[1].equals("pin")) { values.add("0"); for(int i=1;i<=plugin.menu.featuredSlots();i++) values.add(Integer.toString(i)); }
            if(args.length==4 && args[1].equals("payments")) values.addAll(List.of("DONE","FAILED"));
            if(args.length==5 && args[1].equals("payments")) values.add("confirm");
        } else if(args.length==3 && args[0].equals("create")) values.addAll(plugin.categories());
        else if(args.length==3 && args[0].equals("edit")) values.addAll(List.of("name","description","category","icon","visible","location"));
        else if(args.length==3 && args[0].equals("event")) values.add("off");
        else if(args.length==2 && Set.of("tp","detail","check","share","favorite","comments","edit","delete","event").contains(args[0]) && s instanceof Player p)
            plugin.store.all().values().stream().filter(w -> plugin.canSee(p,w)).filter(w -> !Set.of("edit","delete").contains(args[0]) || p.hasPermission("islandwarp.admin") || w.owner().equals(p.getUniqueId())).forEach(w -> values.add(Long.toString(w.id())));
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
