package us.tastybento.bskyblock.api.addons;

import java.util.ArrayList;
import java.util.List;

public final class AddonManager {

    private final List<BSAddon> addons = new ArrayList<>();

    public List<BSAddon> getAddons() {
        return addons;
    }
}
