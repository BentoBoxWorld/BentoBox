import com.islandwarp.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class FeatureCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future,String message) throws Exception {
        try { future.get(); throw new AssertionError(message); } catch(ExecutionException expected) {}
    }
    public static void main(String[] args) throws Exception {
        UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(),outsider=UUID.randomUUID();
        long warp,comment,expiry=System.currentTimeMillis()+3600000;
        try(WarpStore db=new WarpStore(Path.of(args[0]))) {
            warp=db.create(new Warp(0,owner,"owner","&a岛屿","","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0),5,0).get();
            db.comment(guest,"guest",warp,"测试留言",100,50).get();
            comment=db.comments(warp).get(0).id();
            db.mute(owner,true).get();
            check(db.muted(owner)&&db.notices(owner).get().isEmpty()&&db.unread(owner)==1,"muted keeps unread");
            db.report(guest,warp,comment,"广告").get();
            rejected(db.report(guest,warp,comment,"再次举报"),"duplicate pending report");
            rejected(db.reports(false),"reports permission");
            check(db.reports(true).get().size()==1,"duplicate reports");
            long report=db.reports(true).get().get(0).id();
            rejected(db.resolveReport(report,outsider,false,true),"resolve permission");
            db.visit(warp,"guest",2).get(); db.visit(warp,"owner",2).get(); db.visit(warp,"third",2).get();
            check(db.visits(warp,owner,false).get().size()==2,"visitor retention");
            check(db.dailyVisits(warp,owner,false).get().values().stream().mapToInt(Integer::intValue).sum()==3,"daily totals");
            rejected(db.visits(warp,outsider,false),"visitor privacy");
            db.buyPin(warp,owner,1,7,expiry).get();
            db.remindPins(7200000).get(); db.remindPins(7200000).get();
            db.mute(owner,false).get();
            check(db.notices(owner).get().size()==2&&db.notices(owner).get().stream().filter(n -> n.kind()==2).count()==1,"one reminder per expiry");
            rejected(db.renewPin(warp,outsider,expiry,3600000),"renew ownership");
            rejected(db.renewPin(warp,owner,expiry-1,3600000),"stale renewal");
            db.renewPin(warp,owner,expiry,3600000).get();
            check(db.pinExpiry(1)==expiry+3600000,"extend from old expiry");
            db.remindPins(10800000).get();
            check(db.notices(owner).get().stream().filter(n -> n.kind()==2).count()==2,"renewed expiry new reminder");
            db.edit(warp,outsider,true,w -> w.edit(w.name(),"admin edited",w.category(),w.icon(),true)).get();
            db.ticket(warp,outsider,true,true,5.0).get();
            db.pin(warp,1,true,7,outsider.toString()).get();
            check(db.pinExpiry(1)==0,"admin pin permanent");
            db.resolveReport(report,outsider,true,true).get();
            check(db.comments(warp).isEmpty()&&db.reports(true).get().isEmpty(),"report delete and close");
            check(db.audits(true).get().size()==4,"audit edit ticket pin report");
            rejected(db.audits(false),"audit permission");
            db.mute(owner,true).get();
        }
        try(WarpStore db=new WarpStore(Path.of(args[0]))) {
            check(db.muted(owner)&&db.notices(owner).get().isEmpty(),"mute restart persistence");
            check(db.visits(warp,owner,false).get().size()==2,"visits restart persistence");
            db.mute(owner,false).get();
            check(db.notices(owner).get().size()==3,"queued notifications survive restart and mute");
            db.delete(warp,outsider,true).get();
            check(db.audits(true).get().size()==5,"deletion audit survives warp deletion");
            check(db.notices(owner).get().isEmpty(),"notice cascade");
        }
        System.out.println("FEATURE_CHECK_PASS: reports, permissions, mute/unread, visits/privacy, renew/stale payment, reminder dedup, audits, restart");
    }
}
