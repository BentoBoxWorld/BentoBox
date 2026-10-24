import com.islandwarp.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class StatusCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future) throws Exception {
        try { future.get(); throw new AssertionError("invalid operation succeeded"); } catch(ExecutionException expected) {}
    }
    static void blocked(WarpStore db,long id) {
        try { db.requireUnfrozen(id); throw new AssertionError("frozen access succeeded"); } catch(IllegalArgumentException expected) {}
    }
    public static void main(String[] args) throws Exception {
        Path path=Path.of(args[0]); UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(); long id,comment;
        try(WarpStore db=new WarpStore(path)) {
            id=db.create(new Warp(0,owner,"owner","Status","","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0),5,0).get();
            Warp w=db.all().get(id);
            db.comment(guest,"guest",id,"existing",100,50,false).get(); comment=db.comments(id).get(0).id();
            db.ticket(id,owner,false,true,10.0).get();
            check(db.checked(w,"world unavailable",true).get(),"new abnormal status");
            check(!db.checked(w,"world unavailable",true).get(),"same abnormal state dedup");
            check(db.notices(owner).get().size()==1&&db.notices(owner).get().get(0).kind()==3,"persistent owner notice");
            rejected(db.freeze(id,"guest",false,true,"violation"));
            rejected(db.freeze(id,"admin",true,true," "));
            db.freeze(id,"admin",true,true,"violation").get(); blocked(db,id);
            check(db.status(id).invalid().equals("world unavailable"),"freeze preserves health");
            rejected(db.comment(guest,"guest",id,"new",100,50));
            rejected(db.reply(owner,"owner",id,comment,"reply",100,true));
            rejected(db.editComment(guest,id,comment,"edit",100,0));
            rejected(db.ticket(id,owner,false,true,50.0));
            check(db.comments(id).size()==1&&db.ticket(id).price()==10,"freeze does not destroy data");
            db.checked(w,"",true).get();
            check(db.status(id).frozen()&&db.status(id).invalid().isEmpty()&&db.notices(owner).get().isEmpty(),"recovery clears stale notice, not freeze");
            db.mute(owner,true).get(); db.checked(w,"unsafe landing",true).get();
            check(db.notices(owner).get().isEmpty(),"muted owner notices hidden");
            db.mute(owner,false).get(); check(db.notices(owner).get().size()==1,"muted notice retained");
            db.edit(id,owner,false,v -> new Warp(v.id(),v.owner(),v.ownerName(),v.name(),v.description(),v.category(),v.world(),10,65,10,0,0,v.icon(),v.visible(),v.island(),v.created(),v.visits())).get();
            rejected(db.checked(w,"outdated result",true));
            check(db.status(id).checked()==0&&db.status(id).invalid().isEmpty()&&db.status(id).frozen(),"move invalidates old health, retains freeze");
            db.checked(db.all().get(id),"unsafe landing",true).get();
        }
        try(WarpStore db=new WarpStore(path)) {
            blocked(db,id); check(db.status(id).reason().equals("violation")&&db.status(id).invalid().equals("unsafe landing"),"restart persists status");
            check(db.notices(owner).get().size()==1,"offline notice persists");
            db.delete(id,owner,false,7).get(); db.restore(id,owner,false,5,0).get(); blocked(db,id);
            db.freeze(id,"admin",true,false,"").get(); db.requireUnfrozen(id);
            check(db.status(id).invalid().equals("unsafe landing"),"unfreeze does not clear health");
            db.checked(db.all().get(id),"",false).get();
            check(db.status(id).checked()>0&&!db.status(id).frozen(),"health recovery");
            db.reply(owner,"owner",id,comment,"allowed",100,true).get();
            db.editComment(guest,id,comment,"allowed edit",100,0).get();
            db.ticket(id,owner,false,true,20.0).get();
            check(db.audits(true).get().size()==2,"freeze and unfreeze audited");
            Path export=db.export(path.getParent().resolve("status-exports"),true).get();
            try(ZipFile zip=new ZipFile(export.toFile())) {
                String csv=new String(zip.getInputStream(zip.getEntry("warp_status.csv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                check(csv.contains("frozen")&&csv.contains("checked"),"status export included");
            }
        }
        System.out.println("STATUS_CHECK_PASS: freeze permissions, gates, persistence, recovery, audit, health dedup, stale results, notices, export");
    }
}
