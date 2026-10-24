package com.islandwarp;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** One connection, one worker. Publish cache snapshots only after a successful commit. */
public final class WarpStore implements AutoCloseable {
    private final Connection db;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "IslandWarp-SQLite"));
    private volatile Map<Long, Warp> warps = Map.of();
    private volatile Map<UUID, Set<Long>> favorites = Map.of();
    private volatile Map<Integer, Long> pins = Map.of();
    private volatile Map<Integer, Long> pinExpiry = Map.of();
    private volatile Map<Long, Rating> ratings = Map.of();
    private volatile Map<String, Integer> playerRatings = Map.of();
    private volatile Map<Long, List<Comment>> comments = Map.of();
    private volatile Map<Long, List<Reply>> replies = Map.of();
    private volatile Map<UUID,Map<Long,Integer>> unread = Map.of();
    private volatile boolean healthy = true;
    private volatile boolean cacheDirty;
    public record Ticket(boolean enabled, double price) {}
    public record Rating(double average, int count, int positive, int negative) {}
    public record Comment(long id, UUID player, String author, String text, long created, long edited) {}
    public record Trash(long id,String name,UUID owner,long expires) {}
    public record Income(long warp,String name,double gross,double tax,double net,boolean paid,long created) {}
    private static final List<String> RECYCLED=List.of("warps","tickets","favorites","ratings","comments","replies","visits","daily_visits","warp_blocks","warp_status","warp_events");
    public record Event(long start,long end) {
        public boolean active(long now) { return now>=start&&now<end; }
    }
    private volatile Map<Long,Event> events=Map.of();
    public Event event(long id) { return events.get(id); }
    public boolean eventActive(long id,long now) { Event event=event(id); return event==null||event.active(now); }
    public boolean publicAvailable(Warp w) { return w.visible()&&eventActive(w.id(),System.currentTimeMillis()); }
    public void requireEventActive(long id) { if(!eventActive(id,System.currentTimeMillis())) throw new IllegalArgumentException("活动尚未开始或已经结束，暂不可传送"); }
    public record Status(boolean frozen,String reason,String actor,String invalid,long checked) {}
    private volatile Map<Long,Status> statuses=Map.of();
    public Status status(long id) { return statuses.getOrDefault(id,new Status(false,"","","",0)); }
    public void requireCacheReady() { if(cacheDirty) throw new IllegalArgumentException("数据库缓存待恢复，暂时不能操作，请稍后重试"); }
    public void requireUnfrozen(long id) { requireCacheReady(); if(status(id).frozen()) throw new IllegalArgumentException("坐标已被管理员冻结："+status(id).reason()); }
    public record Reply(String author, String text, long created) {}
    public record Notice(long id, long warp, String name, String author, boolean reply, int kind,long comment) {}
    public record History(long id,long warp,String name,String author,int kind,long comment,long created,boolean read) {}
    private volatile int historyLimit=100;
    public void historyLimit(int limit) { historyLimit=Math.max(1,Math.min(1000,limit)); }
    public record Block(UUID player,String name) {}
    private volatile Map<Long,List<Block>> blocks=Map.of();
    public record Report(long id,long warp,long comment,String reporter,String author,String text,String reason,long created) {}
    public record Visit(String visitor,long created) {}
    public record Audit(String actor,String action,long warp,String name,String detail,long created) {}
    private volatile Set<UUID> muted=Set.of();
    private volatile Map<Long, Ticket> tickets = Map.of();
    public Ticket ticket(long id) { return tickets.getOrDefault(id,new Ticket(false,0)); }

    public WarpStore(Path path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        db = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement s = db.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL"); s.execute("PRAGMA synchronous=FULL");
            s.execute("PRAGMA foreign_keys=ON"); s.execute("PRAGMA busy_timeout=5000");
            s.execute("CREATE TABLE IF NOT EXISTS warps(id INTEGER PRIMARY KEY AUTOINCREMENT,owner TEXT NOT NULL,owner_name TEXT NOT NULL,name TEXT NOT NULL UNIQUE COLLATE NOCASE,description TEXT NOT NULL,category TEXT NOT NULL,world TEXT NOT NULL,x REAL NOT NULL,y REAL NOT NULL,z REAL NOT NULL,yaw REAL NOT NULL,pitch REAL NOT NULL,icon TEXT NOT NULL,visible INTEGER NOT NULL,island TEXT,created INTEGER NOT NULL,visits INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS favorites(player TEXT NOT NULL,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,PRIMARY KEY(player,warp))");
            s.execute("CREATE TABLE IF NOT EXISTS pins(slot INTEGER PRIMARY KEY CHECK(slot BETWEEN 1 AND 36),warp INTEGER NOT NULL UNIQUE REFERENCES warps(id) ON DELETE CASCADE,expires INTEGER NOT NULL DEFAULT 0)");
            String pinSchema;
            try (ResultSet r=s.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='pins'")) {
                pinSchema=r.next()?r.getString(1):"";
            }
            if(pinSchema.contains("BETWEEN 1 AND 5")) {
                s.execute("PRAGMA foreign_keys=OFF");
                s.execute("ALTER TABLE pins RENAME TO pins_old");
                s.execute("CREATE TABLE pins(slot INTEGER PRIMARY KEY CHECK(slot BETWEEN 1 AND 36),warp INTEGER NOT NULL UNIQUE REFERENCES warps(id) ON DELETE CASCADE,expires INTEGER NOT NULL DEFAULT 0)");
                s.execute("INSERT INTO pins(slot,warp,expires) SELECT slot,warp,0 FROM pins_old");
                s.execute("DROP TABLE pins_old");
                s.execute("PRAGMA foreign_keys=ON");
            }
            if(!pinSchema.contains("expires") && !pinSchema.contains("BETWEEN 1 AND 5")) s.execute("ALTER TABLE pins ADD COLUMN expires INTEGER NOT NULL DEFAULT 0");
            s.execute("CREATE TABLE IF NOT EXISTS tickets(warp INTEGER PRIMARY KEY REFERENCES warps(id) ON DELETE CASCADE,enabled INTEGER NOT NULL,price REAL NOT NULL CHECK(price>=0))");
            s.execute("CREATE TABLE IF NOT EXISTS ratings(player TEXT NOT NULL,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),PRIMARY KEY(player,warp))");
            s.execute("CREATE TABLE IF NOT EXISTS comments(id INTEGER PRIMARY KEY AUTOINCREMENT,player TEXT NOT NULL,author TEXT NOT NULL,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,text TEXT NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS replies(id INTEGER PRIMARY KEY AUTOINCREMENT,comment INTEGER NOT NULL REFERENCES comments(id) ON DELETE CASCADE,author TEXT NOT NULL,text TEXT NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS notices(id INTEGER PRIMARY KEY AUTOINCREMENT,player TEXT NOT NULL,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,name TEXT NOT NULL,author TEXT NOT NULL,reply INTEGER NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS notices_player ON notices(player,id DESC)");
            s.execute("CREATE TABLE IF NOT EXISTS unread(player TEXT NOT NULL,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,count INTEGER NOT NULL,PRIMARY KEY(player,warp))");
            s.execute("CREATE TABLE IF NOT EXISTS comment_cooldowns(player TEXT PRIMARY KEY,last INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS notification_settings(player TEXT PRIMARY KEY,muted INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS reports(id INTEGER PRIMARY KEY AUTOINCREMENT,warp INTEGER NOT NULL,comment INTEGER NOT NULL,reporter TEXT NOT NULL,author TEXT NOT NULL,text TEXT NOT NULL,reason TEXT NOT NULL,created INTEGER NOT NULL,resolved INTEGER NOT NULL DEFAULT 0,UNIQUE(comment,reporter))");
            s.execute("CREATE TABLE IF NOT EXISTS visits(id INTEGER PRIMARY KEY AUTOINCREMENT,warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,visitor TEXT NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS daily_visits(warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,day TEXT NOT NULL,count INTEGER NOT NULL,PRIMARY KEY(warp,day))");
            s.execute("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY AUTOINCREMENT,actor TEXT NOT NULL,action TEXT NOT NULL,warp INTEGER NOT NULL,name TEXT NOT NULL,detail TEXT NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS pin_reminders(slot INTEGER PRIMARY KEY,expires INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS warp_blocks(warp INTEGER NOT NULL REFERENCES warps(id) ON DELETE CASCADE,player TEXT NOT NULL,name TEXT NOT NULL,PRIMARY KEY(warp,player))");
            boolean noticeComment=false;
            try(ResultSet r=s.executeQuery("PRAGMA table_info(notices)")) { while(r.next()) if(r.getString("name").equals("comment")) noticeComment=true; }
            if(!noticeComment) s.execute("ALTER TABLE notices ADD COLUMN comment INTEGER NOT NULL DEFAULT 0");
            boolean historyExists;
            try(ResultSet r=s.executeQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='notice_history'")) { historyExists=r.next(); }
            s.execute("CREATE TABLE IF NOT EXISTS notice_history(id INTEGER PRIMARY KEY,player TEXT NOT NULL,warp INTEGER NOT NULL,name TEXT NOT NULL,author TEXT NOT NULL,kind INTEGER NOT NULL,comment INTEGER NOT NULL,created INTEGER NOT NULL,read INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE INDEX IF NOT EXISTS notice_history_player ON notice_history(player,id DESC)");
            if(!historyExists) s.execute("INSERT OR IGNORE INTO notice_history(id,player,warp,name,author,kind,comment,created) SELECT id,player,warp,name,author,reply,comment,0 FROM notices");
            boolean edited=false;
            try(ResultSet r=s.executeQuery("PRAGMA table_info(comments)")) { while(r.next()) if(r.getString("name").equals("edited")) edited=true; }
            if(!edited) s.execute("ALTER TABLE comments ADD COLUMN edited INTEGER NOT NULL DEFAULT 0");
            s.execute("CREATE TABLE IF NOT EXISTS recycle(id INTEGER PRIMARY KEY,owner TEXT NOT NULL,name TEXT NOT NULL,expires INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS warp_status(warp INTEGER PRIMARY KEY REFERENCES warps(id) ON DELETE CASCADE,frozen INTEGER NOT NULL DEFAULT 0,reason TEXT NOT NULL DEFAULT '',actor TEXT NOT NULL DEFAULT '',invalid TEXT NOT NULL DEFAULT '',checked INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS warp_events(warp INTEGER PRIMARY KEY REFERENCES warps(id) ON DELETE CASCADE,start INTEGER NOT NULL,end INTEGER NOT NULL CHECK(end>start))");
            for(String table:RECYCLED) {
                s.execute("CREATE TABLE IF NOT EXISTS recycle_"+table+" AS SELECT *,0 AS trash_warp FROM "+table+" WHERE 0");
                Set<String> mirrored=new HashSet<>();
                try(ResultSet r=s.executeQuery("PRAGMA table_info(recycle_"+table+")")) { while(r.next()) mirrored.add(r.getString("name")); }
                List<String> additions=new ArrayList<>();
                try(ResultSet r=s.executeQuery("PRAGMA table_info("+table+")")) { while(r.next()) {
                    String column=r.getString("name"),value=r.getString("dflt_value");
                    if(!mirrored.contains(column)) additions.add("ALTER TABLE recycle_"+table+" ADD COLUMN \""+column+"\" "+r.getString("type")+(value==null?"":" DEFAULT "+value));
                } }
                for(String sql:additions) s.execute(sql);
            }
            s.execute("CREATE TABLE IF NOT EXISTS income(id INTEGER PRIMARY KEY AUTOINCREMENT,owner TEXT NOT NULL,warp INTEGER NOT NULL,name TEXT NOT NULL,gross REAL NOT NULL,tax REAL NOT NULL,net REAL NOT NULL,paid INTEGER NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS income_owner_id ON income(owner,id DESC)");
            s.execute("CREATE TABLE IF NOT EXISTS payments(id TEXT PRIMARY KEY,player TEXT NOT NULL,amount REAL NOT NULL,direction TEXT NOT NULL,detail TEXT NOT NULL,state TEXT NOT NULL,created INTEGER NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS payments_player ON payments(player,state)");
        }
        expireData(); refresh();
    }
    public Map<Long, Warp> all() { return warps; }
    public Set<Long> favorites(UUID player) { return favorites.getOrDefault(player, Set.of()); }
    public boolean healthy() { return healthy; }
    public Map<Integer, Long> pins() { return pins; }
    public long pinExpiry(int slot) { return pinExpiry.getOrDefault(slot,0L); }
    public Rating rating(long id) { return ratings.getOrDefault(id,new Rating(0,0,0,0)); }
    public int rating(UUID player,long id) { return playerRatings.getOrDefault(player+":"+id,0); }
    public List<Comment> comments(long id) { return comments.getOrDefault(id,List.of()); }
    public List<Reply> replies(long comment) { return replies.getOrDefault(comment,List.of()); }
    public int unread(UUID player,long warp) { return unread.getOrDefault(player,Map.of()).getOrDefault(warp,0); }
    public int unread(UUID player) { return unread.getOrDefault(player,Map.of()).values().stream().mapToInt(Integer::intValue).sum(); }
    public boolean muted(UUID player) { return muted.contains(player); }
    public List<Block> blocks(long warp) { return blocks.getOrDefault(warp,List.of()); }
    public boolean blocked(long warp,UUID player) { return blocks(warp).stream().anyMatch(b -> b.player().equals(player)); }
    public long owned(UUID player) { return warps.values().stream().filter(w -> w.owner().equals(player)).count(); }
    public long ownedIsland(String island) { return island == null ? 0 : warps.values().stream().filter(w -> island.equals(w.island())).count(); }

    private void refresh() throws SQLException {
        Map<Long,Event> eventData=new HashMap<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT warp,start,end FROM warp_events")) { while(r.next()) eventData.put(r.getLong(1),new Event(r.getLong(2),r.getLong(3))); }
        Map<Long, Warp> loaded = new HashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM warps")) {
            while (r.next()) {
                String island = r.getString("island");
                Warp w = new Warp(r.getLong("id"), UUID.fromString(r.getString("owner")), r.getString("owner_name"),
                        r.getString("name"), r.getString("description"), r.getString("category"), UUID.fromString(r.getString("world")),
                        r.getDouble("x"), r.getDouble("y"), r.getDouble("z"), r.getFloat("yaw"), r.getFloat("pitch"),
                        r.getString("icon"), r.getBoolean("visible"), island,
                        r.getLong("created"), r.getLong("visits"));
                loaded.put(w.id(), w);
            }
        }
        Map<UUID, Set<Long>> fav = new HashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM favorites")) {
            while (r.next()) fav.computeIfAbsent(UUID.fromString(r.getString(1)), k -> new HashSet<>()).add(r.getLong(2));
        }
        fav.replaceAll((k,v) -> Set.copyOf(v));
        Map<Integer, Long> featured = new HashMap<>();
        Map<Integer,Long> expiry = new HashMap<>();
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT slot,warp,expires FROM pins")) {
            while (r.next()) { featured.put(r.getInt(1),r.getLong(2)); expiry.put(r.getInt(1),r.getLong(3)); }
        }
        Map<Long, Ticket> fees = new HashMap<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM tickets")) {
            while(r.next()) fees.put(r.getLong(1),new Ticket(r.getBoolean(2),r.getDouble(3)));
        }
        Map<Long, int[]> ratingData = new HashMap<>();
        Map<String,Integer> playerRatingData = new HashMap<>();
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT player,warp,rating FROM ratings")) {
            while(r.next()) playerRatingData.put(r.getString(1)+":"+r.getLong(2),r.getInt(3));
        }
        try (Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT warp,AVG(rating),COUNT(*),SUM(CASE WHEN rating>=4 THEN 1 ELSE 0 END),SUM(CASE WHEN rating<=2 THEN 1 ELSE 0 END) FROM ratings GROUP BY warp")) {
            while(r.next()) ratingData.put(r.getLong(1),new int[]{(int)Math.round(r.getDouble(2)*100),r.getInt(3),r.getInt(4),r.getInt(5)});
        }
        Map<Long, Rating> ratingSnapshot=new HashMap<>();
        ratingData.forEach((id,v) -> ratingSnapshot.put(id,new Rating(v[0]/100.0,v[1],v[2],v[3])));
        Map<Long,List<Comment>> commentData=new HashMap<>();
        try(PreparedStatement s=db.prepareStatement("SELECT author,text,created,warp,id,player,edited FROM comments ORDER BY created DESC,id DESC")) {
            try(ResultSet r=s.executeQuery()) { while(r.next()) commentData.computeIfAbsent(r.getLong(4),k -> new ArrayList<>()).add(new Comment(r.getLong(5),UUID.fromString(r.getString(6)),r.getString(1),r.getString(2),r.getLong(3),r.getLong(7))); }
        }
        commentData.replaceAll((k,v) -> List.copyOf(v));
        Map<Long,List<Reply>> replyData=new HashMap<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT comment,author,text,created FROM replies ORDER BY id")) {
            while(r.next()) replyData.computeIfAbsent(r.getLong(1),k -> new ArrayList<>()).add(new Reply(r.getString(2),r.getString(3),r.getLong(4)));
        }
        replyData.replaceAll((k,v) -> List.copyOf(v));
        Map<UUID,Map<Long,Integer>> unreadData=new HashMap<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT player,warp,count FROM unread")) {
            while(r.next()) unreadData.computeIfAbsent(UUID.fromString(r.getString(1)),k -> new HashMap<>()).put(r.getLong(2),r.getInt(3));
        }
        unreadData.replaceAll((k,v) -> Map.copyOf(v));
        Set<UUID> mutedData=new HashSet<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT player FROM notification_settings WHERE muted=1")) { while(r.next()) mutedData.add(UUID.fromString(r.getString(1))); }
        Map<Long,List<Block>> blockData=new HashMap<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT warp,player,name FROM warp_blocks ORDER BY name")) { while(r.next()) blockData.computeIfAbsent(r.getLong(1),k -> new ArrayList<>()).add(new Block(UUID.fromString(r.getString(2)),r.getString(3))); }
        blockData.replaceAll((k,v) -> List.copyOf(v));
        Map<Long,Status> statusData=new HashMap<>();
        try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT warp,frozen,reason,actor,invalid,checked FROM warp_status")) { while(r.next()) statusData.put(r.getLong(1),new Status(r.getBoolean(2),r.getString(3),r.getString(4),r.getString(5),r.getLong(6))); }
        statuses=Map.copyOf(statusData);
        events=Map.copyOf(eventData); pins=Map.copyOf(featured); pinExpiry=Map.copyOf(expiry);
        tickets=Map.copyOf(fees); ratings=Map.copyOf(ratingSnapshot); playerRatings=Map.copyOf(playerRatingData);
        comments=Map.copyOf(commentData); replies=Map.copyOf(replyData); unread=Map.copyOf(unreadData);
        muted=Set.copyOf(mutedData); blocks=Map.copyOf(blockData);
        warps = Map.copyOf(loaded); favorites = Map.copyOf(fav);
    }
    @FunctionalInterface private interface SqlTask<T> { T run() throws Exception; }
    private <T> CompletableFuture<T> submit(SqlTask<T> task) {
        return submit(task,true);
    }
    private <T> CompletableFuture<T> submit(SqlTask<T> task,boolean refreshCache) {
        return submit(task,refreshCache,null,true);
    }
    private <T> CompletableFuture<T> read(SqlTask<T> task) { return submit(task,false,null,false); }
    private <T> CompletableFuture<T> write(SqlTask<T> task,Runnable publish) { return submit(task,false,publish,true); }
    private <T> CompletableFuture<T> submit(SqlTask<T> task,boolean refreshCache,Runnable publish,boolean transaction) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            worker.execute(() -> {
                T value;
                try {
                    if(cacheDirty) { db.setAutoCommit(true); refresh(); cacheDirty=false; }
                    db.setAutoCommit(!transaction);
                    value = task.run(); if(transaction) db.commit();
                } catch (Exception e) {
                    if (!(e instanceof IllegalArgumentException) && !(e instanceof SQLException sql && sql.getErrorCode() == 19)) healthy = false;
                    try { if(!db.getAutoCommit()) db.rollback(); db.setAutoCommit(true); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                    result.completeExceptionally(e);
                    return;
                }
                // A committed purchase must never become a refundable failure because cache recovery failed.
                try {
                    db.setAutoCommit(true);
                    if(refreshCache) refresh();
                    if(publish!=null) publish.run();
                    healthy=true;
                } catch(Exception e) {
                    healthy=false; cacheDirty=true;
                    java.util.logging.Logger.getLogger(WarpStore.class.getName()).log(java.util.logging.Level.SEVERE,"事务已提交，禁止重复付款/退款；缓存将在下一项任务前重试恢复",e);
                }
                result.complete(value);
            });
        } catch (RejectedExecutionException e) { result.completeExceptionally(e); }
        return result;
    }
    public CompletableFuture<Long> create(Warp w, int playerLimit, int islandLimit) {
        return submit(() -> {
            uniqueName(w.name(),0);
            if (owned(w.owner()) >= playerLimit) throw new IllegalArgumentException("已达到玩家地标数量上限");
            if (islandLimit > 0 && w.island() != null && ownedIsland(w.island()) >= islandLimit) throw new IllegalArgumentException("已达到岛屿传送点上限");
            try (PreparedStatement s = db.prepareStatement("INSERT INTO warps(owner,owner_name,name,description,category,world,x,y,z,yaw,pitch,icon,visible,island,created) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                s.setString(1,w.owner().toString()); s.setString(2,w.ownerName()); s.setString(3,w.name());
                s.setString(4,w.description()); s.setString(5,w.category()); s.setString(6,w.world().toString());
                s.setDouble(7,w.x()); s.setDouble(8,w.y()); s.setDouble(9,w.z()); s.setFloat(10,w.yaw()); s.setFloat(11,w.pitch());
                s.setString(12,w.icon()); s.setBoolean(13,w.visible()); s.setString(14,w.island());
                s.setLong(15,w.created()); s.executeUpdate();
            }
            try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT last_insert_rowid()")) { r.next(); return r.getLong(1); }
        });
    }
    private Warp authorized(long id, UUID actor, boolean admin) {
        Warp w = warps.get(id);
        if (w == null) throw new IllegalArgumentException("地标已被删除");
        if (!admin && !w.owner().equals(actor)) throw new IllegalArgumentException("你不能管理这个地标");
        return w;
    }
    public CompletableFuture<Void> edit(long id, UUID actor, boolean admin, Function<Warp, Warp> change) {
        return edit(id,actor,admin,change,0);
    }
    public CompletableFuture<Void> edit(long id, UUID actor, boolean admin, Function<Warp, Warp> change,int islandLimit) {
        return submit(() -> {
            Warp w = change.apply(authorized(id,actor,admin));
            if(w.visible()&&event(id)!=null&&event(id).end()<=System.currentTimeMillis()) throw new IllegalArgumentException("活动已结束，请先修改或取消活动时间再公开");
            if(!Objects.equals(warps.get(id).island(),w.island())&&w.island()!=null&&islandLimit>0&&ownedIsland(w.island())>=islandLimit) throw new IllegalArgumentException("目标岛屿的坐标数量已达上限，无法迁移");
            if(admin) audit(actor.toString(),"编辑",w,"原名称="+warps.get(id).name()+"；新名称="+w.name()+"；坐标="+w.world()+" "+w.x()+","+w.y()+","+w.z()+"；分类="+w.category()+"；公开="+w.visible()+"；描述="+w.description()+"；图标="+w.icon());
            uniqueName(w.name(),id);
            try (PreparedStatement s = db.prepareStatement("UPDATE warps SET name=?,description=?,category=?,icon=?,visible=?,world=?,x=?,y=?,z=?,yaw=?,pitch=?,island=? WHERE id=?")) {
                s.setString(1,w.name()); s.setString(2,w.description()); s.setString(3,w.category()); s.setString(4,w.icon()); s.setBoolean(5,w.visible());
                s.setString(6,w.world().toString()); s.setDouble(7,w.x()); s.setDouble(8,w.y()); s.setDouble(9,w.z());
                s.setFloat(10,w.yaw()); s.setFloat(11,w.pitch()); s.setString(12,w.island()); s.setLong(13,id); s.executeUpdate();
            }
            Warp old=warps.get(id);
            if(!old.world().equals(w.world())||old.x()!=w.x()||old.y()!=w.y()||old.z()!=w.z()) {
                try(PreparedStatement s=db.prepareStatement("UPDATE warp_status SET invalid='',checked=0 WHERE warp=?")) { s.setLong(1,id); s.executeUpdate(); }
                try(PreparedStatement s=db.prepareStatement("DELETE FROM notices WHERE warp=? AND reply=3")) { s.setLong(1,id); s.executeUpdate(); }
            }
            return null;
        });
    }
    public CompletableFuture<Void> delete(long id, UUID actor, boolean admin) {
        return delete(id,actor,admin,0);
    }
    public CompletableFuture<Void> delete(long id, UUID actor, boolean admin,int retentionDays) {
        return submit(() -> {
            Warp w=authorized(id,actor,admin);
            purgeTrash();
            if(retentionDays>0) {
                long expires=System.currentTimeMillis()+Math.min(36500L,retentionDays)*86400000L;
                try(PreparedStatement s=db.prepareStatement("INSERT INTO recycle VALUES(?,?,?,?)")) { s.setLong(1,id); s.setString(2,w.owner().toString()); s.setString(3,w.name()); s.setLong(4,expires); s.executeUpdate(); }
                for(String table:RECYCLED) {
                    String condition=table.equals("warps")?"id=?":table.equals("replies")?"comment IN (SELECT id FROM comments WHERE warp=?)":"warp=?";
                    String names=columns(table);
                    try(PreparedStatement s=db.prepareStatement("INSERT INTO recycle_"+table+" ("+names+",trash_warp) SELECT "+names+",? FROM "+table+" WHERE "+condition)) { s.setLong(1,id); s.setLong(2,id); s.executeUpdate(); }
                }
            }
            if(admin) audit(actor.toString(),"删除",w,"删除坐标及关联数据");
            try (PreparedStatement s = db.prepareStatement("DELETE FROM warps WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<Boolean> favorite(UUID player, long id) {
        return submit(() -> {
            Warp w = warps.get(id);
            boolean add = !favorites(player).contains(id);
            if (add && (w == null || (!publicAvailable(w) && !w.owner().equals(player)))) throw new IllegalArgumentException("地标不可收藏");
            try (PreparedStatement s = db.prepareStatement(add ? "INSERT INTO favorites VALUES(?,?)" : "DELETE FROM favorites WHERE player=? AND warp=?")) {
                s.setString(1,player.toString()); s.setLong(2,id); s.executeUpdate();
            }
            return add;
        });
    }
    private void purgeTrash() throws SQLException {
        for(String table:RECYCLED) try(PreparedStatement s=db.prepareStatement("DELETE FROM recycle_"+table+" WHERE trash_warp IN (SELECT id FROM recycle WHERE expires<=?)")) { s.setLong(1,System.currentTimeMillis()); s.executeUpdate(); }
        try(PreparedStatement s=db.prepareStatement("DELETE FROM recycle WHERE expires<=?")) { s.setLong(1,System.currentTimeMillis()); s.executeUpdate(); }
    }
    private String columns(String table)throws SQLException {
        List<String> names=new ArrayList<>();
        try(Statement s=db.createStatement();ResultSet r=s.executeQuery("PRAGMA table_info("+table+")")) { while(r.next()) names.add("\""+r.getString("name")+"\""); }
        return String.join(",",names);
    }
    public CompletableFuture<List<Trash>> trash(UUID actor,boolean admin) {
        return submit(() -> {
            purgeTrash(); List<Trash> result=new ArrayList<>();
            try(PreparedStatement s=db.prepareStatement("SELECT id,name,owner,expires FROM recycle"+(admin?"":" WHERE owner=?")+" ORDER BY expires DESC")) {
                if(!admin) s.setString(1,actor.toString());
                try(ResultSet r=s.executeQuery()) { while(r.next()) result.add(new Trash(r.getLong(1),r.getString(2),UUID.fromString(r.getString(3)),r.getLong(4))); }
            }
            return List.copyOf(result);
        },false);
    }
    public CompletableFuture<Void> restore(long id,UUID actor,boolean admin,int playerLimit,int islandLimit) {
        return submit(() -> {
            try(PreparedStatement s=db.prepareStatement("SELECT owner,name,expires FROM recycle WHERE id=?")) {
                s.setLong(1,id);
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()||r.getLong(3)<=System.currentTimeMillis()) throw new IllegalArgumentException("回收记录不存在或已过期");
                    UUID owner=UUID.fromString(r.getString(1));
                    if(!admin&&!owner.equals(actor)) throw new IllegalArgumentException("只能恢复自己的坐标");
                    uniqueName(r.getString(2),0);
                    if(owned(owner)>=playerLimit) throw new IllegalArgumentException("主人坐标数量已达上限");
                }
            }
            try(PreparedStatement s=db.prepareStatement("SELECT island FROM recycle_warps WHERE trash_warp=?")) {
                s.setLong(1,id); try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("回收数据缺失"); String island=r.getString(1); if(island!=null&&islandLimit>0&&ownedIsland(island)>=islandLimit) throw new IllegalArgumentException("目标岛屿坐标数量已达上限"); }
            }
            for(String table:RECYCLED) {
                List<String> columns=new ArrayList<>();
                try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("PRAGMA table_info("+table+")")) { while(r.next()) columns.add("\""+r.getString("name")+"\""); }
                String names=String.join(",",columns);
                try(PreparedStatement s=db.prepareStatement("INSERT INTO "+table+" ("+names+") SELECT "+names+" FROM recycle_"+table+" WHERE trash_warp=?")) { s.setLong(1,id); s.executeUpdate(); }
                try(PreparedStatement s=db.prepareStatement("DELETE FROM recycle_"+table+" WHERE trash_warp=?")) { s.setLong(1,id); s.executeUpdate(); }
            }
            try(PreparedStatement s=db.prepareStatement("DELETE FROM recycle WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
            if(admin) try(PreparedStatement s=db.prepareStatement("INSERT INTO audit(actor,action,warp,name,detail,created) SELECT ?, '恢复',id,name,'从回收站恢复',? FROM warps WHERE id=?")) { s.setString(1,actor.toString()); s.setLong(2,System.currentTimeMillis()); s.setLong(3,id); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<Void> editComment(UUID actor,long warp,long comment,String text,int maxLength,int seconds) {
        return submit(() -> {
            requireUnfrozen(warp);
            Warp w=warps.get(warp);
            if(w==null||!publicAvailable(w)&&!w.owner().equals(actor)||blocked(warp,actor)) throw new IllegalArgumentException("留言不可编辑");
            validateText(text,maxLength); cooldown(actor,seconds);
            try(PreparedStatement s=db.prepareStatement("UPDATE comments SET text=?,edited=? WHERE id=? AND warp=? AND player=?")) {
                s.setString(1,text); s.setLong(2,System.currentTimeMillis()); s.setLong(3,comment); s.setLong(4,warp); s.setString(5,actor.toString());
                if(s.executeUpdate()!=1) throw new IllegalArgumentException("只能编辑自己的现存留言");
            }
            return null;
        });
    }
    public CompletableFuture<Void> income(UUID owner,long warp,String name,double gross,double net,boolean paid) {
        return submit(() -> {
            if(!Double.isFinite(gross)||!Double.isFinite(net)||gross<=0||net<0||net>gross) throw new IllegalArgumentException("收入金额无效");
            try(PreparedStatement s=db.prepareStatement("INSERT INTO income(owner,warp,name,gross,tax,net,paid,created) VALUES(?,?,?,?,?,?,?,?)")) {
                s.setString(1,owner.toString()); s.setLong(2,warp); s.setString(3,name); s.setDouble(4,gross); s.setDouble(5,Math.round((gross-net)*100)/100.0); s.setDouble(6,net); s.setBoolean(7,paid); s.setLong(8,System.currentTimeMillis()); s.executeUpdate();
            }
            return null;
        },false);
    }
    public CompletableFuture<List<Income>> income(UUID owner) {
        return submit(() -> {
            List<Income> result=new ArrayList<>();
            try(PreparedStatement s=db.prepareStatement("SELECT warp,name,gross,tax,net,paid,created FROM income WHERE owner=? ORDER BY id DESC")) {
                s.setString(1,owner.toString()); try(ResultSet r=s.executeQuery()) { while(r.next()) result.add(new Income(r.getLong(1),r.getString(2),r.getDouble(3),r.getDouble(4),r.getDouble(5),r.getBoolean(6),r.getLong(7))); }
            }
            return List.copyOf(result);
        },false);
    }
    public record IncomePage(List<Income> rows,int page,int pages,double total,double daily) {}
    public CompletableFuture<IncomePage> incomePage(UUID owner,int requested,long today) {
        return submit(() -> {
            long count; double total,daily;
            try(PreparedStatement s=db.prepareStatement("SELECT COUNT(*),COALESCE(SUM(CASE WHEN paid=1 THEN net ELSE 0 END),0),COALESCE(SUM(CASE WHEN paid=1 AND created>=? THEN net ELSE 0 END),0) FROM income WHERE owner=?")) {
                s.setLong(1,today);s.setString(2,owner.toString());
                try(ResultSet r=s.executeQuery()) { r.next();count=r.getLong(1);total=r.getDouble(2);daily=r.getDouble(3); }
            }
            int pages=(int)Math.max(1,Math.min(Integer.MAX_VALUE,(count+27)/28)),page=Math.max(0,Math.min(requested,pages-1));
            List<Income> rows=new ArrayList<>();
            try(PreparedStatement s=db.prepareStatement("SELECT warp,name,gross,tax,net,paid,created FROM income WHERE owner=? ORDER BY id DESC LIMIT 28 OFFSET ?")) {
                s.setString(1,owner.toString());s.setLong(2,page*28L);
                try(ResultSet r=s.executeQuery()) { while(r.next()) rows.add(new Income(r.getLong(1),r.getString(2),r.getDouble(3),r.getDouble(4),r.getDouble(5),r.getBoolean(6),r.getLong(7))); }
            }
            return new IncomePage(List.copyOf(rows),page,pages,total,daily);
        },false);
    }
    public record Payment(String id,String player,double amount,String direction,String detail,String state) {}
    public CompletableFuture<String> payment(UUID player,double amount,String direction,String detail) {
        return submit(() -> {
            if(!Double.isFinite(amount)||amount<=0||!Set.of("DEBIT","CREDIT").contains(direction)) throw new IllegalArgumentException("交易金额或方向无效");
            if(direction.equals("DEBIT")) try(PreparedStatement s=db.prepareStatement("SELECT 1 FROM payments WHERE player=? AND state NOT IN ('DONE','FAILED')")) {
                s.setString(1,player.toString());try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("存在未完成交易，请联系管理员核对后再付款"); }
            }
            String id=UUID.randomUUID().toString();
            try(PreparedStatement s=db.prepareStatement("INSERT INTO payments VALUES(?,?,?,?,?,'PREPARED',?)")) {
                s.setString(1,id);s.setString(2,player.toString());s.setDouble(3,amount);s.setString(4,direction);s.setString(5,detail);s.setLong(6,System.currentTimeMillis());s.executeUpdate();
            }
            return id;
        },false);
    }
    public CompletableFuture<Void> paymentState(String id,String state) {
        if(id==null) return CompletableFuture.completedFuture(null);
        return submit(() -> {
            if(!Set.of("EXECUTING","PAID","DONE","FAILED").contains(state)) throw new IllegalArgumentException("交易状态无效");
            try(PreparedStatement s=db.prepareStatement("UPDATE payments SET state=? WHERE id=?")) { s.setString(1,state);s.setString(2,id);if(s.executeUpdate()!=1) throw new IllegalArgumentException("交易记录不存在"); }
            return null;
        },false);
    }
    public CompletableFuture<List<Payment>> unresolvedPayments() {
        return read(() -> {
            List<Payment> rows=new ArrayList<>();
            try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT id,player,amount,direction,detail,state FROM payments WHERE state NOT IN ('DONE','FAILED') ORDER BY created")) {
                while(r.next()) rows.add(new Payment(r.getString(1),r.getString(2),r.getDouble(3),r.getString(4),r.getString(5),r.getString(6)));
            }
            return List.copyOf(rows);
        });
    }
    public CompletableFuture<Void> reconcilePayment(String id,String state,String actor) {
        return submit(() -> {
            if(!Set.of("DONE","FAILED").contains(state)) throw new IllegalArgumentException("核对状态无效");
            try(PreparedStatement s=db.prepareStatement("UPDATE payments SET state=?,detail=detail||? WHERE id=? AND state NOT IN ('DONE','FAILED')")) {
                s.setString(1,state);s.setString(2,"；人工核对="+actor+"，时间="+System.currentTimeMillis());s.setString(3,id);
                if(s.executeUpdate()!=1) throw new IllegalArgumentException("交易不存在或已核对");
            }
            return null;
        },false);
    }
    public CompletableFuture<Void> rate(UUID player,long id,int rating) {
        return submit(() -> {
            Warp w=warps.get(id);
            if(w==null||!publicAvailable(w)&&!w.owner().equals(player)) throw new IllegalArgumentException("地标不可评价");
            if(rating<1||rating>5) throw new IllegalArgumentException("评分必须是 1 到 5 星");
            try(PreparedStatement s=db.prepareStatement("INSERT INTO ratings(player,warp,rating) VALUES(?,?,?) ON CONFLICT(player,warp) DO UPDATE SET rating=excluded.rating")) {
                s.setString(1,player.toString()); s.setLong(2,id); s.setInt(3,rating); s.executeUpdate();
            }
            return null;
        });
    }
    public CompletableFuture<Void> comment(UUID player,String author,long id,String text,int maxLength,int maxCount) {
        return comment(player,author,id,text,maxLength,maxCount,true);
    }
    public CompletableFuture<Void> comment(UUID player,String author,long id,String text,int maxLength,int maxCount,boolean notify) {
        return comment(player,author,id,text,maxLength,maxCount,notify,0);
    }
    public CompletableFuture<Void> comment(UUID player,String author,long id,String text,int maxLength,int maxCount,boolean notify,int cooldown) {
        return submit(() -> {
            requireUnfrozen(id);
            Warp w=warps.get(id);
            if(w==null||!publicAvailable(w)&&!w.owner().equals(player)) throw new IllegalArgumentException("地标不可留言");
            if(blocked(id,player)) throw new IllegalArgumentException("坐标主人已禁止你留言");
            validateText(text,maxLength);
            cooldown(player,cooldown);
            if(maxCount>0) {
                try(PreparedStatement count=db.prepareStatement("SELECT COUNT(*) FROM comments WHERE warp=?")) { count.setLong(1,id); try(ResultSet r=count.executeQuery()) { r.next(); if(r.getInt(1)>=maxCount) throw new IllegalArgumentException("该传送点留言已达到上限"); } }
            }
            try(PreparedStatement s=db.prepareStatement("INSERT INTO comments(player,author,warp,text,created) VALUES(?,?,?,?,?)")) {
                s.setString(1,player.toString()); s.setString(2,author); s.setLong(3,id); s.setString(4,text); s.setLong(5,System.currentTimeMillis()); s.executeUpdate();
            }
            long comment;
            try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT last_insert_rowid()")) { r.next(); comment=r.getLong(1); }
            enqueue(w.owner(),w,author,false,notify,comment);
            return null;
        });
    }
    private void enqueue(UUID player,Warp w,String author,boolean reply,boolean notify,long comment) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("INSERT INTO unread(player,warp,count) VALUES(?,?,1) ON CONFLICT(player,warp) DO UPDATE SET count=count+1")) {
            s.setString(1,player.toString()); s.setLong(2,w.id()); s.executeUpdate();
        }
        if(!notify) return;
        try(PreparedStatement s=db.prepareStatement("INSERT INTO notices(player,warp,name,author,reply,comment) VALUES(?,?,?,?,?,?)")) {
            s.setString(1,player.toString()); s.setLong(2,w.id()); s.setString(3,w.name()); s.setString(4,author); s.setBoolean(5,reply); s.setLong(6,comment); s.executeUpdate();
        }
        pruneNotices(player);
        rememberNotice(player);
    }
    private void rememberNotice(UUID player) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("INSERT INTO notice_history(id,player,warp,name,author,kind,comment,created) SELECT id,player,warp,name,author,reply,comment,? FROM notices WHERE id=last_insert_rowid()")) { s.setLong(1,System.currentTimeMillis()); s.executeUpdate(); }
        pruneHistory(player);
    }
    private void pruneHistory(UUID player) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("DELETE FROM notice_history WHERE player=? AND id NOT IN (SELECT id FROM notice_history WHERE player=? ORDER BY id DESC LIMIT ?)")) { s.setString(1,player.toString()); s.setString(2,player.toString()); s.setInt(3,historyLimit); s.executeUpdate(); }
    }
    // shortcut: 保留每人 100 条待推送，历史表仍保留；数据量更大时改为配置化分页队列。
    private void pruneNotices(UUID player) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("DELETE FROM notices WHERE player=? AND id NOT IN (SELECT id FROM notices WHERE player=? ORDER BY id DESC LIMIT 100)")) { s.setString(1,player.toString()); s.setString(2,player.toString()); s.executeUpdate(); }
    }
    public CompletableFuture<List<History>> history(UUID player) {
        return submit(() -> {
            pruneHistory(player);
            List<History> result=new ArrayList<>();
            try(PreparedStatement s=db.prepareStatement("SELECT id,warp,name,author,kind,comment,created,read FROM notice_history WHERE player=? ORDER BY id DESC")) {
                s.setString(1,player.toString()); try(ResultSet r=s.executeQuery()) { while(r.next()) result.add(new History(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getInt(5),r.getLong(6),r.getLong(7),r.getBoolean(8))); }
            }
            return List.copyOf(result);
        },false);
    }
    public CompletableFuture<Void> readHistory(UUID player,long id) {
        return submit(() -> {
            try(PreparedStatement s=db.prepareStatement("UPDATE notice_history SET read=1 WHERE player=? AND id=?")) { s.setString(1,player.toString()); s.setLong(2,id); s.executeUpdate(); }
            return null;
        },false);
    }
    public CompletableFuture<Void> reply(UUID owner,String author,long warp,long comment,String text,int maxLength,boolean notify) {
        return reply(owner,author,warp,comment,text,maxLength,notify,0);
    }
    public CompletableFuture<Void> reply(UUID owner,String author,long warp,long comment,String text,int maxLength,boolean notify,int cooldown) {
        return submit(() -> {
            requireUnfrozen(warp);
            Warp w=authorized(warp,owner,false);
            Comment target=comments(warp).stream().filter(c -> c.id()==comment).findFirst().orElseThrow(() -> new IllegalArgumentException("留言已删除或不属于此坐标"));
            validateText(text,maxLength);
            cooldown(owner,cooldown);
            try(PreparedStatement s=db.prepareStatement("INSERT INTO replies(comment,author,text,created) VALUES(?,?,?,?)")) {
                s.setLong(1,comment); s.setString(2,author); s.setString(3,text); s.setLong(4,System.currentTimeMillis()); s.executeUpdate();
            }
            enqueue(target.player(),w,author,true,notify,comment);
            return null;
        });
    }
    public CompletableFuture<List<Notice>> notices(UUID player) {
        return submit(() -> {
            List<Notice> result=new ArrayList<>();
            if(muted(player)) return List.of();
            try(PreparedStatement s=db.prepareStatement("SELECT id,warp,name,author,reply,comment FROM notices WHERE player=? ORDER BY id")) {
                s.setString(1,player.toString());
                try(ResultSet r=s.executeQuery()) { while(r.next()) result.add(new Notice(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getInt(5)==1,r.getInt(5),r.getLong(6))); }
            }
            return List.copyOf(result);
        },false);
    }
    private void uniqueName(String name,long id) {
        if(warps.values().stream().anyMatch(w -> w.id()!=id&&WarpText.plain(w.name()).equalsIgnoreCase(WarpText.plain(name)))) throw new IllegalArgumentException("去除颜色后的名称已存在");
    }
    private void validateText(String text,int limit) {
        if(WarpText.plain(text).isBlank()||WarpText.plain(text).length()>limit||text.length()>200||text.contains("\n")||text.contains("\r")) throw new IllegalArgumentException("文字为空、过长或含换行");
    }
    private void cooldown(UUID player,int seconds) throws SQLException {
        long now=System.currentTimeMillis();
        try(PreparedStatement s=db.prepareStatement("SELECT last FROM comment_cooldowns WHERE player=?")) {
            s.setString(1,player.toString());
            try(ResultSet r=s.executeQuery()) { if(r.next()&&now-r.getLong(1)<Math.max(0,seconds)*1000L) throw new IllegalArgumentException("留言或回复过于频繁，请稍后再试"); }
        }
        try(PreparedStatement s=db.prepareStatement("INSERT INTO comment_cooldowns VALUES(?,?) ON CONFLICT(player) DO UPDATE SET last=excluded.last")) {
            s.setString(1,player.toString()); s.setLong(2,now); s.executeUpdate();
        }
    }
    public CompletableFuture<Void> deleteComment(UUID actor,long warp,long comment) {
        return submit(() -> {
            Warp w=warps.get(warp);
            Comment target=comments(warp).stream().filter(c -> c.id()==comment).findFirst().orElseThrow(() -> new IllegalArgumentException("留言已删除"));
            if(w==null||!actor.equals(w.owner())&&!actor.equals(target.player())) throw new IllegalArgumentException("只能删除自己的留言或管理自己坐标的留言");
            try(PreparedStatement s=db.prepareStatement("DELETE FROM comments WHERE id=?")) { s.setLong(1,comment); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<Void> markRead(UUID player,long warp) {
        return write(() -> {
            try(PreparedStatement s=db.prepareStatement("DELETE FROM unread WHERE player=? AND warp=?")) { s.setString(1,player.toString()); s.setLong(2,warp); s.executeUpdate(); }
            return null;
        },() -> { Map<UUID,Map<Long,Integer>> next=new HashMap<>(unread);Map<Long,Integer> values=new HashMap<>(next.getOrDefault(player,Map.of()));values.remove(warp);next.put(player,Map.copyOf(values));unread=Map.copyOf(next); });
    }
    public CompletableFuture<Void> acknowledge(UUID player,List<Long> ids) {
        return submit(() -> {
            try(PreparedStatement s=db.prepareStatement("DELETE FROM notices WHERE player=? AND id=?")) {
                for(long id:ids) { s.setString(1,player.toString()); s.setLong(2,id); s.addBatch(); }
                s.executeBatch();
            }
            return null;
        },false);
    }
    public CompletableFuture<Void> pin(long id, int slot, boolean admin,int maxSlots) {
        return pin(id,slot,admin,maxSlots,"管理员（旧接口）");
    }
    public CompletableFuture<Void> pin(long id,int slot,boolean admin,int maxSlots,String actor) {
        return submit(() -> {
            if (!admin) throw new IllegalArgumentException("只有管理员可以设置置顶");
            if (!warps.containsKey(id)) throw new IllegalArgumentException("地标已删除");
            audit(actor,"置顶",warps.get(id),"位置="+slot+"；原占用="+pins.get(slot));
            if (slot < 0 || slot > maxSlots || slot > 36) throw new IllegalArgumentException("置顶位超出当前配置范围，0 表示取消");
            try (PreparedStatement s = db.prepareStatement("DELETE FROM pins WHERE warp=?")) {
                s.setLong(1,id); s.executeUpdate();
            }
            if (slot > 0) {
                try (PreparedStatement s = db.prepareStatement("INSERT INTO pins(slot,warp) VALUES(?,?) ON CONFLICT(slot) DO UPDATE SET warp=excluded.warp,expires=0")) {
                    s.setInt(1,slot); s.setLong(2,id); s.executeUpdate();
                }
            }
            return null;
        });
    }
    public CompletableFuture<Void> visit(long id) {
        return visit(id,null,100);
    }
    public CompletableFuture<Void> visit(long id,String visitor,int retained) {
        return write(() -> {
            try (PreparedStatement s = db.prepareStatement("UPDATE warps SET visits=visits+1 WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
            if(visitor!=null&&warps.containsKey(id)) {
                try(PreparedStatement s=db.prepareStatement("INSERT INTO visits(warp,visitor,created) VALUES(?,?,?)")) { s.setLong(1,id); s.setString(2,visitor); s.setLong(3,System.currentTimeMillis()); s.executeUpdate(); }
                try(PreparedStatement s=db.prepareStatement("DELETE FROM visits WHERE warp=? AND id NOT IN (SELECT id FROM visits WHERE warp=? ORDER BY id DESC LIMIT ?)")) { s.setLong(1,id); s.setLong(2,id); s.setInt(3,Math.max(1,retained)); s.executeUpdate(); }
                try(PreparedStatement s=db.prepareStatement("INSERT INTO daily_visits VALUES(?,?,1) ON CONFLICT(warp,day) DO UPDATE SET count=count+1")) { s.setLong(1,id); s.setString(2,java.time.LocalDate.now().toString()); s.executeUpdate(); }
                // shortcut: 日统计只服务最近 30 天页面，保留 90 天；需要长期分析时升级为归档表。
                try(PreparedStatement s=db.prepareStatement("DELETE FROM daily_visits WHERE day < date('now','-90 days')")) { s.executeUpdate(); }
            }
            return null;
        },() -> {
            Warp old=warps.get(id);if(old==null) return;
            Map<Long,Warp> next=new HashMap<>(warps);next.put(id,old.withVisits(old.visits()+1));warps=Map.copyOf(next);
        });
    }
    public CompletableFuture<Void> ticket(long id, UUID actor, boolean admin, Boolean enabled, Double price) {
        return submit(() -> {
            requireUnfrozen(id);
            Warp w=authorized(id,actor,admin);
            if(admin) audit(actor.toString(),"门票",w,"开关="+enabled+"；价格="+price);
            Ticket old=ticket(id);
            double value=price==null?old.price():price;
            if(!Double.isFinite(value)||value<0) throw new IllegalArgumentException("票价必须是非负有限数字");
            try(PreparedStatement s=db.prepareStatement("INSERT INTO tickets VALUES(?,?,?) ON CONFLICT(warp) DO UPDATE SET enabled=excluded.enabled,price=excluded.price")) {
                s.setLong(1,id); s.setBoolean(2,enabled==null?old.enabled():enabled); s.setDouble(3,value); s.executeUpdate();
            }
            return null;
        });
    }
    public CompletableFuture<Void> buyPin(long id, UUID actor, int slot,int maxSlots,long expires) {
        return submit(() -> {
            requireUnfrozen(id);
            expirePins();
            Warp w=authorized(id,actor,false);
            if(!w.visible()) throw new IllegalArgumentException("请先公开地标");
            if(slot<1||slot>maxSlots||slot>36) throw new IllegalArgumentException("置顶位超出当前配置范围");
            try(PreparedStatement s=db.prepareStatement("SELECT 1 FROM pins WHERE slot=? OR warp=?")) { s.setInt(1,slot);s.setLong(2,id);try(ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("推荐位已占用或此地标已置顶"); } }
            try(PreparedStatement s=db.prepareStatement("INSERT INTO pins(slot,warp,expires) VALUES(?,?,?)")) { s.setInt(1,slot); s.setLong(2,id); s.setLong(3,expires); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<Void> releasePin(long id, UUID actor) {
        return submit(() -> {
            authorized(id,actor,false);
            try(PreparedStatement s=db.prepareStatement("DELETE FROM pins WHERE warp=?")) { s.setLong(1,id); s.executeUpdate(); }
            return null;
        });
    }
    @Override public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(30, TimeUnit.SECONDS)) throw new IllegalStateException("数据库队列未在 30 秒内结束");
            db.close();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        catch (SQLException e) { throw new IllegalStateException(e); }
    }
    private void audit(String actor,String action,Warp w,String detail) throws SQLException {
        try(PreparedStatement s=db.prepareStatement("INSERT INTO audit(actor,action,warp,name,detail,created) VALUES(?,?,?,?,?,?)")) {
            s.setString(1,actor); s.setString(2,action); s.setLong(3,w.id()); s.setString(4,w.name()); s.setString(5,detail); s.setLong(6,System.currentTimeMillis()); s.executeUpdate();
        }
    }
    public CompletableFuture<Void> freeze(long id,String actor,boolean admin,boolean frozen,String reason) {
        return submit(() -> {
            if(!admin) throw new IllegalArgumentException("需要管理员权限");
            Warp w=warps.get(id); if(w==null) throw new IllegalArgumentException("坐标已删除");
            if(frozen) validateText(reason,100);
            try(PreparedStatement s=db.prepareStatement("INSERT INTO warp_status(warp,frozen,reason,actor) VALUES(?,?,?,?) ON CONFLICT(warp) DO UPDATE SET frozen=excluded.frozen,reason=excluded.reason,actor=excluded.actor")) {
                s.setLong(1,id); s.setBoolean(2,frozen); s.setString(3,frozen?reason:""); s.setString(4,actor); s.executeUpdate();
            }
            audit(actor,frozen?"冻结":"解冻",w,frozen?reason:status(id).reason());
            return null;
        });
    }
    public CompletableFuture<Void> event(long id,UUID actor,boolean admin,long start,long end) {
        return submit(() -> {
            Warp w=authorized(id,actor,admin);
            try(PreparedStatement s=db.prepareStatement("UPDATE warps SET visible=0 WHERE id=? AND id IN (SELECT warp FROM warp_events WHERE end<=?)")) { s.setLong(1,id); s.setLong(2,System.currentTimeMillis()); s.executeUpdate(); }
            if(start!=0||end!=0) {
                if(start<0||end<=start||end<=System.currentTimeMillis()) throw new IllegalArgumentException("活动结束时间需晚于开始时间及当前时间");
                try(PreparedStatement s=db.prepareStatement("INSERT INTO warp_events VALUES(?,?,?) ON CONFLICT(warp) DO UPDATE SET start=excluded.start,end=excluded.end")) { s.setLong(1,id); s.setLong(2,start); s.setLong(3,end); s.executeUpdate(); }
            } else try(PreparedStatement s=db.prepareStatement("DELETE FROM warp_events WHERE warp=?")) { s.setLong(1,id); s.executeUpdate(); }
            if(admin) audit(actor.toString(),"活动时间",w,"开始="+start+"；结束="+end);
            return null;
        });
    }
    public CompletableFuture<Boolean> checked(Warp expected,String invalid,boolean notify) {
        long[] checked=new long[1];
        return write(() -> {
            if(!expected.sameSettings(warps.get(expected.id()))) throw new IllegalArgumentException("检测期间坐标发生变更，请重新检查");
            Status old=status(expected.id());
            try(PreparedStatement s=db.prepareStatement("INSERT INTO warp_status(warp,invalid,checked) VALUES(?,?,?) ON CONFLICT(warp) DO UPDATE SET invalid=excluded.invalid,checked=excluded.checked")) {
                checked[0]=System.currentTimeMillis();s.setLong(1,expected.id()); s.setString(2,invalid); s.setLong(3,checked[0]); s.executeUpdate();
            }
            boolean changed=!invalid.isEmpty()&&!invalid.equals(old.invalid());
            if(!invalid.equals(old.invalid())) try(PreparedStatement s=db.prepareStatement("DELETE FROM notices WHERE warp=? AND reply=3")) { s.setLong(1,expected.id()); s.executeUpdate(); }
            if(changed&&notify) try(PreparedStatement s=db.prepareStatement("INSERT INTO notices(player,warp,name,author,reply) VALUES(?,?,?,?,3)")) {
                s.setString(1,expected.owner().toString()); s.setLong(2,expected.id()); s.setString(3,expected.name()); s.setString(4,invalid); s.executeUpdate();
                rememberNotice(expected.owner());
                pruneNotices(expected.owner());
            }
            return changed;
        },() -> {
            Status old=status(expected.id());Map<Long,Status> next=new HashMap<>(statuses);
            next.put(expected.id(),new Status(old.frozen(),old.reason(),old.actor(),invalid,checked[0]));statuses=Map.copyOf(next);
        });
    }
    public CompletableFuture<Void> mute(UUID player,boolean value) {
        return submit(() -> {
            try(PreparedStatement s=db.prepareStatement("INSERT INTO notification_settings VALUES(?,?) ON CONFLICT(player) DO UPDATE SET muted=excluded.muted")) { s.setString(1,player.toString()); s.setBoolean(2,value); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<Void> block(long warp,UUID owner,UUID player,String name,boolean value) {
        return submit(() -> {
            Warp w=authorized(warp,owner,false);
            if(player.equals(w.owner())) throw new IllegalArgumentException("不能将主人加入黑名单");
            try(PreparedStatement s=db.prepareStatement(value?"INSERT INTO warp_blocks VALUES(?,?,?) ON CONFLICT(warp,player) DO UPDATE SET name=excluded.name":"DELETE FROM warp_blocks WHERE warp=? AND player=?")) {
                s.setLong(1,warp); s.setString(2,player.toString()); if(value) s.setString(3,name); s.executeUpdate();
            }
            return null;
        });
    }
    public CompletableFuture<Path> export(Path directory,boolean admin) {
        return submit(() -> {
            if(!admin) throw new IllegalArgumentException("需要管理员权限");
            java.nio.file.Files.createDirectories(directory);
            Path output=directory.resolve("islandwarp-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".zip");
            try(var zip=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(output,java.nio.file.StandardOpenOption.CREATE_NEW),java.nio.charset.StandardCharsets.UTF_8)) {
                for(String table:List.of("warps","comments","replies","audit","warp_status","warp_events","income","payments")) {
                    zip.putNextEntry(new java.util.zip.ZipEntry(table+".csv"));
                    var writer=new java.io.OutputStreamWriter(zip,java.nio.charset.StandardCharsets.UTF_8);
                    writer.write('\ufeff');
                    try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM "+table)) {
                        var meta=r.getMetaData(); int columns=meta.getColumnCount();
                        for(int i=1;i<=columns;i++) { if(i>1) writer.write(','); writer.write(csv(meta.getColumnName(i))); } writer.write("\r\n");
                        while(r.next()) {
                            for(int i=1;i<=columns;i++) { if(i>1) writer.write(','); writer.write(csv(r.getString(i))); } writer.write("\r\n");
                        }
                    }
                    writer.flush(); zip.closeEntry();
                }
            } catch(Exception e) { java.nio.file.Files.deleteIfExists(output); throw e; }
            return output;
        },false);
    }
    private static String csv(String value) {
        if(value==null) value="";
        // Spreadsheet formula injection must remain inert when staff open player-authored text.
        if(value.stripLeading().matches("(?s)^[=+@\\-\\t\\r\\n].*")) value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    public CompletableFuture<Void> report(UUID player,long warp,long comment,String reason) {
        return submit(() -> {
            Warp w=warps.get(warp);
            if(w==null||!publicAvailable(w)&&!w.owner().equals(player)) throw new IllegalArgumentException("坐标不可用");
            Comment c=comments(warp).stream().filter(v -> v.id()==comment).findFirst().orElseThrow(() -> new IllegalArgumentException("留言已删除"));
            validateText(reason,100);
            try(PreparedStatement s=db.prepareStatement("INSERT INTO reports(warp,comment,reporter,author,text,reason,created) VALUES(?,?,?,?,?,?,?) ON CONFLICT(comment,reporter) DO UPDATE SET author=excluded.author,text=excluded.text,reason=excluded.reason,created=excluded.created,resolved=0 WHERE reports.resolved=1")) {
                s.setLong(1,warp); s.setLong(2,comment); s.setString(3,player.toString()); s.setString(4,c.author()); s.setString(5,c.text()); s.setString(6,reason); s.setLong(7,System.currentTimeMillis());
                if(s.executeUpdate()!=1) throw new IllegalArgumentException("这条留言已有你的待处理举报");
            }
            return null;
        },false);
    }
    public CompletableFuture<List<Report>> reports(boolean admin) {
        return submit(() -> {
            if(!admin) throw new IllegalArgumentException("需要管理员权限");
            List<Report> result=new ArrayList<>();
            try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT id,warp,comment,reporter,author,text,reason,created FROM reports WHERE resolved=0 ORDER BY id DESC")) {
                while(r.next()) result.add(new Report(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getLong(8)));
            }
            return List.copyOf(result);
        },false);
    }
    public CompletableFuture<Void> resolveReport(long id,UUID actor,boolean admin,boolean delete) {
        return submit(() -> {
            if(!admin) throw new IllegalArgumentException("需要管理员权限");
            try(PreparedStatement s=db.prepareStatement("SELECT warp,comment,text FROM reports WHERE id=? AND resolved=0")) {
                s.setLong(1,id);
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()) throw new IllegalArgumentException("举报已处理");
                    long warp=r.getLong(1),comment=r.getLong(2);
                    Warp w=warps.get(warp);
                    if(w!=null) audit(actor.toString(),"处理举报",w,"举报="+id+"；删除留言="+delete+"；原文="+r.getString(3));
                    if(delete) try(PreparedStatement d=db.prepareStatement("DELETE FROM comments WHERE id=? AND warp=?")) { d.setLong(1,comment); d.setLong(2,warp); d.executeUpdate(); }
                    if(delete) {
                        try(PreparedStatement d=db.prepareStatement("DELETE FROM recycle_replies WHERE comment IN (SELECT id FROM recycle_comments WHERE id=? AND warp=?)")) { d.setLong(1,comment);d.setLong(2,warp);d.executeUpdate(); }
                        try(PreparedStatement d=db.prepareStatement("DELETE FROM recycle_comments WHERE id=? AND warp=?")) { d.setLong(1,comment);d.setLong(2,warp);d.executeUpdate(); }
                        try(PreparedStatement d=db.prepareStatement("UPDATE reports SET resolved=1 WHERE comment=? AND warp=?")) { d.setLong(1,comment);d.setLong(2,warp);d.executeUpdate(); }
                    }
                }
            }
            try(PreparedStatement s=db.prepareStatement("UPDATE reports SET resolved=1 WHERE id=?")) { s.setLong(1,id); s.executeUpdate(); }
            return null;
        });
    }
    public CompletableFuture<List<Audit>> audits(boolean admin) {
        return submit(() -> {
            if(!admin) throw new IllegalArgumentException("需要管理员权限");
            List<Audit> result=new ArrayList<>();
            try(Statement s=db.createStatement(); ResultSet r=s.executeQuery("SELECT actor,action,warp,name,detail,created FROM audit ORDER BY id DESC LIMIT 200")) {
                while(r.next()) result.add(new Audit(r.getString(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5),r.getLong(6)));
            }
            return List.copyOf(result);
        },false);
    }
    public CompletableFuture<List<Visit>> visits(long warp,UUID actor,boolean admin) {
        return submit(() -> {
            authorized(warp,actor,admin);
            List<Visit> result=new ArrayList<>();
            try(PreparedStatement s=db.prepareStatement("SELECT visitor,created FROM visits WHERE warp=? ORDER BY id DESC")) { s.setLong(1,warp); try(ResultSet r=s.executeQuery()) { while(r.next()) result.add(new Visit(r.getString(1),r.getLong(2))); } }
            return List.copyOf(result);
        },false);
    }
    public CompletableFuture<Map<String,Integer>> dailyVisits(long warp,UUID actor,boolean admin) {
        return submit(() -> {
            authorized(warp,actor,admin);
            Map<String,Integer> result=new LinkedHashMap<>();
            try(PreparedStatement s=db.prepareStatement("SELECT day,count FROM daily_visits WHERE warp=? ORDER BY day DESC LIMIT 30")) { s.setLong(1,warp); try(ResultSet r=s.executeQuery()) { while(r.next()) result.put(r.getString(1),r.getInt(2)); } }
            return Collections.unmodifiableMap(result);
        },false);
    }
    public CompletableFuture<Void> renewPin(long warp,UUID owner,long expectedExpiry,long duration) {
        return submit(() -> {
            requireUnfrozen(warp);
            Warp w=authorized(warp,owner,false);
            if(!w.visible()||duration<=0) throw new IllegalArgumentException("坐标需公开且续费期限需大于零");
            try(PreparedStatement s=db.prepareStatement("UPDATE pins SET expires=? WHERE warp=? AND expires=? AND expires>?")) {
                s.setLong(1,Math.addExact(expectedExpiry,duration)); s.setLong(2,warp); s.setLong(3,expectedExpiry); s.setLong(4,System.currentTimeMillis());
                if(s.executeUpdate()!=1) throw new IllegalArgumentException("置顶已到期或期限变更，请重新确认");
            }
            return null;
        });
    }
    public CompletableFuture<Void> remindPins(long leadMillis) {
        return write(() -> {
            purgeTrash();
            try(PreparedStatement s=db.prepareStatement("DELETE FROM pins WHERE expires>0 AND expires<=?")) { s.setLong(1,System.currentTimeMillis());s.executeUpdate(); }
            if(leadMillis<=0) return null;
            long now=System.currentTimeMillis();
            try(PreparedStatement s=db.prepareStatement("SELECT p.slot,p.warp,p.expires FROM pins p LEFT JOIN pin_reminders r ON p.slot=r.slot WHERE p.expires>? AND p.expires-?<=? AND (r.expires IS NULL OR r.expires<>p.expires)")) {
                s.setLong(1,now); s.setLong(2,now); s.setLong(3,leadMillis);
                try(ResultSet r=s.executeQuery()) { while(r.next()) {
                    Warp w=warps.get(r.getLong(2)); if(w==null) continue;
                    try(PreparedStatement n=db.prepareStatement("INSERT INTO notices(player,warp,name,author,reply) VALUES(?,?,?,?,2)")) {
                        n.setString(1,w.owner().toString()); n.setLong(2,w.id()); n.setString(3,w.name()); n.setString(4,new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(r.getLong(3)))); n.executeUpdate();
                        rememberNotice(w.owner());
                        pruneNotices(w.owner());
                    }
                    try(PreparedStatement n=db.prepareStatement("INSERT INTO pin_reminders VALUES(?,?) ON CONFLICT(slot) DO UPDATE SET expires=excluded.expires")) { n.setInt(1,r.getInt(1)); n.setLong(2,r.getLong(3)); n.executeUpdate(); }
                } }
            }
            return null;
        },() -> {
            long now=System.currentTimeMillis();Map<Integer,Long> next=new HashMap<>(pins),expires=new HashMap<>(pinExpiry);
            expires.entrySet().removeIf(e -> { if(e.getValue()>0&&e.getValue()<=now) { next.remove(e.getKey());return true; }return false; });
            pins=Map.copyOf(next);pinExpiry=Map.copyOf(expires);
        });
    }
    private void expireData()throws SQLException {
        try(PreparedStatement s=db.prepareStatement("UPDATE warps SET visible=0 WHERE id IN (SELECT warp FROM warp_events WHERE end<=?)")) { s.setLong(1,System.currentTimeMillis());s.executeUpdate(); }
        expirePins();
    }
    private void expirePins()throws SQLException {
        try(PreparedStatement s=db.prepareStatement("DELETE FROM pins WHERE expires>0 AND expires<=?")) { s.setLong(1,System.currentTimeMillis());s.executeUpdate(); }
    }
    public CompletableFuture<Void> expireEvents() {
        return submit(() -> {
            try(PreparedStatement s=db.prepareStatement("UPDATE warps SET visible=0 WHERE id IN (SELECT warp FROM warp_events WHERE end<=?)")) { s.setLong(1,System.currentTimeMillis()); s.executeUpdate(); }
            return null;
        });
    }
}
