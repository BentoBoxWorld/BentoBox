package com.islandwarp;

import java.util.UUID;
import java.util.Objects;

public record Warp(long id, UUID owner, String ownerName, String name, String description,
                   String category, UUID world, double x, double y, double z, float yaw, float pitch,
                   String icon, boolean visible, String island, long created, long visits) {
    public Warp withVisits(long value) { return new Warp(id,owner,ownerName,name,description,category,world,x,y,z,yaw,pitch,icon,visible,island,created,value); }
    public boolean sameSettings(Warp other) {
        return other!=null && id==other.id && Objects.equals(owner,other.owner)
                && Objects.equals(ownerName,other.ownerName) && Objects.equals(name,other.name)
                && Objects.equals(description,other.description) && Objects.equals(category,other.category)
                && Objects.equals(world,other.world) && Double.compare(x,other.x)==0 && Double.compare(y,other.y)==0
                && Double.compare(z,other.z)==0 && Float.compare(yaw,other.yaw)==0 && Float.compare(pitch,other.pitch)==0
                && Objects.equals(icon,other.icon) && visible==other.visible && Objects.equals(island,other.island)
                && created==other.created;
    }
    public Warp edit(String name, String description, String category, String icon, boolean visible) {
        return new Warp(id, owner, ownerName, name, description, category, world, x, y, z, yaw, pitch,
                icon, visible, island, created, visits);
    }
}
