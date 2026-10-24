package com.islandwarp;

import java.util.Locale;

public record SearchFilter(double maxPrice,double minRating) {
    public SearchFilter {
        if(!Double.isFinite(maxPrice)||maxPrice<0||!Double.isFinite(minRating)||minRating<0||minRating>5) throw new IllegalArgumentException("最高票价需为非负金额，最低评分需在 0–5 之间");
    }
    public boolean matches(Warp w,String keyword,double price,double rating) {
        return price<=maxPrice&&rating>=minRating&&WarpText.plain(w.name()+" "+w.ownerName()+" "+w.description()).toLowerCase(Locale.ROOT).contains(WarpText.plain(keyword).toLowerCase(Locale.ROOT));
    }
}
