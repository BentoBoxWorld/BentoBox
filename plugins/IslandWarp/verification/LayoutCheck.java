package com.islandwarp;

import java.util.HashSet;

public class LayoutCheck {
    static void check(int count,int firstRow,int lastRow) {
        var slots=new HashSet<Integer>();
        for(int i=0;i<count;i++) {
            int slot=MenuLayout.slot(i,count,firstRow);
            if(slot!=10+(firstRow-1+i/7)*9+i%7) throw new AssertionError("Not left-to-right");
            if(slot<10||slot>43||slot%9<1||slot%9>7||slot/9<firstRow||slot/9>lastRow||!slots.add(slot))
                throw new AssertionError("Bounds/overlap: count="+count+" firstRow="+firstRow+" slot="+slot);
        }
    }
    public static void main(String[] args) {
        for(int count=1;count<=28;count++) check(count,1,4);
        for(int count=1;count<=21;count++) check(count,2,4);
        var detail=new HashSet<Integer>(java.util.List.of(4,13,20,22,24,47,49,51));
        for(int stars=1;stars<=5;stars++) if(!detail.add(MenuLayout.ratingSlot(stars))) throw new AssertionError("Rating overlaps detail button");
        var management=java.util.List.of(4,13,20,21,22,23,24,29,30,31,32,33,46,47,48,49,50,51,52);
        if(new HashSet<>(management).size()!=management.size()) throw new AssertionError("Management overlap");
        var listing=java.util.List.of(1,2,3,4,6,7,45,46,47,48,49,51,52,53);
        if(new HashSet<>(listing).size()!=listing.size()) throw new AssertionError("List tools overlap");
        var filter=new HashSet<Integer>();
        int[] expected={20,22,24,29,31,33};
        for(int i=0;i<6;i++) if(MenuLayout.filterSlot(i)!=expected[i]||!filter.add(MenuLayout.filterSlot(i))) throw new AssertionError("Filter layout");
        var category=new HashSet<Integer>(java.util.List.of(4,13,49,45,53));
        for(int i=0;i<5;i++) if(!category.add(29+i)) throw new AssertionError("Category overlap");
        System.out.println("LAYOUT_CHECK_PASS: counts 1-28 full grid and 1-21 lower grid, left-to-right and unique");
    }
}
