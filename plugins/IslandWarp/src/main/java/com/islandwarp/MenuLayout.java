package com.islandwarp;

final class MenuLayout {
    private MenuLayout() {}
    static int filterSlot(int index) {
        if(index<0||index>=6) throw new IllegalArgumentException("Invalid filter position");
        return 20+(index/3)*9+(index%3)*2;
    }
    static int ratingSlot(int stars) {
        if(stars<1||stars>5) throw new IllegalArgumentException("Invalid rating");
        return 28+stars;
    }

    static int slot(int index,int count) { return slot(index,count,1); }

    /** Places items left-to-right in a 7-wide grid; firstRow is the 1-based content row (1-4). */
    static int slot(int index,int count,int firstRow) {
        if(count<1||count>28||index<0||index>=count||firstRow<1||firstRow>4||firstRow+(count-1)/7>4) throw new IllegalArgumentException("Invalid grid position");
        int row=index/7, column=index%7;
        return 10+(firstRow-1+row)*9+column;
    }
}
