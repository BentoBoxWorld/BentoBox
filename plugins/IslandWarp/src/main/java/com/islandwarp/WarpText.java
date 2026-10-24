package com.islandwarp;

public final class WarpText {
    private WarpText() {}
    public static long eventTime(String text) {
        try {
            var time=java.time.LocalDateTime.parse(text);
            var zone=java.time.ZoneId.systemDefault();
            var offsets=zone.getRules().getValidOffsets(time);
            if(offsets.size()!=1) throw new IllegalArgumentException("该时间处于夏令时跳时或重复区间，请换一个时间");
            return time.toInstant(offsets.get(0)).toEpochMilli();
        } catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("时间格式应为 yyyy-MM-ddTHH:mm，例如 2026-10-15T18:00"); }
    }
    public static String remaining(long expiry,long now) {
        if(expiry<=now) return "已到期";
        long minutes=(expiry-now+59999)/60000;
        return minutes>=1440?minutes/1440+" 天 "+minutes%1440/60+" 小时":minutes>=60?minutes/60+" 小时 "+minutes%60+" 分钟":minutes+" 分钟";
    }
    public static String plain(String text) { return text.replaceAll("(?i)[&§][0-9a-fk-or]", ""); }
    public static String normalize(String text) { return text.replace('§','&'); }
    public static String name(String text,int limit) {
        String value=normalize(text);
        if(!plain(value).matches("[\\p{L}\\p{N}_\\-]{1,"+Math.max(1,limit)+"}")||value.length()>200)
            throw new IllegalArgumentException("名称只能使用文字、数字、下划线、短横线和颜色代码");
        return value;
    }
}
