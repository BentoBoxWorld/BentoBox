import com.islandwarp.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class NoticeCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path path=Path.of(args[0]); UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(),other=UUID.randomUUID(); long id,comment,entry;
        try(WarpStore db=new WarpStore(path)) {
            id=db.create(new Warp(0,owner,"owner","Notice","","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0),5,0).get();
            db.comment(guest,"guest",id,"hello",100,50).get(); comment=db.comments(id).get(0).id();
            entry=db.history(owner).get().get(0).id();
            check(db.history(other).get().isEmpty(),"history is private");
            db.readHistory(other,entry).get(); check(!db.history(owner).get().get(0).read(),"cross-user read cannot change state");
            db.acknowledge(owner,db.notices(owner).get().stream().map(WarpStore.Notice::id).toList()).get();
            check(db.notices(owner).get().isEmpty()&&db.history(owner).get().size()==1,"push acknowledgement retains history");
            db.readHistory(owner,entry).get();
            check(db.history(owner).get().get(0).read()&&db.unread(owner,id)==1,"history read independent of board unread");
            db.mute(owner,true).get(); db.comment(guest,"guest",id,"second",100,50).get();
            check(db.notices(owner).get().isEmpty()&&db.history(owner).get().size()==2,"muted retains history");
            db.reply(owner,"owner",id,comment,"reply",100,true).get();
            check(db.history(guest).get().get(0).kind()==1&&db.history(guest).get().get(0).comment()==comment,"reply target persists");
            db.buyPin(id,owner,1,7,System.currentTimeMillis()+3600000).get(); db.remindPins(7200000).get();
            db.checked(db.all().get(id),"unsafe",true).get();
            check(db.history(owner).get().stream().anyMatch(n -> n.kind()==2)&&db.history(owner).get().stream().anyMatch(n -> n.kind()==3),"pin and health history");
            db.checked(db.all().get(id),"",true).get();
            check(db.history(owner).get().size()==4,"health recovery retains historical event");
        }
        try(WarpStore db=new WarpStore(path)) {
            check(db.history(owner).get().size()==4&&db.history(owner).get().stream().anyMatch(n -> n.id()==entry&&n.read()),"restart retains history and read state");
            db.delete(id,owner,false).get();
            check(db.notices(guest).get().isEmpty()&&db.history(guest).get().size()==1,"deleted warp retains history, not push queue");
            db.historyLimit(2); check(db.history(owner).get().size()==2,"config shrink prunes oldest");
            db.historyLimit(100); check(db.history(owner).get().size()==2,"pruned entries not restored");
            long next=db.create(new Warp(0,owner,"owner","New","","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0),5,0).get();
            db.historyLimit(2);
            for(int i=0;i<5;i++) db.comment(guest,"guest",next,"comment "+i,100,50).get();
            check(db.history(owner).get().size()==2&&db.notices(owner).get().isEmpty(),"write retention while muted");
            db.mute(owner,false).get();
            check(db.notices(owner).get().size()==5,"history cap does not drop pending pushes");
        }
        try(WarpStore db=new WarpStore(path)) { check(db.history(owner).get().size()==2,"restart does not reimport pruned pending records"); }
        // An older installation has only the pending queue, without history timestamps.
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path); Statement s=c.createStatement()) { s.execute("DROP TABLE notice_history"); }
        try(WarpStore db=new WarpStore(path)) {
            check(db.history(owner).get().size()==5&&db.history(owner).get().stream().allMatch(n -> n.created()==0),"legacy pending notifications imported once");
            db.readHistory(owner,db.history(owner).get().get(0).id()).get();
        }
        try(WarpStore db=new WarpStore(path)) { check(db.history(owner).get().size()==5&&db.history(owner).get().get(0).read(),"migration idempotent"); }
        System.out.println("NOTICE_CHECK_PASS: ownership, read state, push acknowledgement, mute, targets, retention, deleted warps, restart, legacy migration");
    }
}
