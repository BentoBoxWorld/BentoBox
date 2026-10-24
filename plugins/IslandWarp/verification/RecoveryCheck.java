import com.islandwarp.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class RecoveryCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future) throws Exception {
        try { future.get(); throw new AssertionError("invalid operation succeeded"); } catch(ExecutionException expected) {}
    }
    static Warp warp(UUID owner,String name,String island) {
        return new Warp(0,owner,"owner",name,"description","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,island,1,0);
    }
    public static void main(String[] args) throws Exception {
        Path path=Path.of(args[0]); UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(); String island="island-recover"; long id,comment;
        try(WarpStore db=new WarpStore(path)) {
            id=db.create(warp(owner,"&aRecover",island),5,1).get();
            db.comment(guest,"guest",id,"original",100,50).get(); comment=db.comments(id).get(0).id();
            db.reply(owner,"owner",id,comment,"reply",100,true).get();
            rejected(db.editComment(owner,id,comment,"forged",100,0));
            rejected(db.editComment(guest,id,comment," ",100,0));
            db.editComment(guest,id,comment,"&aedited",100,0).get();
            check(db.comments(id).get(0).edited()>0&&db.replies(comment).size()==1,"edit marker and replies retained");
            rejected(db.editComment(guest,id,comment,"too fast",100,10));
            db.ticket(id,owner,false,true,10.0).get(); db.favorite(guest,id).get(); db.rate(guest,id,5).get();
            db.visit(id,"guest",100).get(); db.block(id,owner,guest,"guest",true).get();
            db.buyPin(id,owner,1,7,System.currentTimeMillis()+86400000).get();
            db.income(owner,id,"Recover",10,9.5,true).get(); db.income(owner,id,"Recover",20,19,false).get();
            rejected(db.income(owner,id,"Recover",Double.NaN,0,true));
            db.delete(id,owner,false,7).get();
            check(db.all().isEmpty()&&db.comments(id).isEmpty()&&db.pins().isEmpty(),"deleted is inaccessible");
            check(db.trash(guest,false).get().isEmpty()&&db.trash(owner,false).get().size()==1,"trash privacy");
            rejected(db.restore(id,guest,false,5,1));
            long collision=db.create(warp(owner,"Recover",null),5,0).get();
            rejected(db.restore(id,owner,false,5,1)); db.delete(collision,owner,false).get();
            rejected(db.restore(id,owner,false,0,1));
            long full=db.create(warp(owner,"Full",island),5,1).get();
            rejected(db.restore(id,owner,true,5,1)); db.delete(full,owner,false).get();
        }
        try(WarpStore db=new WarpStore(path)) {
            db.restore(id,owner,false,5,1).get();
            check(db.all().get(id).name().equals("&aRecover"),"restart restore original identity");
            check(db.comments(id).get(0).text().equals("&aedited")&&db.replies(comment).size()==1,"restore comments and replies");
            check(db.ticket(id).enabled()&&db.favorites(guest).contains(id)&&db.rating(guest,id)==5,"restore ticket favorites rating");
            check(db.visits(id,owner,false).get().size()==1&&db.dailyVisits(id,owner,false).get().size()==1&&db.blocked(id,guest),"restore visits and blocks");
            check(db.pins().isEmpty()&&db.notices(owner).get().isEmpty(),"no stale pin or notice");
            check(db.income(owner).get().size()==2&&db.income(guest).get().isEmpty(),"income persists and is private");
            check(db.income(owner).get().stream().filter(WarpStore.Income::paid).mapToDouble(WarpStore.Income::net).sum()==9.5,"failed settlement not paid");
            db.delete(id,owner,false,7).get();
        }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path); Statement s=c.createStatement()) { s.execute("UPDATE recycle SET expires=1"); }
        try(WarpStore db=new WarpStore(path)) {
            rejected(db.restore(id,owner,false,5,1));
            check(db.trash(owner,false).get().isEmpty(),"expiry purge");
        }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT COUNT(*) FROM recycle_comments")) { r.next(); check(r.getInt(1)==0,"expired child snapshots removed"); }
        System.out.println("RECOVERY_CHECK_PASS: restore, ownership, conflicts, limits, linked data, expiry, edited comments, income, restart");
    }
}
