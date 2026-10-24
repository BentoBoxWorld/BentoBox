import com.islandwarp.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class StoreCheck {
    static Warp warp(UUID owner,String name) {
        return new Warp(0,owner,"tester",name,"描述","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0);
    }
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        UUID owner=UUID.randomUUID(), visitor=UUID.randomUUID(); long retained;
        check(WarpText.name("&a彩色名称",24).equals("&a彩色名称"),"colored name validation");
        check(WarpText.plain("&a彩色&l名称").equals("彩色名称"),"plain name search");
        try { WarpText.name("&a",24); throw new AssertionError("empty colored name"); } catch(IllegalArgumentException expected) {}
        try(WarpStore db=new WarpStore(Path.of(args[0]))) {
            long first=db.create(warp(owner,"测试地标"),2,0).get();
            retained=db.create(warp(owner,"Second"),2,0).get();
            try { db.create(warp(visitor,"&aSecond"),5,0).get(); throw new AssertionError("colored duplicate"); } catch(ExecutionException expected) {}
            db.comment(visitor,"visitor",first,"第一个坐标留言",100,2).get();
            db.comment(visitor,"visitor",retained,"第二个坐标留言",100,1).get();
            check(db.comments(first).size()==1 && db.comments(first).get(0).text().equals("第一个坐标留言"),"comments bound to first warp");
            check(db.comments(retained).size()==1 && db.comments(retained).get(0).text().equals("第二个坐标留言"),"comments bound to second warp");
            long comment=db.comments(retained).get(0).id();
            try { db.reply(visitor,"visitor",retained,comment,"冒充主人",100,true).get(); throw new AssertionError("reply ownership"); } catch(ExecutionException expected) {}
            try { db.reply(owner,"tester",first,comment,"错误坐标",100,true).get(); throw new AssertionError("reply warp binding"); } catch(ExecutionException expected) {}
            try { db.reply(owner,"tester",retained,comment,"\n",100,true).get(); throw new AssertionError("reply validation"); } catch(ExecutionException expected) {}
            db.reply(owner,"tester",retained,comment,"感谢留言",100,true).get();
            check(db.replies(comment).size()==1 && db.replies(comment).get(0).text().equals("感谢留言"),"owner reply");
            check(db.unread(owner)==2 && db.unread(visitor,retained)==1,"unread counts");
            try { db.comment(visitor,"visitor",first,"太快",100,10,true,60).get(); throw new AssertionError("cooldown"); } catch(ExecutionException expected) {}
            try { db.comment(visitor,"visitor",first,"&a",100,10).get(); throw new AssertionError("blank color text"); } catch(ExecutionException expected) {}
            try { db.deleteComment(UUID.randomUUID(),retained,comment).get(); throw new AssertionError("delete comment permission"); } catch(ExecutionException expected) {}
            check(db.notices(owner).get().size()==2,"comment notices queued");
            var replyNotices=db.notices(visitor).get();
            check(replyNotices.size()==1 && replyNotices.get(0).reply() && replyNotices.get(0).warp()==retained,"reply notice queued");
            db.acknowledge(owner,replyNotices.stream().map(WarpStore.Notice::id).toList()).get();
            check(db.notices(visitor).get().size()==1,"cannot acknowledge another player notice");
            try { db.comment(visitor,"visitor",retained,"超过上限",100,1).get(); throw new AssertionError("comment count limit"); } catch(ExecutionException expected) {}
            try { db.comment(visitor,"visitor",first,"\n",100,2).get(); throw new AssertionError("comment validation"); } catch(ExecutionException expected) {}
            db.ticket(retained,owner,false,true,12.5).get();
            check(db.ticket(retained).enabled() && db.ticket(retained).price()==12.5,"ticket save");
            try { db.ticket(retained,visitor,false,false,1.0).get(); throw new AssertionError("ticket ownership"); } catch(ExecutionException expected) {}
            try { db.ticket(retained,owner,false,true,Double.NaN).get(); throw new AssertionError("ticket finite"); } catch(ExecutionException expected) {}
            db.buyPin(retained,owner,4,10,0).get();
            try { db.buyPin(first,owner,4,10,0).get(); throw new AssertionError("occupied paid pin"); } catch(ExecutionException expected) {}
            try { db.buyPin(retained,visitor,5,10,0).get(); throw new AssertionError("paid pin ownership"); } catch(ExecutionException expected) {}
            db.releasePin(retained,owner).get();
            try { db.create(warp(owner,"Third"),2,0).get(); throw new AssertionError("limit"); } catch(ExecutionException expected) {}
            try { db.create(warp(visitor,"second"),2,0).get(); throw new AssertionError("unique"); } catch(ExecutionException expected) {}
            try { db.delete(first,visitor,false).get(); throw new AssertionError("ownership"); } catch(ExecutionException expected) {}
            check(db.favorite(visitor,first).get(),"favorite add");
            try { db.pin(first,1,false,10).get(); throw new AssertionError("pin permission"); } catch(ExecutionException expected) {}
            try { db.pin(first,11,true,10).get(); throw new AssertionError("pin range"); } catch(ExecutionException expected) {}
            db.pin(first,1,true,10).get();
            db.pin(retained,1,true,10).get();
            check(db.pins().get(1)==retained && db.pins().size()==1,"pin replacement");
            db.pin(retained,2,true,10).get();
            check(!db.pins().containsKey(1) && db.pins().get(2)==retained,"pin move");
            db.pin(first,3,true,10).get();
            check(!db.favorite(visitor,first).get(),"favorite remove");
            db.favorite(visitor,first).get();
            db.edit(first,owner,false,w -> w.edit(w.name(),w.description(),w.category(),w.icon(),false)).get();
            check(!db.favorite(visitor,first).get(),"hidden favorite removal");
            try { db.favorite(visitor,first).get(); throw new AssertionError("hidden add"); } catch(ExecutionException expected) {}
            db.delete(first,owner,false).get();
            check(db.comments(first).isEmpty(),"comment cascade delete");
            check(db.favorites(visitor).isEmpty(),"cascade delete");
            check(!db.pins().containsKey(3),"pin cascade delete");
            List<CompletableFuture<Void>> updates=new ArrayList<>();
            for(int i=0;i<50;i++) updates.add(db.visit(retained));
            CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new)).get();
            check(db.all().get(retained).visits()==50,"concurrent visits");
        }
        try(WarpStore db=new WarpStore(Path.of(args[0]))) {
            check(db.all().size()==1&&db.all().get(retained).visits()==50,"restart persistence");
            check(db.pins().get(2)==retained,"pin restart persistence");
            check(db.ticket(retained).enabled() && db.ticket(retained).price()==12.5,"ticket persistence");
            check(db.comments(retained).size()==1 && db.comments(retained).get(0).text().equals("第二个坐标留言"),"comment persistence");
            check(db.replies(db.comments(retained).get(0).id()).size()==1,"reply restart persistence");
            check(db.notices(owner).get().size()==1,"notice cascade delete and restart persistence");
            var pending=db.notices(visitor).get();
            check(pending.size()==1 && pending.get(0).reply(),"offline reply notice restart persistence");
            db.acknowledge(visitor,pending.stream().map(WarpStore.Notice::id).toList()).get();
            check(db.notices(visitor).get().isEmpty(),"notice acknowledged after delivery");
            check(db.unread(visitor,retained)==1,"delivery does not mark read");
            db.markRead(visitor,retained).get();
            check(db.unread(visitor)==0,"mark read");
            long comment=db.comments(retained).get(0).id();
            db.deleteComment(visitor,retained,comment).get();
            check(db.comments(retained).isEmpty()&&db.replies(comment).isEmpty(),"comment author delete and reply cascade");
            db.pin(retained,0,true,10).get();
            check(db.pins().isEmpty(),"unpin");
        }
        // Simulate upgrading a pre-feature database, preserving existing rows and favorites.
        try(var connection=java.sql.DriverManager.getConnection("jdbc:sqlite:"+Path.of(args[0]).toAbsolutePath()); var statement=connection.createStatement()) {
            statement.execute("DROP TABLE pins");
            statement.execute("CREATE TABLE pins(slot INTEGER PRIMARY KEY CHECK(slot BETWEEN 1 AND 5),warp INTEGER NOT NULL UNIQUE REFERENCES warps(id) ON DELETE CASCADE)");
        }
        try(WarpStore db=new WarpStore(Path.of(args[0]))) {
            check(db.all().get(retained).visits()==50 && db.pins().isEmpty(),"legacy migration");
            db.pin(retained,10,true,10).get();
            check(db.pins().get(10)==retained,"expanded pin schema");
        }
        System.out.println("STORE_CHECK_PASS: ownership, unique names, limits, favorites, hidden access, cascade delete, concurrent visits, tickets, pins, migration, per-warp comments, owner replies, offline notice persistence, recipient-scoped acknowledgement");
    }
}
