package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.List;

import us.tastybento.bskyblock.api.addons.BSBAddon;

public final class AddonsManager {

    private final List<BSBAddon> addons = new ArrayList<>();

    public List<BSBAddon> getAddons() {
        return addons;
    }
}
