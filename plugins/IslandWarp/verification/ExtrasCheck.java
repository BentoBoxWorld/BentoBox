import com.islandwarp.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class ExtrasCheck {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future) throws Exception {
        try { future.get(); throw new AssertionError("Unauthorized or invalid operation succeeded"); } catch(ExecutionException expected) {}
    }
    static Warp warp(UUID owner,String name,String island) {
        return new Warp(0,owner,"owner",name,"","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,island,1,0);
    }
    static Warp move(Warp w,String island) {
        return new Warp(w.id(),w.owner(),w.ownerName(),w.name(),w.description(),w.category(),w.world(),10,65,10,0,0,w.icon(),true,island,w.created(),w.visits());
    }
    public static void main(String[] args) throws Exception {
        Path path=Path.of(args[0]); UUID owner=UUID.randomUUID(),guest=UUID.randomUUID(),other=UUID.randomUUID(); String island="island-extras";
        long first,second,comment;
        try(WarpStore db=new WarpStore(path)) {
            first=db.create(warp(owner,"First",null),10,0).get();
            second=db.create(warp(owner,"Second",island),10,1).get();
            rejected(db.edit(first,owner,false,w -> move(w,island),1));
            rejected(db.edit(first,owner,true,w -> move(w,island),1));
            check(db.all().get(first).island()==null,"failed move preserves original");
            db.edit(second,owner,false,w -> move(w,island),1).get();
            db.block(first,owner,guest,"guest",true).get();
            check(db.blocked(first,guest)&&!db.blocked(second,guest),"per warp blacklist");
            rejected(db.block(first,other,guest,"guest",false));
            rejected(db.block(first,owner,owner,"owner",true));
            rejected(db.comment(guest,"guest",first,"blocked",100,50));
            db.block(first,owner,guest,"guest",false).get();
            db.comment(guest,"guest",first,"=HYPERLINK(\"example\")",100,50).get();
            comment=db.comments(first).get(0).id();
            check(db.notices(owner).get().get(0).comment()==comment,"comment notice targets exact comment");
            db.reply(owner,"owner",first,comment,"reply",100,true).get();
            check(db.notices(guest).get().get(0).comment()==comment,"reply notice targets exact comment");
            rejected(db.export(path.getParent().resolve("exports"),false));
            Path export=db.export(path.getParent().resolve("exports"),true).get();
            try(ZipFile zip=new ZipFile(export.toFile(),java.nio.charset.StandardCharsets.UTF_8)) {
                check(zip.size()==8&&zip.getEntry("warp_status.csv")!=null&&zip.getEntry("warp_events.csv")!=null&&zip.getEntry("payments.csv")!=null&&zip.getEntry("income.csv")!=null,"all export datasets including payments and income");
                String csv=new String(zip.getInputStream(zip.getEntry("comments.csv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                check(csv.contains("'=HYPERLINK("),"spreadsheet formula protected");
                check(csv.contains("\"\"example\"\""),"CSV quotes escaped");
            }
            db.block(first,owner,guest,"guest",true).get();
        }
        try(WarpStore db=new WarpStore(path)) {
            check(db.blocked(first,guest),"blacklist survives restart");
            check(db.notices(guest).get().get(0).comment()==comment,"focused notice survives restart");
            db.delete(second,owner,false).get();
            db.edit(first,owner,false,w -> move(w,island),1).get();
            check(island.equals(db.all().get(first).island()),"move after capacity freed");
            db.delete(first,owner,false).get();
            check(db.blocks(first).isEmpty(),"blacklist cascade delete");
        }
        check(WarpText.remaining(0,1).equals("已到期"),"expired countdown");
        check(WarpText.remaining(60000,0).equals("1 分钟"),"minute countdown");
        check(WarpText.remaining(90000000,0).equals("1 天 1 小时"),"day countdown");
        // Simulate an older database: new notification column must be added without dropping queued messages.
        try(var connection=java.sql.DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath()); var s=connection.createStatement()) {
            s.execute("ALTER TABLE notices DROP COLUMN comment");
        }
        try(WarpStore db=new WarpStore(path)) { check(db.all().isEmpty(),"legacy migration opens"); }
        System.out.println("EXTRAS_CHECK_PASS: blacklists, reply targeting, migration limits, safe export, countdown, persistence, schema migration");
    }
}
