package com.islandwarp;

import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

public class ReviewCheck {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static void rejected(CompletableFuture<?> future)throws Exception {try{future.get();throw new AssertionError("failure expected");}catch(ExecutionException expected){}}
    public static void main(String[] args)throws Exception {
        UUID owner=UUID.randomUUID(); Path path=Path.of(args[0]);
        try(WarpStore store=new WarpStore(path)) {
            long id=store.create(new Warp(0,owner,"owner","Review","","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0),5,0).get();
            Warp before=store.all().get(id);
            store.visit(id).get(); check(before.sameSettings(store.all().get(id)),"visits invalidate operation");
            store.checked(before,"",false).get();
            check(!before.sameSettings(before.edit("changed","","other","COMPASS",true)),"edit ignored");
            check(!before.sameSettings(null),"deletion ignored");
            var dbField=WarpStore.class.getDeclaredField("db");dbField.setAccessible(true);Connection original=(Connection)dbField.get(store);
            AtomicInteger refreshes=new AtomicInteger(),failures=new AtomicInteger();
            Connection proxy=(Connection)Proxy.newProxyInstance(ReviewCheck.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("prepareStatement")&&a[0].toString().startsWith("SELECT author,text,created,warp,id,player,edited FROM comments")) {
                    refreshes.incrementAndGet();if(failures.getAndUpdate(n->Math.max(0,n-1))>0)throw new SQLException("injected refresh failure");
                }
                try{return m.invoke(original,a);}catch(InvocationTargetException e){throw e.getCause();}
            });
            dbField.set(store,proxy);
            store.notices(owner).get();store.history(owner).get();store.income(owner).get();store.reports(true).get();store.audits(true).get();
            store.visits(id,owner,false).get();store.dailyVisits(id,owner,false).get();store.acknowledge(owner,List.of()).get();store.readHistory(owner,1).get();
            check(refreshes.get()==0,"query/history acknowledgements perform full refresh");
            failures.set(2);
            store.ticket(id,owner,false,true,25d).get(); // Commit succeeded: must not return the failure that triggers a Vault refund.
            check(!store.healthy(),"refresh failure hidden");
            try{store.requireUnfrozen(id);throw new AssertionError("stale permission cache usable");}catch(IllegalArgumentException expected){}
            try(var s=original.createStatement();var r=s.executeQuery("SELECT price FROM tickets WHERE warp="+id)){check(r.next()&&r.getDouble(1)==25,"committed purchase lost");}
            rejected(store.ticket(id,owner,false,true,50d)); // Recovery still fails: no further write.
            store.income(owner).get();check(store.healthy()&&store.ticket(id).price()==25,"next task did not recover cache");
            check(refreshes.get()==3&&failures.get()==0,"fault injection did not match expected refresh calls");
            rejected(store.create(before.edit("Review","","other","COMPASS",true),5,0));
            check(store.all().size()==1,"rollback regression");
        }
        WarpTeleport teleport=new WarpTeleport(null);
        var field=WarpTeleport.class.getDeclaredField("cooldown");field.setAccessible(true);
        @SuppressWarnings("unchecked") var cooldown=(Map<UUID,Long>)field.get(teleport);
        UUID playerId=UUID.randomUUID(),expired=UUID.randomUUID();cooldown.put(playerId,System.currentTimeMillis()+60000);cooldown.put(expired,0L);
        Player player=(Player)Proxy.newProxyInstance(ReviewCheck.class.getClassLoader(),new Class[]{Player.class},(p,m,a)->m.getName().equals("getUniqueId")?playerId:null);
        teleport.quit(new PlayerQuitEvent(player,net.kyori.adventure.text.Component.empty()));
        check(cooldown.containsKey(playerId)&&!cooldown.containsKey(expired),"quit cleared active cooldown or retained expired cooldown");
        System.out.println("REVIEW_CHECK_PASS: committed refresh failure is not refundable, stale cache blocked/recovered, reads skip refresh, visits ignored, edits rejected, quit retains cooldown");
    }
}
