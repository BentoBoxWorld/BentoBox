package com.islandwarp;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class FixCheck {
    static void check(boolean value,String reason) { if(!value) throw new AssertionError(reason); }
    static void rejected(CompletableFuture<?> future)throws Exception {
        try { future.get();throw new AssertionError("expected rejection"); } catch(ExecutionException expected) { check(expected.getCause() instanceof IllegalArgumentException,"unexpected failure: "+expected); }
    }
    static Warp warp(UUID owner,String name) { return new Warp(0,owner,"owner",name,"","other",UUID.randomUUID(),0,65,0,0,0,"COMPASS",true,null,1,0); }
    public static void main(String[] args)throws Exception {
        Path path=Path.of(args[0]);UUID owner=UUID.randomUUID(),guest=UUID.randomUUID();String outstanding;
        try(WarpStore db=new WarpStore(path)) {
            long a=db.create(warp(owner,"A"),5,0).get(),b=db.create(warp(owner,"B"),5,0).get();
            db.freeze(a,"admin",true,true,"frozen").get();
            rejected(db.buyPin(a,owner,1,7,0));
            db.freeze(a,"admin",true,false,"").get();
            long expiry=System.currentTimeMillis()+3600000;
            db.buyPin(a,owner,1,7,expiry).get();
            db.freeze(a,"admin",true,true,"frozen").get();rejected(db.renewPin(a,owner,expiry,1000));
            db.freeze(a,"admin",true,false,"").get();db.releasePin(a,owner).get();
            db.buyPin(a,owner,1,7,System.currentTimeMillis()-1).get();
            db.buyPin(b,owner,1,7,0).get();check(db.pins().get(1)==b,"expired pin not reusable");
            db.favorite(guest,a).get();db.edit(a,owner,false,w -> w.edit(w.name(),"","other","COMPASS",false)).get();
            check(!db.favorite(guest,a).get(),"hidden favorite not removable");rejected(db.favorite(guest,a));
            db.edit(a,owner,false,w -> w.edit(w.name(),"","other","COMPASS",true)).get();
            db.comment(guest,"guest",a,"old",100,50).get();long comment=db.comments(a).get(0).id();
            db.report(guest,a,comment,"old reason").get();rejected(db.report(guest,a,comment,"duplicate"));
            db.resolveReport(db.reports(true).get().get(0).id(),owner,true,false).get();
            db.editComment(guest,a,comment,"new",100,0).get();db.report(guest,a,comment,"new reason").get();
            var report=db.reports(true).get().get(0);check(report.text().equals("new"),"reopened report stale");
            db.reply(owner,"owner",a,comment,"reply",100,true,0).get();
            db.delete(a,owner,false,7).get();db.resolveReport(report.id(),owner,true,true).get();
            db.restore(a,owner,false,5,0).get();check(db.comments(a).isEmpty()&&db.replies(comment).isEmpty(),"moderated content restored");
            for(int i=0;i<31;i++) db.income(owner,a,"A",10,9,true).get();
            db.income(guest,a,"A",100,90,true).get();db.income(owner,a,"A",10,9,false).get();
            var first=db.incomePage(owner,0,0).get();var second=db.incomePage(owner,1,0).get();
            check(first.pages()==2&&first.rows().size()==28&&second.rows().size()==4,"income pagination");
            check(first.total()==279&&first.daily()==279&&db.incomePage(guest,0,0).get().total()==90,"income aggregation/privacy");
            check(db.incomePage(owner,Integer.MAX_VALUE,0).get().page()==1,"income page clamp");
            outstanding=db.payment(owner,100,"DEBIT","purchase test").get();db.paymentState(outstanding,"EXECUTING").get();
            String done=db.payment(owner,100,"CREDIT","refund test").get();db.paymentState(done,"DONE").get();
            String failed=db.payment(guest,100,"DEBIT","declined test").get();db.paymentState(failed,"FAILED").get();
            rejected(db.payment(owner,100,"DEBIT","duplicate debit"));
            check(db.unresolvedPayments().get().size()==1,"terminal payment filtering");
            rejected(db.payment(owner,Double.NaN,"DEBIT","invalid"));
        }
        try(WarpStore db=new WarpStore(path)) {
            var unresolved=db.unresolvedPayments().get();check(unresolved.size()==1&&unresolved.get(0).id().equals(outstanding)&&unresolved.get(0).state().equals("EXECUTING"),"unknown payment lost on restart");
            db.paymentState(outstanding,"PAID").get();db.reconcilePayment(outstanding,"DONE","admin").get();check(db.unresolvedPayments().get().isEmpty(),"reconciled payment remains open");
            rejected(db.reconcilePayment(outstanding,"DONE","admin"));
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath());Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT 1 FROM sqlite_master WHERE name='income_owner_id'")) { check(r.next(),"income index missing"); }
        }
        System.out.println("FIX_CHECK_PASS: frozen promotion, expired pin reuse, hidden favorite removal, report reopen, recycle moderation, paginated income, durable payment states");
    }
}
