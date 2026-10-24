import com.islandwarp.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class EventSearchCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future) throws Exception {
        try { future.get(); throw new AssertionError("invalid operation succeeded"); } catch(ExecutionException expected) {}
    }
    static void invalidFilter(double price,double rating) {
        try { new SearchFilter(price,rating); throw new AssertionError("invalid filter accepted"); } catch(IllegalArgumentException expected) {}
    }
    public static void main(String[] args) throws Exception {
        UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(); Path path=Path.of(args[0]); long id;
        Warp w=new Warp(0,owner,"Alice","&aFestival","&6Large Shop","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0);
        SearchFilter filter=new SearchFilter(10,4);
        check(filter.matches(w,"alice",10,4)&&filter.matches(w,"SHOP",0,5)&&filter.matches(w,"festival",10,4),"owner description colored name search");
        check(!filter.matches(w,"alice",10.01,5)&&!filter.matches(w,"shop",10,3.99)&&!filter.matches(w,"missing",0,5),"combined filter boundaries");
        check(new SearchFilter(0,0).matches(w,"",0,0),"free and unrated");
        invalidFilter(Double.NaN,0); invalidFilter(-1,0); invalidFilter(10,5.1); invalidFilter(10,Double.POSITIVE_INFINITY);
        check(new WarpStore.Event(10,20).active(10)&&!new WarpStore.Event(10,20).active(9)&&!new WarpStore.Event(10,20).active(20),"start inclusive end exclusive");
        check(WarpText.eventTime("2030-01-01T12:00")>0,"parse server time");
        try { WarpText.eventTime("2030-02-30T12:00"); throw new AssertionError("invalid date accepted"); } catch(IllegalArgumentException expected) {}
        try(WarpStore db=new WarpStore(path)) {
            id=db.create(w,5,0).get(); w=db.all().get(id); long now=System.currentTimeMillis();
            rejected(db.event(id,guest,false,now+60000,now+120000));
            rejected(db.event(id,owner,false,now+120000,now+60000));
            rejected(db.event(id,owner,false,1,2));
            db.event(id,owner,false,now+60000,now+120000).get();
            check(!db.publicAvailable(db.all().get(id)),"scheduled not public");
            try { db.requireEventActive(id); throw new AssertionError("early teleport allowed"); } catch(IllegalArgumentException expected) {}
            rejected(db.comment(guest,"guest",id,"early",100,50)); rejected(db.rate(guest,id,5));
            db.event(id,owner,false,now-1000,now+120000).get(); db.requireEventActive(id);
            check(db.publicAvailable(db.all().get(id)),"active is public");
            db.comment(guest,"guest",id,"active",100,50).get();
            db.delete(id,owner,false,7).get(); db.restore(id,owner,false,5,0).get();
            check(db.event(id)!=null&&db.comments(id).size()==1,"recycle retains event and comments");
            db.freeze(id,"admin",true,true,"test").get(); db.event(id,owner,false,now-1000,now+180000).get();
            check(db.status(id).frozen(),"changing event does not bypass freeze");
        }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path); Statement s=c.createStatement()) { s.execute("UPDATE warp_events SET start=1,end=2"); }
        try(WarpStore db=new WarpStore(path)) {
            check(!db.all().get(id).visible()&&!db.publicAvailable(db.all().get(id)),"offline expiry hidden on restart");
            check(db.comments(id).size()==1,"expired data retained");
            rejected(db.edit(id,owner,false,v -> v.edit(v.name(),v.description(),v.category(),v.icon(),true)));
            db.event(id,owner,false,0,0).get();
            check(!db.all().get(id).visible()&&db.event(id)==null,"cancel does not republish");
            db.freeze(id,"admin",true,false,"").get();
            db.edit(id,owner,false,v -> v.edit(v.name(),v.description(),v.category(),v.icon(),true)).get();
            check(db.publicAvailable(db.all().get(id)),"explicit republish");
            db.event(id,owner,true,1,System.currentTimeMillis()+60000).get();
            check(db.audits(true).get().stream().anyMatch(a -> a.action().equals("活动时间")),"admin event audited");
        }
        System.out.println("EVENT_SEARCH_CHECK_PASS: text and combined filters, validation, permissions, timing, persistence, recycle, visibility, frozen status, audit");
    }
}
