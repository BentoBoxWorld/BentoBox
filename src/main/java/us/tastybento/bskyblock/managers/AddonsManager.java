package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.List;

import us.tastybento.bskyblock.api.addons.BSAddon;

public final class AddonsManager {

    private final List<BSAddon> addons = new ArrayList<>();

    public List<BSAddon> getAddons() {
        return addons;
    }
}
